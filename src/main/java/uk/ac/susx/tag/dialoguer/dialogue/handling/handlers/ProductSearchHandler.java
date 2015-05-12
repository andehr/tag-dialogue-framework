package uk.ac.susx.tag.dialoguer.dialogue.handling.handlers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import uk.ac.susx.tag.dialoguer.Dialoguer;
import uk.ac.susx.tag.dialoguer.dialogue.components.Dialogue;
import uk.ac.susx.tag.dialoguer.dialogue.components.Intent;
import uk.ac.susx.tag.dialoguer.dialogue.components.IntentMatch;
import uk.ac.susx.tag.dialoguer.dialogue.components.Response;
import uk.ac.susx.tag.dialoguer.dialogue.handling.IntentMerger;
import uk.ac.susx.tag.dialoguer.dialogue.handling.factories.HandlerFactory;
import uk.ac.susx.tag.dialoguer.dialogue.handling.factories.ProductSearchHandlerFactory;
import uk.ac.susx.tag.dialoguer.dialogue.handling.handlers.productSearchIntentHandlers.BuyMethod;
import uk.ac.susx.tag.dialoguer.dialogue.handling.handlers.productSearchIntentHandlers.ConfirmProblemHandler;
import uk.ac.susx.tag.dialoguer.knowledge.database.product.ProductMongoDB;
import uk.ac.susx.tag.dialoguer.utils.StringUtils;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by juliewe on 05/05/2015.
 */
public class ProductSearchHandler extends Handler {

    //db settings provided by the json file product_search_handler.json
    private String dbHost;
    private String dbPort;
    private String dbName;
    protected transient ProductMongoDB db;

    //analyser sourceIds
    public static final String mainAnalyser="wit.ai";
    public static final String yesNoAnalyser="simple_yes_no";
    public static final String giftAnalyser="gift";
    public static final String merged="merged";
    public static final List<String> analysers = Lists.newArrayList(mainAnalyser, yesNoAnalyser,giftAnalyser);


    //intent names - match wit.ai intents
    public static final String quit="cancel_query";
    public static final String buy="really_buy";
    public static final String giftIntent="gift";
    public static final String confirm="confirm";
   // public static final String yes="yes";
   // public static final String no="no";
    public static final List<String> confirmIntents=Lists.newArrayList(confirm, Intent.yes);

    //slot names
    public static final String productSlot="product_query";
    public static final String recipientSlot="recipient";
    public static final String messageSlot="message_body";
    public static final String yes_no_slot="yes_no";
    public static final String witTitle="title";
    public static final String witAuthor="author";

    //recipient names
    public static final List<String> recipients = Lists.newArrayList("julie","simon","andrew");

    public ProductSearchHandler(){
        //register problem and intent handlers here
        super.registerIntentHandler(quit, (i, d, r) -> Response.buildCancellationResponse());
        super.registerIntentHandler(Intent.cancel, (i,d,r)-> Response.buildCancellationResponse()); // shouldn't be needed since this intent and response should have been picked up by dialoguer
        super.registerIntentHandler(buy, new BuyMethod());
        super.registerProblemHandler(new ConfirmProblemHandler());
    }

    @Override
    public HandlerFactory getFactory() {
        return new ProductSearchHandlerFactory();
    }

    @Override
    public Dialogue getNewDialogue(String dialogueId){

        Dialogue d = new Dialogue(dialogueId);
        d.setState("initial_query");

        return d;
    }

    public void setupDatabase()throws Dialoguer.DialoguerException {
        try {
            if(!dbHost.equals("")) {
                db = new ProductMongoDB(dbHost, Integer.parseInt(dbPort), dbName);
            } else {
                db= new ProductMongoDB();

            }
        } catch (UnknownHostException e) {
            throw new Dialoguer.DialoguerException("Cannot connect to database host", e);
        }
    }

    @Override
    public void close() throws Exception {
        // Close any resources here (like database)
        db.close();
    }

    @Override
    public List<Intent> preProcessIntents(List<Intent> intents, List<IntentMatch> matches, Dialogue d){
        //do any preprocessing/filtering of intents here before the dialoguer gets to auto-query etc

        //useful debug - see what the intents actually are
        for(Intent i:intents){
            System.err.println(i.toString());
        }

       boolean isGift=(Intent.isPresent(giftIntent,intents)||d.isInWorkingMemory("gift","yes"));
       if(isGift){d.putToWorkingMemory("gift","yes");}

       intents = new IntentMerger(intents)
                        .merge(Sets.newHashSet("buy_media"), (intentsToBeMerged) -> {
                            Intent output = new Intent("really_buy");
                            output.copySlots(intentsToBeMerged);
                            if(!isGift&&!output.areSlotsFilled(Sets.newHashSet(recipientSlot))){//default value for recipient if not identified as gift
                                output.fillSlot(recipientSlot,d.getId());
                            }
                            if(!isGift&&!output.areSlotsFilled(Sets.newHashSet(messageSlot))){
                                output.fillSlot(messageSlot,"none");
                            }
                            output.setSource(merged);
                            return output;
                        })
                        .getIntents();


        intents=intents.stream().map(intent->BuyMethod.makeQueryMap(intent)).collect(Collectors.toList()); //turn any witTitle and witAuthor into a product_query
        //intents = IntentMerger.merge(intents, Sets.newHashSet("1", "2"), "12");
        for(Intent i:intents){
            System.err.println(i.toString());
        }

        return intents;
    }

    @Override
    public Response handle(List<Intent> intents, Dialogue dialogue){
        //how to handle a list of intents
        Response r=applyFirstProblemHandlerOrNull(intents, dialogue, this.db);//first check whether there is a specific problemHandler associated with these intents

        if(r==null){
            Intent i = Intent.getFirstIntentFromSource(merged,intents); //look for pre-processed/merged intents first
            if(i!=null){
                r=applyIntentHandler(i,dialogue,this.db);
            }
        }


        for(String analyser:analysers) { //try each analyser in order of priority for a non-null response
            if(r==null) {
                Intent i = Intent.getFirstIntentFromSource(analyser, intents);
                if (i != null) {
                    r = applyIntentHandler(i, dialogue, this.db);//
                }
            }
        }

        if(r==null){//no intent handler
            if(dialogue.getStates().contains("initial")) { //state specific unknown
                r = new Response("unknown");
            } else {
                r= new Response("unknown");
            }

        }
        //System.err.println("Handler generated response "+ r.toString());
        return r;
    }

    public static ProductMongoDB castDB(Object resource) {
        ProductMongoDB db;
        try {
            db = (ProductMongoDB) resource;
        } catch (ClassCastException e) {
            throw new Dialoguer.DialoguerException("Resource should be mongo db", e);
        }
        return db;
    }

    @Override
    public Set<String> getRequiredAnalyserSourceIds(){return Sets.newHashSet(analysers);}

    public static Response processStack(Dialogue d, ProductMongoDB db){
        String focus="unknown";
        if (!d.isEmptyFocusStack()) {
            focus = d.popTopFocus();
        }
        Map<String, String> responseVariables = new HashMap<>();
        try {
            switch (focus) {
                case "confirm_buy":
                    responseVariables.put(ProductSearchHandler.productSlot, StringUtils.detokenise(d.peekTopIntent().getSlotValuesByType(ProductSearchHandler.productSlot)));
                    responseVariables.put(ProductSearchHandler.recipientSlot, StringUtils.detokenise(d.peekTopIntent().getSlotValuesByType(ProductSearchHandler.recipientSlot)));
                    responseVariables.put(ProductSearchHandler.messageSlot, StringUtils.detokenise(d.peekTopIntent().getSlotValuesByType(ProductSearchHandler.messageSlot)));
                    break;
                case "confirm_buy_no_message":
                    responseVariables.put(ProductSearchHandler.productSlot, StringUtils.detokenise(d.peekTopIntent().getSlotValuesByType(ProductSearchHandler.productSlot)));
                    responseVariables.put(ProductSearchHandler.recipientSlot, StringUtils.detokenise(d.peekTopIntent().getSlotValuesByType(ProductSearchHandler.recipientSlot)));
                    break;
                case "unknown_recipient":
                    break;
                case "unknown_product":
                    break;
                case "confirm_completion":
                    break;
            }
        } catch(ArrayIndexOutOfBoundsException e){
            throw new Dialoguer.DialoguerException("Error with response variables: "+e.toString());
        }
        return new Response(focus,responseVariables);

    }

}

package uk.ac.susx.tag.dialoguer.dialogue.handling.handlers.productSearchIntentHandlers;

import uk.ac.susx.tag.dialoguer.dialogue.components.Dialogue;
import uk.ac.susx.tag.dialoguer.dialogue.components.Intent;
import uk.ac.susx.tag.dialoguer.dialogue.components.Response;
import uk.ac.susx.tag.dialoguer.dialogue.handling.handlers.Handler;
import uk.ac.susx.tag.dialoguer.dialogue.handling.handlers.ProductSearchHandler;
import uk.ac.susx.tag.dialoguer.knowledge.database.product.ProductMongoDB;

import java.util.List;

/**
 * Created by juliewe on 12/05/2015.
 */
public class RejectProblemHandler implements Handler.ProblemHandler{
    @Override
    public boolean isInHandleableState(List<Intent> intents, Dialogue dialogue) {
        //should handle no for Julie in a different problem handler
        return intents.stream().anyMatch(i->(i.getName().equals(ProductSearchHandler.confirm)&&i.getSlotValuesByType(ProductSearchHandler.yes_no_slot).contains("no"))||i.getName().equals(Intent.no));
    }

    @Override
    public void handle(List<Intent> intents, Dialogue d, Object resource) {
        //first of all need to find out which slot is being rejected
        //push need to confirm individual items of intent which have not yet been confirmed
        System.err.println("RejectProblemHandler has fired");
        d.pushFocus("confirm_buy"); //for final confirmation after individual confirmations
        if(!d.isInWorkingMemory(ProductSearchHandler.messageSlot,"confirmed")) {
            d.pushFocus("confirm_message");
        }
        if(!d.isInWorkingMemory(ProductSearchHandler.recipientSlot,"confirmed")) {
            d.pushFocus("confirm_recipient");
        }

        if(!d.isInWorkingMemory(ProductSearchHandler.productIdSlot,"confirmed")) {
            d.pushFocus("confirm_product");
        }

    }

    @Override
    public void registerStackKey(Handler.PHKey key) {

    }


}

package uk.ac.susx.tag.dialoguer.dialogue.handling.handlers.modular;

import uk.ac.susx.tag.dialoguer.dialogue.components.Dialogue;
import uk.ac.susx.tag.dialoguer.dialogue.components.Intent;
import uk.ac.susx.tag.dialoguer.dialogue.handling.handlers.Handler;

import java.util.List;
import java.util.Map;

/**
 * Created by Daniel Saska on 7/20/2015.
 */
public class HelpMeProblemHandler implements Handler.ProblemHandler {
    public Map<String, String> helpTable;

    //Intents
    private  final String intent_help = "get_help";

    //Focuses
    static public final String focus_help = "give_help";

    //Memory
    static public final String help_string = "help_string";


    /**
     * Determines whether the intent set is handlable by this problem handler and returns value accordingly.
     * @param intents Current set of intent
     * @param dialogue Dialugoue instance
     * @return True if the set is handlable, false otherwise
     */
    @Override
    public boolean isInHandleableState(List<Intent> intents, Dialogue dialogue) {
        boolean demandS = intents.stream().filter(i->i.getName().equals(intent_help)).count()>0;
        int histSize = dialogue.getMessageHistory().size();
        if (histSize <= 1) {
            return false;
        }
        return demandS && dialogue.getMessageHistory().get(histSize - 2).getResponseName() != null;
    }


    /**
     * Handle the current set of intents
     * @param intents Intents extracted from user input
     * @param dialogue Dialogue instance
     * @param resource Unused
     */
    @Override
    public void handle(List<Intent> intents, Dialogue dialogue, Object resource) {
        dialogue.pushFocus(focus_help);
        int histSize = dialogue.getMessageHistory().size();
        if (helpTable.containsKey(dialogue.getMessageHistory().get(histSize - 2).getResponseName())) {
            dialogue.putToWorkingMemory("help_string", helpTable.get(dialogue.getMessageHistory().get(histSize - 2).getResponseName()));
        } else {
            dialogue.putToWorkingMemory("help_string", "I do not know how to help you with my last request.");
        }
    }

    /**
     * Unused
     * @param key
     */
    @Override
    public void registerStackKey(Handler.PHKey key) {

    }
}


package tln;

import simplenlg.lexicon.italian.ITXMLLexicon;

public class NLG {

    public static void main(String[] args) {
        LTS lts = new LTS(new ITXMLLexicon());
        
        System.out.println(lts.makeSentence(args[0]));
    }

}

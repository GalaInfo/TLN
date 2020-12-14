//Logic to Sentence class
package tln;

import java.util.ArrayList;
import simplenlg.features.Feature;
import simplenlg.features.Tense;
import simplenlg.features.italian.ItalianLexicalFeature;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.NLGFactory;
import simplenlg.framework.WordElement;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.AdvPhraseSpec;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.PPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.phrasespec.VPPhraseSpec;
import simplenlg.realiser.Realiser;

public class LTS {

    private final Lexicon lex;
    private final NLGFactory factory;
    private final Realiser realiser;
    private final ArrayList<String[]> clauses;

    public LTS(Lexicon lex) {
        this.lex = lex;
        clauses = new ArrayList<>();
        factory = new NLGFactory(lex);
        realiser = new Realiser();
    }

    //translates logic as a String matrix
    private void translateLogic(String logic) {
        logic = logic.replaceAll("\\s+", "").replaceAll("[(, )]", ",");
        String[] temp = logic.split("&");
        for (String t : temp) {
            clauses.add(t.split(","));
        }
    }

    private ArrayList<String[]> createRefs(String lexeme) {
        ArrayList<String[]> refs = new ArrayList<>();

        for (String[] clause : clauses) {
            if (clause.length > 1 && lexeme.equals(clause[1])) {
                refs.add(clause);
            }
        }
        return refs;
    }

    //recursive functions to create a PhraseSpec
    private NPPhraseSpec expandNP(String lexeme) {
        ArrayList<String[]> refs = createRefs(lexeme);
        boolean plural = false;
        boolean pronoun = false;
        boolean adverb = false;
        String article = "il";

        NPPhraseSpec np = factory.createNounPhrase(lexeme);

        for (String[] ref : refs) {
            switch (ref[0]) {
                case "desc" -> {
                    WordElement we = lex.getWord(ref[2], LexicalCategory.ADJECTIVE);
                    we.setFeature(ItalianLexicalFeature.QUALITATIVE, true);
                    np.addModifier(we);
                }
                case "poss" -> {
                    WordElement we = lex.getWord(ref[2], LexicalCategory.ADJECTIVE);
                    we.setFeature(ItalianLexicalFeature.POSSESSIVE, true);
                    np.addModifier(we);
                }
                case "adv" ->
                    adverb = true;
                case "prep" -> {
                    PPPhraseSpec pp = factory.createPrepositionPhrase(ref[3], expandNP(ref[2]));
                    np.addComplement(pp);
                }
                case "udf" ->
                    article = "un";
                case "pl" ->
                    plural = true;
                case "pn" ->
                    pronoun = true;
            }
        }

        if (!(pronoun || adverb)) {
            np.setSpecifier(article);
            np.setPlural(plural);
        }
        return np;
    }

    private VPPhraseSpec expandVP(String lexeme) {
        ArrayList<String[]> refs = createRefs(lexeme);

        VPPhraseSpec vp = factory.createVerbPhrase(lexeme);

        for (String[] ref : refs) {
            switch (ref[0]) {
                case "tense" -> {
                    switch (ref[2]) {
                        case "pres" ->
                            vp.setFeature(Feature.TENSE, Tense.PRESENT);
                        case "past" ->
                            vp.setFeature(Feature.TENSE, Tense.PAST);
                        case "fut" ->
                            vp.setFeature(Feature.TENSE, Tense.FUTURE);
                    }
                    switch (ref[3]) {
                        case "progr" ->
                            vp.setFeature(Feature.PROGRESSIVE, true);
                        case "perf" ->
                            vp.setFeature(Feature.PERFECT, true);
                    }
                }

                case "adv" -> {
                    if ("ci".equals(ref[2])) {
                        vp.addComplement("ci");
                    } else {
                        AdvPhraseSpec ap = expandAP(ref[2]);
                        vp.addComplement(ap);
                    }
                }
                case "prep" -> {
                    PPPhraseSpec pp = factory.createPrepositionPhrase(ref[3], expandNP(ref[2]));
                    vp.addComplement(pp);
                }
            }
        }
        return vp;
    }

    private AdvPhraseSpec expandAP(String lexeme) {
        ArrayList<String[]> refs = createRefs(lexeme);

        AdvPhraseSpec ap = factory.createAdverbPhrase(lexeme);

        for (String[] ref : refs) {
            if ("prep".equals(ref[0])) {
                PPPhraseSpec pp = factory.createPrepositionPhrase(ref[3], expandNP(ref[2]));
                ap.addComplement(pp);
            }
        }
        return ap;
    }

    //makes a sentence plan
    public String makeSentence(String logic) {
        translateLogic(logic);
        String[] core = clauses.remove(0);

        NPPhraseSpec subject = "NONE".equals(core[1]) ? null : expandNP(core[1]);
        VPPhraseSpec action = expandVP(core[0]);
        NPPhraseSpec object = core.length > 2 ? expandNP(core[2]) : null;
        SPhraseSpec sentence;
        if (subject == null && object != null) {
            action.setPlural(object.isPlural());
            sentence = factory.createClause(action, object);
        } else {
            sentence = factory.createClause(subject, action, object);
        }
        return realiser.realiseSentence(sentence);
    }
}

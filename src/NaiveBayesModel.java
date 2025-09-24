import java.io.Serializable;
import java.util.*;

/**
 * Classe NaiveBayesModel
 * Représente le modèle entraîné et permet la prédiction.
 */
public class NaiveBayesModel implements Serializable {

    private static final long serialVersionUID = 1L; // nécessaire pour la sérialisation

    private Map<String, Integer> docCounts;                // nb documents par classe
    private Map<String, Map<String, Integer>> wordCounts;  // nb d’occurrences de chaque mot par classe
    private Map<String, Integer> totalWords;               // nb total de mots par classe
    private Set<String> vocabulary;                        // vocabulaire global
    private int totalDocs;                                 // nb total de documents

    public NaiveBayesModel(PartialResult pr) {
        this.docCounts = new HashMap<>(pr.docCounts);
        this.wordCounts = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : pr.wordCountsPerClass.entrySet()) {
            this.wordCounts.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        this.totalWords = new HashMap<>(pr.totalWordsPerClass);
        this.vocabulary = new HashSet<>(pr.vocabulary);

        this.totalDocs = 0;
        for (int c : docCounts.values()) {
            this.totalDocs += c;
        }
    }

    /**
     * Prédit la classe d’une liste de tokens
     */
    public String predict(List<String> tokens) {
        String bestClass = null;
        double bestLogProb = Double.NEGATIVE_INFINITY;

        for (String label : docCounts.keySet()) {
            // Prior P(c)
            double logProb = Math.log((double) docCounts.get(label) / totalDocs);

            // Likelihood P(w|c)
            for (String token : tokens) {
                int count = wordCounts.getOrDefault(label, Collections.emptyMap())
                                      .getOrDefault(token, 0);
                double prob = (count + 1.0) / (totalWords.get(label) + vocabulary.size()); // lissage Laplace
                logProb += Math.log(prob);
            }

            if (logProb > bestLogProb) {
                bestLogProb = logProb;
                bestClass = label;
            }
        }
        return bestClass;
    }
}

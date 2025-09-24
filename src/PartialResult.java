import java.util.*;

public class PartialResult {
    public Map<String, Integer> docCounts = new HashMap<>();
    public Map<String, Map<String, Integer>> wordCountsPerClass = new HashMap<>();
    public Map<String, Integer> totalWordsPerClass = new HashMap<>();
    public Set<String> vocabulary = new HashSet<>();

    // Fusionner un autre résultat partiel dans celui-ci
    public void mergeIn(PartialResult pr) {
        // 🔹 Fusion des documents par classe
        for (Map.Entry<String, Integer> e : pr.docCounts.entrySet()) {
            String label = e.getKey();
            int count = e.getValue();
            this.docCounts.put(label, this.docCounts.getOrDefault(label, 0) + count);
        }

        // 🔹 Fusion des mots par classe
        for (Map.Entry<String, Map<String, Integer>> e : pr.wordCountsPerClass.entrySet()) {
            String label = e.getKey();
            Map<String, Integer> wordMap = e.getValue();

            this.wordCountsPerClass.putIfAbsent(label, new HashMap<>());
            Map<String, Integer> targetMap = this.wordCountsPerClass.get(label);

            for (Map.Entry<String, Integer> we : wordMap.entrySet()) {
                String word = we.getKey();
                int count = we.getValue();
                targetMap.put(word, targetMap.getOrDefault(word, 0) + count);
            }
        }

        // 🔹 Fusion des totaux de mots
        for (Map.Entry<String, Integer> e : pr.totalWordsPerClass.entrySet()) {
            String label = e.getKey();
            int count = e.getValue();
            this.totalWordsPerClass.put(label, this.totalWordsPerClass.getOrDefault(label, 0) + count);
        }

        // 🔹 Fusion du vocabulaire
        this.vocabulary.addAll(pr.vocabulary);
    }
}

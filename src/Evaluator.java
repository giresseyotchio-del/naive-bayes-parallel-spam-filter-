import java.util.*;

public class Evaluator {

    public static void evaluate(NaiveBayesModel model, List<DataPoint> testData) {
        int correct = 0;
        int total = testData.size();

        // Confusion matrix
        Map<String, Map<String, Integer>> confusion = new HashMap<>();

        for (DataPoint dp : testData) {
            String predicted = model.predict(dp.tokens);
            if (predicted.equals(dp.label)) {
                correct++;
            }

            // Update confusion matrix
            confusion
                .computeIfAbsent(dp.label, k -> new HashMap<>())
                .put(predicted, confusion.getOrDefault(dp.label, new HashMap<>())
                    .getOrDefault(predicted, 0) + 1);
        }

        double accuracy = (double) correct / total;

        System.out.println("=== Résultats d'évaluation ===");
        System.out.println("Accuracy: " + accuracy);

        // Affiche la matrice de confusion
        System.out.println("\nMatrice de confusion:");
        for (String actual : confusion.keySet()) {
            System.out.print(actual + " -> ");
            for (Map.Entry<String, Integer> e : confusion.get(actual).entrySet()) {
                System.out.print(e.getKey() + ":" + e.getValue() + "  ");
            }
            System.out.println();
        }
    }
}

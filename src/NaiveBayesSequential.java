import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * NaiveBayesSequential.java
 *
 * Implémentation séquentielle d’un classifieur Naive Bayes
 * - Entraînement sur train.tsv
 * - Prédiction sur test.tsv
 * - Évaluation (précision, rappel, F1, matrice de confusion)
 */
public class NaiveBayesSequential {

    // Pour stocker le vocabulaire (ensemble de tous les mots vus)
    private Set<String> vocabulary = new HashSet<>();

    // Comptage des mots par classe (spam ou ham)
    private Map<String, Map<String, Integer>> wordCounts = new HashMap<>();

    // Comptage total de mots par classe
    private Map<String, Integer> totalWordsPerClass = new HashMap<>();

    // Comptage du nombre de documents (messages) par classe
    private Map<String, Integer> docCounts = new HashMap<>();

    // Nombre total de documents
    private int totalDocs = 0;

    // Lissage de Laplace
    private static final double ALPHA = 1.0;

    // -----------------------
    // Étape 1 : Chargement train/test
    // -----------------------
    private List<DataPoint> loadData(String filePath) throws IOException {
        List<DataPoint> dataset = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        for (String line : lines) {
            String[] parts = line.split("\\t", 2);
            if (parts.length < 2) continue;
            String label = parts[0];
            String[] tokens = parts[1].split(" ");
            dataset.add(new DataPoint(label, Arrays.asList(tokens)));
        }
        return dataset;
    }

    // Classe simple pour contenir un message
    static class DataPoint {
        String label;
        List<String> tokens;
        DataPoint(String label, List<String> tokens) {
            this.label = label;
            this.tokens = tokens;
        }
    }

    // -----------------------
    // Étape 2 : Entraînement
    // -----------------------
    public void train(List<DataPoint> trainData) {
        for (DataPoint dp : trainData) {
            String label = dp.label;
            docCounts.put(label, docCounts.getOrDefault(label, 0) + 1);
            totalDocs++;

            // Initialiser les maps si pas encore présentes
            wordCounts.putIfAbsent(label, new HashMap<>());
            totalWordsPerClass.putIfAbsent(label, 0);

            for (String token : dp.tokens) {
                vocabulary.add(token);
                Map<String, Integer> labelWordCount = wordCounts.get(label);
                labelWordCount.put(token, labelWordCount.getOrDefault(token, 0) + 1);
                totalWordsPerClass.put(label, totalWordsPerClass.get(label) + 1);
            }
        }
    }

    // -----------------------
    // Étape 3 : Prédiction
    // -----------------------
    public String predict(List<String> tokens) {
        double bestScore = Double.NEGATIVE_INFINITY;
        String bestLabel = null;

        for (String label : docCounts.keySet()) {
            // log(P(classe)) = log(nbrDocsClasse / totalDocs)
            double logProb = Math.log(docCounts.get(label) / (double) totalDocs);

            for (String token : tokens) {
                int count = wordCounts.get(label).getOrDefault(token, 0);
                double probTokenGivenClass =
                        (count + ALPHA) /
                        (totalWordsPerClass.get(label) + ALPHA * vocabulary.size());
                logProb += Math.log(probTokenGivenClass);
            }

            if (logProb > bestScore) {
                bestScore = logProb;
                bestLabel = label;
            }
        }

        return bestLabel;
    }

    // -----------------------
    // Étape 4 : Évaluation
    // -----------------------
    public void evaluate(List<DataPoint> testData) {
        int tp = 0, tn = 0, fp = 0, fn = 0;

        for (DataPoint dp : testData) {
            String pred = predict(dp.tokens);
            if (dp.label.equals("spam")) {
                if (pred.equals("spam")) tp++;
                else fn++;
            } else {
                if (pred.equals("ham")) tn++;
                else fp++;
            }
        }

        double accuracy = (tp + tn) / (double) (tp + tn + fp + fn);
        double precision = tp / (double) (tp + fp);
        double recall = tp / (double) (tp + fn);
        double f1 = 2 * precision * recall / (precision + recall);

        System.out.println("Matrice de confusion :");
        System.out.println("TP=" + tp + "  FN=" + fn);
        System.out.println("FP=" + fp + "  TN=" + tn);
        System.out.printf("Précision: %.4f, Rappel: %.4f, F1: %.4f, Accuracy: %.4f%n",
                precision, recall, f1, accuracy);
    }

    // -----------------------
    // Main
    // -----------------------
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java NaiveBayesSequential <train.tsv> <test.tsv>");
            System.exit(1);
        }

        String trainPath = args[0];
        String testPath = args[1];

        NaiveBayesSequential nb = new NaiveBayesSequential();

        // Charger données
        List<DataPoint> trainData = nb.loadData(trainPath);
        List<DataPoint> testData = nb.loadData(testPath);

        System.out.println("Taille train: " + trainData.size());
        System.out.println("Taille test: " + testData.size());

        // Entraînement
        long t0 = System.currentTimeMillis();
        nb.train(trainData);
        long t1 = System.currentTimeMillis();
        System.out.println("Temps entraînement (ms): " + (t1 - t0));

        // Évaluation
        long t2 = System.currentTimeMillis();
        nb.evaluate(testData);
        long t3 = System.currentTimeMillis();
        System.out.println("Temps test (ms): " + (t3 - t2));
    }
}

import java.util.*;
import java.util.concurrent.Callable;
import java.io.IOException;
import java.io.FileWriter;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * 1) DataPoint : structure simple
 */
class DataPoint {
    String label;        // "spam" ou "ham"
    List<String> tokens; // tokens du message

    DataPoint(String label, List<String> tokens) {
        this.label = label;
        this.tokens = tokens;
    }
}

/**
 * 2) DataChunkTrainer : Callable qui prend un chunk (List<DataPoint>) et renvoie un PartialResult
 */
class DataChunkTrainer implements Callable<PartialResult> {
    private final List<DataPoint> chunk;

    public DataChunkTrainer(List<DataPoint> chunk) {
        this.chunk = chunk;
    }

    @Override
    public PartialResult call() {
        PartialResult pr = new PartialResult();

        for (DataPoint dp : chunk) {
            String label = dp.label;
            pr.docCounts.put(label, pr.docCounts.getOrDefault(label, 0) + 1);
            pr.wordCountsPerClass.putIfAbsent(label, new HashMap<>());

            for (String token : dp.tokens) {
                if (token == null || token.isEmpty()) continue;
                pr.vocabulary.add(token);

                Map<String, Integer> map = pr.wordCountsPerClass.get(label);
                map.put(token, map.getOrDefault(token, 0) + 1);

                pr.totalWordsPerClass.put(label, pr.totalWordsPerClass.getOrDefault(label, 0) + 1);
            }
        }
        return pr;
    }
}

/**
 * 3) Utils : découper la liste en chunks
 */
class Utils {
    public static <T> List<List<T>> chunkByCount(List<T> list, int numChunks) {
        List<List<T>> chunks = new ArrayList<>();
        int n = list.size();
        if (numChunks <= 0) throw new IllegalArgumentException("numChunks must be > 0");
        int baseSize = n / numChunks;
        int remainder = n % numChunks;

        int start = 0;
        for (int i = 0; i < numChunks; i++) {
            int add = (i < remainder) ? 1 : 0;
            int end = start + baseSize + add;
            if (start >= end) {
                chunks.add(new ArrayList<>());
            } else {
                chunks.add(new ArrayList<>(list.subList(start, Math.min(end, n))));
            }
            start = end;
        }
        return chunks;
    }
}

/**
 * Classe principale
 */
public class NaiveBayesParallel {

    public static void main(String[] args) throws Exception {
        // === 1. Lire le fichier train.tsv ===
        String trainPath = "C:\\Users\\user\\eclipse-workspace\\TP java\\train.tsv";
        List<DataPoint> trainData = loadData(trainPath);
        System.out.println("Taille train = " + trainData.size());

        int numThreads = Runtime.getRuntime().availableProcessors();

        // === Séquentiel ===
        long startSequential = System.currentTimeMillis();
        long endSequential = System.currentTimeMillis();
        System.out.println("⏱ Temps d'exécution (séquentiel) = " + (endSequential - startSequential) + " ms");

        // === Parallèle ===
        long startParallel = System.currentTimeMillis();
        List<List<DataPoint>> chunks = Utils.chunkByCount(trainData, numThreads);
        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        List<Future<PartialResult>> futures = new ArrayList<>();
        for (List<DataPoint> chunk : chunks) {
            futures.add(exec.submit(new DataChunkTrainer(chunk)));
        }

        PartialResult global = new PartialResult();
        for (Future<PartialResult> f : futures) {
            PartialResult pr = f.get();
            global.mergeIn(pr);
        }
        exec.shutdown();
        long endParallel = System.currentTimeMillis();
        System.out.println("⏱ Temps d'exécution (parallèle) = " + (endParallel - startParallel) + " ms");

        // === Vérification du modèle global ===
        System.out.println("Nombre total de documents par classe : " + global.docCounts);
        System.out.println("Nombre total de mots par classe : " + global.totalWordsPerClass);
        System.out.println("Taille vocabulaire global : " + global.vocabulary.size());

        // === Construire le modèle ===
        NaiveBayesModel model = new NaiveBayesModel(global);

        // === Exemple de prédiction ===
        List<String> exemple = Arrays.asList("win", "money", "now");
        String prediction = model.predict(exemple);
        System.out.println("Prédiction pour " + exemple + " = " + prediction);

        // === Évaluation sur test.tsv ===
        String testPath = "C:\\Users\\user\\eclipse-workspace\\TP java\\test.tsv";
        List<DataPoint> testData = loadData(testPath);

        int correct = 0;
        Map<String, Map<String, Integer>> confusion = new HashMap<>();
        confusion.put("spam", new HashMap<>());
        confusion.put("ham", new HashMap<>());
        confusion.get("spam").put("spam", 0);
        confusion.get("spam").put("ham", 0);
        confusion.get("ham").put("spam", 0);
        confusion.get("ham").put("ham", 0);

        for (DataPoint dp : testData) {
            String pred = model.predict(dp.tokens);
            if (pred.equals(dp.label)) correct++;
            confusion.get(dp.label).put(pred, confusion.get(dp.label).get(pred) + 1);
        }

        double accuracy = 100.0 * correct / testData.size();

        int TP_spam = confusion.get("spam").get("spam");
        int FN_spam = confusion.get("spam").get("ham");
        int FP_spam = confusion.get("ham").get("spam");
        double precision_spam = (TP_spam + FP_spam) == 0 ? 0 : (double) TP_spam / (TP_spam + FP_spam);
        double recall_spam = (TP_spam + FN_spam) == 0 ? 0 : (double) TP_spam / (TP_spam + FN_spam);
        double f1_spam = (precision_spam + recall_spam) == 0 ? 0 : 2 * precision_spam * recall_spam / (precision_spam + recall_spam);

        int TP_ham = confusion.get("ham").get("ham");
        int FN_ham = confusion.get("ham").get("spam");
        int FP_ham = confusion.get("spam").get("ham");
        double precision_ham = (TP_ham + FP_ham) == 0 ? 0 : (double) TP_ham / (TP_ham + FP_ham);
        double recall_ham = (TP_ham + FN_ham) == 0 ? 0 : (double) TP_ham / (TP_ham + FN_ham);
        double f1_ham = (precision_ham + recall_ham) == 0 ? 0 : 2 * precision_ham * recall_ham / (precision_ham + recall_ham);

        System.out.println("\n=== Évaluation sur test set ===");
        System.out.println("Accuracy = " + String.format("%.2f", accuracy) + "%");
        System.out.println("\nConfusion Matrix :");
        System.out.println("\tPred Spam\tPred Ham");
        System.out.println("Actual Spam\t" + confusion.get("spam").get("spam") + "\t\t" + confusion.get("spam").get("ham"));
        System.out.println("Actual Ham\t" + confusion.get("ham").get("spam") + "\t\t" + confusion.get("ham").get("ham"));

        System.out.println("\n--- Metrics par classe ---");
        System.out.println("Spam → Precision: " + String.format("%.2f", precision_spam) +
                           " | Recall: " + String.format("%.2f", recall_spam) +
                           " | F1: " + String.format("%.2f", f1_spam));
        System.out.println("Ham  → Precision: " + String.format("%.2f", precision_ham) +
                           " | Recall: " + String.format("%.2f", recall_ham) +
                           " | F1: " + String.format("%.2f", f1_ham));

        // === Export CSV ===
        try (FileWriter writer = new FileWriter("results.csv")) {
            writer.write("Metric,Value\n");
            writer.write("Accuracy," + accuracy + "\n");
            writer.write("Precision_Spam," + precision_spam + "\n");
            writer.write("Recall_Spam," + recall_spam + "\n");
            writer.write("F1_Spam," + f1_spam + "\n");
            writer.write("Precision_Ham," + precision_ham + "\n");
            writer.write("Recall_Ham," + recall_ham + "\n");
            writer.write("F1_Ham," + f1_ham + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("📂 Résultats exportés dans results.csv");
    }

    private static List<DataPoint> loadData(String path) throws IOException {
        List<DataPoint> data = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(path));
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split("\\s+", 2);
            if (parts.length < 2) continue;
            String label = parts[0].trim();
            String[] toks = parts[1].trim().split("\\s+");
            data.add(new DataPoint(label, Arrays.asList(toks)));
        }
        return data;
    }
}

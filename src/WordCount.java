import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class WordCount {
    public static void main(String[] args) {
        String filePath = "fichier.txt";
        HashMap<String, Integer> wordCounts = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line, " ,.;:!?\"'()[]{}<>/\\|*-+=\n\t\r");
                while (tokenizer.hasMoreTokens()) {
                    String word = tokenizer.nextToken().toLowerCase();
                    wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                }
            }

            System.out.println("Liste des mots et occurrences :");
            for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
                System.out.println(entry.getKey() + " : " + entry.getValue());
            }

            // Tri par fréquence décroissante
            System.out.println("\nListe triée par fréquence :");
            List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(wordCounts.entrySet());
            sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            for (Map.Entry<String, Integer> entry : sortedList) {
                System.out.println(entry.getKey() + " : " + entry.getValue());
            }

        } catch (IOException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }
}

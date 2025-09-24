import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExtractHTMLTags {
    public static void main(String[] args) {
        String filePath = "https://glotelho.cm/";
        HashSet<String> tags = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line, "<>", true);
                boolean inTag = false;
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    if (token.equals("<")) {
                        inTag = true;
                    } else if (token.equals(">")) {
                        inTag = false;
                    } else if (inTag) {
                        String tag = token.split("\\s+")[0];
                        tags.add("<" + tag + ">");
                    }
                }
            }

            System.out.println("Balises HTML trouvées :");
            for (String tag : tags) {
                System.out.println(tag);
            }

            // Tri alphabétique
            System.out.println("\nBalises HTML triées :");
            List<String> sortedTags = new ArrayList<>(tags);
            Collections.sort(sortedTags);
            for (String tag : sortedTags) {
                System.out.println(tag);
            }

        } catch (IOException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }
}

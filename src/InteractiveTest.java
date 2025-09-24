import java.io.*;
import java.util.*;

public class InteractiveTest {
    public static void main(String[] args) throws Exception {
        // Charger le modèle sauvegardé
        NaiveBayesModel model;
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("naivebayes_model.ser"))) {
            model = (NaiveBayesModel) ois.readObject();
            System.out.println("✅ Modèle chargé avec succès !");
        } catch (FileNotFoundException e) {
            System.out.println("❌ Erreur : le fichier naivebayes_model.ser est introuvable.");
            System.out.println("👉 Lance d’abord NaiveBayesParallel pour entraîner et sauvegarder le modèle.");
            return;
        }

        // Scanner pour lire les entrées utilisateur
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=== Test interactif du classificateur Naïve Bayes ===");
        System.out.println("Tape un message à classifier (ou 'exit' pour quitter)");

        while (true) {
            System.out.print("\n> ");
            String line = scanner.nextLine().trim();

            if (line.equalsIgnoreCase("exit")) {
                System.out.println("👋 Fin du test interactif. Merci !");
                break;
            }

            if (line.isEmpty()) {
                System.out.println("⚠️ Message vide, réessaye !");
                continue;
            }

            // Découper le message en tokens (par mots séparés)
            List<String> tokens = Arrays.asList(line.toLowerCase().split("\\s+"));

            // Prédire
            String prediction = model.predict(tokens);

            System.out.println("🔮 Prédiction : " + prediction.toUpperCase());
        }

        scanner.close();
    }
}

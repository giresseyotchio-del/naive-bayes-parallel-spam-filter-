import java.io.BufferedReader;            // pour lire le fichier ligne par ligne
import java.io.BufferedWriter;            // pour écrire les fichiers train/test
import java.io.IOException;               // pour gérer les exceptions IO
import java.nio.file.Files;               // utilitaire pour travailler avec fichiers/paths
import java.nio.file.Path;                // représentation d'un chemin
import java.nio.file.Paths;               // construire des Path à partir de String
import java.util.ArrayList;               // liste dynamique
import java.util.Arrays;                  // utilitaire pour convertir tableau -> liste
import java.util.Collections;             // utilitaire pour shuffle
import java.util.HashSet;                 // implémentation Set pour stopwords
import java.util.List;                    // interface List
import java.util.Random;                  // générateur aléatoire (répétabilité)
import java.util.Set;                     // interface Set
import java.util.stream.Collectors;       // utilitaires pour stream -> liste

/**
 * DataPreprocessor.java
 *
 * Charge et prétraite le dataset SMSSpamCollection (format: label \t message).
 * - Nettoyage minimal (minuscules, suppression des caractères non-alphanumériques)
 * - Tokenisation (split sur espaces)
 * - Option de suppression des stopwords
 * - Séparation train/test (shuffle reproducible)
 * - Sauvegarde train.tsv et test.tsv
 */
public class DataPreprocessor {

    // Classe interne simple pour représenter un exemple (label + tokens)
    static class DataPoint {
        public final String label;           // "spam" ou "ham"
        public final List<String> tokens;    // liste des tokens (mots) du message

        // Constructeur qui initialise le label et la liste de tokens
        public DataPoint(String label, List<String> tokens) {
            this.label = label;
            this.tokens = tokens;
        }

        // Représentation textuelle utile pour debug / affichage d'exemples
        @Override
        public String toString() {
            return label + "\t" + String.join(" ", tokens);
        }
    }

    // Petite classe pour retourner un split train/test
    static class Split {
        public final List<DataPoint> train;
        public final List<DataPoint> test;

        public Split(List<DataPoint> train, List<DataPoint> test) {
            this.train = train;
            this.test = test;
        }
    }

    // Ensemble de stopwords (exemple réduit). Tu peux l'enrichir si tu veux.
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        "a", "an", "the", "and", "or", "is", "are", "was", "were", "in", "on",
        "at", "to", "from", "for", "of", "with", "that", "this", "it", "i",
        "you", "he", "she", "we", "they", "me", "my", "your"
    ));

    /**
     * Charge et prétraite le fichier SMSSpamCollection.
     * @param pathStr chemin vers le fichier (ex: "SMSSpamCollection")
     * @param removeStopwords si true enlève les stopwords
     * @return liste de DataPoint prétraités
     * @throws IOException en cas d'erreur lecture fichier
     */
    public static List<DataPoint> loadAndPreprocess(String pathStr, boolean removeStopwords) throws IOException {
        Path path = Paths.get(pathStr);                    // construit un Path à partir du String
        List<DataPoint> dataset = new ArrayList<>();       // liste qui contiendra tous les DataPoint

        // Utilisation d'un BufferedReader (avec Files.newBufferedReader) pour lire ligne par ligne
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;                                   // variable temporaire pour stocker chaque ligne
            while ((line = br.readLine()) != null) {       // tant qu'il y a une ligne à lire
                if (line.trim().isEmpty()) continue;      // ignorer les lignes vides

                // Le dataset SMSSpamCollection sépare label et message par un tab => split avec limite 2
                String[] parts = line.split("\\t", 2);    // split sur tab, max 2 parties
                if (parts.length < 2) continue;          // si format inattendu, ignorer la ligne

                String label = parts[0].trim().toLowerCase();    // label -> "spam" ou "ham", en minuscules
                String message = parts[1].trim().toLowerCase(); // message en minuscules

                // Nettoyage basique : remplacer tout ce qui n'est pas lettre/chiffre/espace par un espace
                message = message.replaceAll("[^a-z0-9\\s]", " ");

                // Normaliser les espaces multiples en un seul et trim final
                message = message.replaceAll("\\s+", " ").trim();

                if (message.isEmpty()) continue;        // si message vide après nettoyage, ignorer

                // Tokenisation : split sur espace
                String[] rawTokens = message.split(" ");

                // Filtrer tokens vides (au cas où) et éventuellement retirer stopwords
                List<String> tokenList = Arrays.stream(rawTokens)
                        .filter(t -> !t.isEmpty())          // enlever tokens vides
                        .filter(t -> !(removeStopwords && STOPWORDS.contains(t))) // enlever stopwords si demandé
                        .collect(Collectors.toList());      // collecter en liste

                if (tokenList.isEmpty()) continue;      // si plus aucun token utile, ignorer l'exemple

                // Ajouter le datapoint à la dataset
                dataset.add(new DataPoint(label, tokenList));
            }
        }

        return dataset;                                   // retourner la liste complète
    }

    /**
     * Sépare la liste en train/test après shuffle reproductible.
     * @param data liste de DataPoint
     * @param trainRatio proportion pour le train (ex: 0.8)
     * @param seed graine pour Random (assure reproductibilité)
     * @return Split contenant train et test
     */
    public static Split trainTestSplit(List<DataPoint> data, double trainRatio, long seed) {
        // Créer une copie pour ne pas modifier la liste d'origine
        List<DataPoint> copy = new ArrayList<>(data);

        // Mélanger la copie de manière reproductible
        Collections.shuffle(copy, new Random(seed));

        // Calculer la taille du jeu d'entraînement (arrondi)
        int trainSize = (int) Math.round(copy.size() * trainRatio);

        // Découper en deux sous-listes
        List<DataPoint> train = new ArrayList<>(copy.subList(0, trainSize));
        List<DataPoint> test = new ArrayList<>(copy.subList(trainSize, copy.size()));

        return new Split(train, test);                     // retourner l'objet Split
    }

    /**
     * Sauvegarde une liste de DataPoint dans un fichier TSV (label \t message_tokens_joined)
     * @param data liste à sauvegarder
     * @param outPath chemin de sortie
     * @throws IOException en cas d'erreur écriture
     */
    public static void saveToTsv(List<DataPoint> data, Path outPath) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(outPath)) {   // writer auto-close grâce à try-with-resources
            for (DataPoint dp : data) {
                // Écrire label \t texte (tokens joints par espace)
                bw.write(dp.label + "\t" + String.join(" ", dp.tokens));
                bw.newLine();                                      // saut de ligne
            }
        }
    }

    /**
     * point d'entrée : 
     * args[0] : chemin vers SMSSpamCollection
     * args[1] : trainRatio (optionnel, default 0.8)
     * args[2] : removeStopwords (optionnel, default true)
     */
    public static void main(String[] args) throws Exception {
        // Vérification des arguments
        if (args.length < 1) {
            System.err.println("Usage: java DataPreprocessor <path-to-SMSSpamCollection> [trainRatio] [removeStopwords]");
            System.err.println("Exemple: java DataPreprocessor SMSSpamCollection 0.8 true");
            System.exit(1);
        }

        String path = args[0];                         // chemin du dataset fourni en argument
        double trainRatio = args.length >= 2 ? Double.parseDouble(args[1]) : 0.8; // ratio train/test
        boolean removeStopwords = args.length >= 3 ? Boolean.parseBoolean(args[2]) : true; // stopwords ?
        long seed = 42L;                               // graine fixe pour reproductibilité

        // Mesurer le temps de chargement / prétraitement
        long t0 = System.currentTimeMillis();
        List<DataPoint> dataset = loadAndPreprocess(path, removeStopwords);
        long t1 = System.currentTimeMillis();

        // Afficher résumé
        System.out.println("Nombre d'exemples chargés après prétraitement: " + dataset.size());
        System.out.println("Temps de prétraitement (ms): " + (t1 - t0));

        // Split train/test
        Split split = trainTestSplit(dataset, trainRatio, seed);
        System.out.println("Taille train: " + split.train.size() + " | Taille test: " + split.test.size());

        // Sauvegarder les fichiers train.tsv et test.tsv
        Path trainOut = Paths.get("train.tsv");
        Path testOut = Paths.get("test.tsv");
        saveToTsv(split.train, trainOut);
        saveToTsv(split.test, testOut);

        System.out.println("Fichiers écrits: " + trainOut.toAbsolutePath() + ", " + testOut.toAbsolutePath());

        // Afficher quelques exemples pour vérification
        System.out.println("\n--- 5 exemples d'entraînement (label \\t tokens) ---");
        for (int i = 0; i < Math.min(5, split.train.size()); i++) {
            System.out.println(split.train.get(i));
        }
    }
}

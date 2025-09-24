# naive-bayes-parallel-spam-filter-
Filtre Anti-Spam avec Naïve Bayes en Java
# 📧 Parallel Naive Bayes Spam Filter (Java)

Ce projet implémente un **classifieur Bayesien naïf parallèle** pour la détection de spam dans des SMS.  
L’objectif est de **réduire le temps d’exécution** par rapport à une version séquentielle tout en maintenant une **bonne précision**.

---

## 🚀 Fonctionnalités
- Lecture et prétraitement d’un dataset SMS (ham/spam).
- Implémentation du **Naive Bayes séquentiel** et **parallèle**.
- Découpage des données en *chunks* pour traitement parallèle.
- Évaluation sur un jeu de test (accuracy, précision, rappel, F1-score).
- Génération d’une **matrice de confusion**.
- Export des résultats vers un fichier `results.csv`.

---

## 📂 Structure du projet

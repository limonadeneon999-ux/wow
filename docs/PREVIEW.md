# Aperçu du design « premium » (CarStudio_Premium_Design.zip)

## 1. Le fichier envoyé est incomplet

Le zip pèse **4 089 octets**. Un dossier avec 6 fichiers Kotlin + README en ferait 50 à 80 Ko.
Le fichier contient bien la signature `PK`, mais **le répertoire central du zip est coupé** :
le transfert s'est arrêté en route.

Ce qui est récupérable :

| Élément | État |
| --- | --- |
| `carstudio_improved/README.md` (8,5 Ko) | lisible en entier (fin légèrement corrompue) |
| `carstudio_improved/ui/theme/CarTheme.kt` | **coupé**, données deflate invalides |
| `ThemeSpec.kt`, `Background.kt`, `Common.kt`, `HomeScreen.kt`, `SettingsScreen.kt` | **absents** du fichier |

Pour un aperçu **exact** du code de Mistral : renvoie le zip (ou colle les `.kt` dans le chat).
En attendant, voici une reconstruction à partir de son README.

## 2. Reconstruction réelle (code Compose, captures de l'app)

Tokens et effets décrits dans le README, implémentés dans l'app :

- `glassIntensity`, `grain`, `shadowDepth`, `metalSheen`, `ambientLight`, `borderGlow` ;
- 4 halos au lieu de 3, vignette, grain procédural, lumière ambiante ;
- verre teinté par l'accent, bordure lumineuse, reflet métallique sur cartes et icônes ;
- typographie retravaillée (poids Light sur l'horloge, letter-spacing) ;
- preset **Néon** (cyan/magenta) et section de réglages **Design premium** (6 curseurs expliqués).

| Écran | Capture |
| --- | --- |
| Accueil (effets poussés) | `docs/preview/home_premium.png` |
| Accueil Néon | `docs/preview/home_neon.png` |
| Focus Néon | `docs/preview/home_neon_focus.png` |
| Split | `docs/preview/home_split.png` |
| Mode jour | `docs/preview/home_day.png` |
| Réglages | `docs/preview/settings.png` |

Avant / après réel : `docs/preview/comparaison_effets.png`

## 3. Maquette concept (cible visuelle)

`docs/preview/concept_premium.png` — maquette HTML/CSS, **pas encore dans l'app**.

Ce qu'elle ajoute par rapport à l'existant :

- carte média avec pochette, titre, artiste, barre de progression et gros bouton play ;
- météo avec prévisions horaires sur 4 créneaux ;
- panneau d'applications unique (12 apps, 4 × 3) au lieu d'une grille qui flotte ;
- dock flottant en bas, un seul bloc de verre ;
- palette bleu/violet, halos larges, grain, bordures claires.

Comparaison : `docs/preview/comparaison.png` (gauche = v0.2, droite = maquette).

Source de la maquette : `docs/preview/concept/index.html` (police Sora incluse).

## 4. Tester

- APK : `dist/car-studio-0.3.0-preview.apk`
- Branche : `design-preview`
- Réglages → Apparence → **Design premium** pour pousser les effets.

## 5. Décision

Deux directions possibles :

1. **Rester sur le verre sombre** et affiner les effets reconstruits (rapide).
2. **Coder la maquette** dans Compose : pochette média réelle, prévisions,
   panneau d'apps, dock flottant (plus de travail, rendu plus spectaculaire).

Dis-moi laquelle tu veux, et si tu peux renvoyer le zip complet je compare avec la version de Mistral.

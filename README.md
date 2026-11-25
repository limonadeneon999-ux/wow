# Car Studio

**Une interface de voiture que tu dessines toi-même.**

Car Studio est une app Android (Kotlin + Jetpack Compose) pensée pour l'écran de la voiture :
un accueil par widgets, un moteur de thème très poussé, et des réglages expliqués un par un.

> **Ce que ce n'est pas.** Ce n'est pas une copie modifiée de l'app Android Auto de Google.
> Android Auto, côté voiture, n'accepte que des composants signés par Google : on ne peut pas
> la remplacer sur un autoradio d'origine. Voir [`docs/ANALYSE.md`](docs/ANALYSE.md) pour le
> détail honnête de ce qui est possible et de ce qui ne l'est pas.

## Où l'app tourne

| Contexte | Ça marche ? | Comment |
|---|---|---|
| Autoradio Android (aftermarket) ou Android Automotive (AAOS) | Oui, nativement | Installer l'APK, la définir comme accueil |
| Téléphone rooté + autoradio d'origine | Oui, via miroir d'écran | Une app de miroir (type Screen2Auto / ScreenOnAuto / KM Auto) affiche Car Studio en plein écran sur l'écran AA |
| Autoradio d'origine sans root | Non | Android Auto n'affichera que les apps qui implémentent ses modèles (media, messagerie, navigation) |
| Raspberry Pi + Crankshaft / OpenAuto | Oui (côté récepteur ouvert) | Ces récepteurs acceptent un certificat non-Google |

## Ce qu'il y a dedans (v0.1)

- **Moteur de thème** : accent, secondaire, fond, surface, rayon des angles, intensité du halo,
  échelle de l'interface, police, forme des icônes, dégradé on/off, animations on/off, fond d'écran.
- **3 accueils** : `Dock` (média + grille), `Split` (colonne info + grille), `Focus` (une grande carte).
- **Widgets** : horloge (digitale / minimale / analogique), média (placeholder), météo (placeholder),
  données véhicule (placeholder OBD), actions rapides.
- **Toutes les apps installées** : chargées via PackageManager, couleur dominante extraite de l'icône.
- **Réglages** : chaque option a un titre, une phrase d'explication, et parfois un détail replié.
  Recherche, aperçu en direct, export / import du thème en JSON, réinitialisation.
- **Mode conduite** : verrouillage des interactions en roulant, grosses cibles tactiles, réduction
  des animations, gaucher / droitier, jour / nuit / auto.

## Compiler

```bash
# JDK 17 + Android SDK (compileSdk 35)
gradle assembleDebug
# APK : app/build/outputs/apk/debug/app-debug.apk
```

Un workflow GitHub Actions (`.github/workflows/build.yml`) compile l'APK à chaque push et le
publie en artefact.

## Structure

```
app/src/main/java/com/carstudio/app/
├── MainActivity.kt
├── CarState.kt              # état + persistance
├── data/                    # modèles, presets, chargement des apps
└── ui/
    ├── CarStudioApp.kt      # racine, navigation accueil <-> réglages
    ├── HomeScreen.kt        # les 3 accueils
    ├── SettingsScreen.kt    # réglages + aperçu + import/export
    ├── theme/CarTheme.kt    # thème dynamique
    └── components/          # cartes verre, halos, tuiles d'app, réglages
```

## Feuille de route

Voir [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Licence

MIT (à ajouter). Projet personnel, non affilié à Google. « Android Auto » est une marque de Google LLC.

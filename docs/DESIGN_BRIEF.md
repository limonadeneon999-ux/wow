# Car Studio — brief design (à donner à ChatGPT, Claude ou un designer)

Ce document décrit l'app, son système de design actuel, ses contraintes et ce qu'il faut améliorer.
Il est fait pour être copié-collé tel quel dans un assistant, avec les captures du dossier
`docs/screenshots/`.

---

## 1. Le produit en une phrase

Car Studio est une app Android qui remplace l'écran d'accueil d'une voiture : horloge, météo,
média, grille de toutes les applications installées, et un moteur de thème très poussé.
Elle est affichée en plein écran sur l'écran de la voiture via une app de miroir (Android Auto
ne peut pas être remplacé : sa poignée de main exige un certificat Google).

## 2. Contraintes non négociables

- **Techno** : Kotlin + Jetpack Compose (Material 3), Kotlin 2.0.21, Compose BOM 2024.10.01,
  minSdk 26, targetSdk 35, AGP 8.7.3, Gradle 8.9.
- **Cible** : écran de voiture paysage, typiquement 1280 × 720 dp, parfois portrait ou ultra-large.
  L'interface doit rester lisible et utilisable à 60 cm des yeux, avec des cibles tactiles
  d'au moins 56 dp.
- **Sécurité** : pas de manipulation complexe en roulant. Grosses cibles, peu de texte, contraste élevé.
- **Aucune dépendance lourde** : pas de librairie d'animation exotique, pas de moteur de rendu externe.
- **Tout doit rester en français**, avec des libellés courts et des réglages expliqués.
- **Le thème est déjà ultra configurable** : accent, secondaire, fond, angles, halos, échelle UI,
  police, forme des icônes, fond d'écran, dégradé, style d'horloge. Le design doit fonctionner
  avec n'importe quelle couleur d'accent, pas seulement le bleu.

## 3. Où est le code

```
app/src/main/java/com/carstudio/app/
├── MainActivity.kt
├── CarState.kt                     état global (réglages, apps, lancement)
├── data/
│   ├── CarSettings.kt              modèle sérialisé de tous les réglages
│   ├── ThemeSpec.kt                couleurs, angles, halos, échelle, police
│   ├── Presets.kt                  7 thèmes prêts à l'emploi
│   ├── AppLoader.kt                charge les apps installées + couleur dominante
│   ├── Weather.kt                  météo Open-Meteo, cache, Wi-Fi uniquement
│   ├── Media.kt                    média via MediaSession
│   └── DemoApps.kt                 apps factices pour le mode démo
├── ui/
│   ├── CarStudioApp.kt             navigation home / réglages / apps
│   ├── HomeScreen.kt               accueil (3 dispositions) + cartes + grille
│   ├── SettingsScreen.kt           réglages + aperçu en direct
│   ├── AppPickerScreen.kt          liste des apps (favori / masquer)
│   ├── components/
│   │   ├── Background.kt           fond dégradé + halos flous
│   │   └── Common.kt               GlassCard, AppTile, AppIcon, SwitchRow, SliderRow…
│   └── theme/CarTheme.kt           mapping ThemeSpec → MaterialTheme
└── media/MediaListenerService.kt
```

## 4. Captures actuelles

| Écran | Fichier |
| --- | --- |
| Accueil Dock | `docs/screenshots/home_dock.png` |
| Accueil Split | `docs/screenshots/home_split.png` |
| Accueil Focus | `docs/screenshots/home_focus.png` |
| Accueil jour | `docs/screenshots/home_day.png` |
| Réglages | `docs/screenshots/settings.png` |
| Applications | `docs/screenshots/app_picker.png` |

Ces captures sont générées par les tests (`app/src/test/java/com/carstudio/app/ScreenshotTest.kt`,
Roborazzi). Le rendu est réel, pas une maquette.

## 5. Design tokens actuels

### Couleurs

| Rôle | Valeur par défaut |
| --- | --- |
| Fond | `#0B1020` (thème « Minuit ») |
| Surface des cartes | `#141B2E` |
| Texte principal | `#E8ECF8` |
| Accent | `#5B8CFF` |
| Secondaire | `#B06BFF` |

Le fond est un dégradé vertical + 2 à 3 halos radiaux flous (accent et secondaire).
Les cartes sont du verre : fond `surface` à 96 % → 60 %, bordure dégradée
`onSurface` 18 % → 2 %, ombre de 22 dp, reflet blanc 4,5 % en haut.

### Formes

- Cartes : `MaterialTheme.shapes.large` (28 dp).
- Tuiles d'apps : `squircle` / `rounded` / `circle` / `square`, rayon réglable 0 → 48 dp.
- Pilules d'action : cercle complet.
- Rayon de base des tuiles : 24 dp.

### Typographie

Material 3 par défaut (Roboto), échelle pilotée par `uiScale` (80 % → 135 %) :

- Horloge principale : `displayLarge` (57 sp).
- Température : `displaySmall` / `displayLarge`.
- Titres de cartes : `titleMedium` / `titleLarge`.
- Libellés : `labelLarge` (14 sp) / `labelSmall` (11 sp).
- Corps : `bodySmall` (12 sp) / `bodyMedium` (14 sp).

### Espacements

- Marge écran : 26 dp horizontal, 18 dp vertical (16 dp si largeur < 720 dp).
- Gouttière entre cartes : 14 dp.
- Gouttière grille : 12 dp horizontal, 14 dp vertical.
- Padding interne des cartes : 18 dp (14 dp pour les lignes compactes).

### Iconographie

Material Icons Rounded, 18 → 22 dp dans les cartes, 62 % de la tuile pour les icônes d'app.

## 6. Inventaire des composants

| Composant | Fichier | Rôle |
| --- | --- | --- |
| `GlassCard` | `components/Common.kt` | conteneur verre, ombre, bordure dégradée |
| `AppIcon` | `components/Common.kt` | tuile d'app (icône réelle ou initiale) |
| `AppTile` | `components/Common.kt` | icône + libellé + appui long (favori) |
| `StatusPill` | `components/Common.kt` | pilule d'état (AA, batterie) |
| `ActionPill` | `HomeScreen.kt` | raccourci bas d'écran |
| `MediaCard` | `HomeScreen.kt` | lecteur : pochette, titre, artiste, contrôles |
| `WeatherCard` / `WeatherHero` | `HomeScreen.kt` | météo compacte / grande |
| `ClockHero` | `HomeScreen.kt` | grande horloge + salutation + état |
| `VehicleCard` | `HomeScreen.kt` | OBD-II (placeholder) |
| `TopBar` | `HomeScreen.kt` | horloge, date, AA, batterie, réglages |
| `SettingCard` | `components/Common.kt` | carte de réglage avec titre + explication |
| `PreviewCard` | `SettingsScreen.kt` | aperçu miniature du thème |

## 7. Ce qui doit être amélioré (priorités)

1. **Hiérarchie visuelle** : l'écran Dock a encore une zone vide entre la carte média et la
   grille. Trouver une composition qui respire sans paraître vide (ex. carte média plus
   basse, grille plus dense, widget « prochaine action »).
2. **Personnalité** : l'ensemble est propre mais générique. Proposer une signature visuelle
   (traitement des halos, grain, verre plus marqué, accent en dégradé, coins, ombres colorées).
3. **Typographie** : proposer une échelle plus affirmée (titres plus grands, libellés plus
   espacés, chiffres tabulaires pour l'horloge et la température).
4. **Tuiles d'apps** : rendre les icônes plus premium (reflet, profondeur, état favori,
   animation d'appui) sans dénaturer les icônes réelles.
5. **Écran Focus** : la grande carte doit raconter quelque chose (heure, météo, média,
   prochaine action) au lieu d'être un grand vide.
6. **Réglages** : garder l'explication systématique, mais rendre la page plus scannable
   (regroupements, ancres, aperçu collant, recherche).
7. **Mode jour** : vérifier le contraste et les bordures (le verre sombre ne marche pas tel quel).
8. **Accessibilité** : contraste AA, cibles 56 dp, tailles de texte, mode « animations réduites ».

## 8. Ce qu'il ne faut pas casser

- Le moteur de thème : toute couleur d'accent doit fonctionner.
- Les 3 dispositions (Dock, Split, Focus) et leurs réglages.
- Le mode démo (apps factices) et le sélecteur d'apps (favori / masquer).
- La météo légère (cache, Wi-Fi uniquement) et le média via MediaSession.
- La lisibilité en voiture : pas de texte fin, pas de contraste faible, pas de micro-boutons.
- Le français, les libellés courts, les explications dans les réglages.

## 9. Prompt prêt à copier-coller

> Tu es designer d'interface et développeur Android senior. Voici le code d'une app Compose
> appelée Car Studio, qui s'affiche sur un écran de voiture 1280 × 720. Je veux que tu
> améliores son design sans changer ses fonctionnalités ni ses contraintes.
>
> Lis d'abord `docs/DESIGN_BRIEF.md`, puis les fichiers `ui/HomeScreen.kt`,
> `ui/components/Common.kt`, `ui/components/Background.kt`, `ui/SettingsScreen.kt`,
> `ui/theme/CarTheme.kt` et `data/ThemeSpec.kt`. Regarde les captures dans
> `docs/screenshots/`.
>
> Objectif : un rendu premium, cohérent, lisible à 60 cm, avec une vraie signature visuelle,
> qui fonctionne avec n'importe quelle couleur d'accent et en mode jour comme en mode nuit.
>
> Livre :
> 1. une critique courte (5 points max) de ce qui ne va pas ;
> 2. le code Kotlin/Compose complet des fichiers modifiés, prêt à remplacer l'existant ;
> 3. les nouveaux tokens (couleurs, rayons, espacements, typographie) ;
> 4. les risques de régression et comment les vérifier.
>
> Contraintes : Compose Material 3, Kotlin 2.0.21, minSdk 26, targetSdk 35, aucune nouvelle
> dépendance lourde, tout en français, cibles tactiles ≥ 56 dp, contraste AA.

## 10. Critères d'acceptation

- L'app compile (`./gradlew assembleDebug`).
- Les 6 captures se régénèrent sans erreur (`./gradlew recordRoborazziDebug`).
- Aucune régression fonctionnelle : dispositions, réglages, apps, météo, média, mode démo.
- Le rendu est meilleur sur les 4 critères : hiérarchie, personnalité, lisibilité, cohérence.

# AGENTS.md — mémoire du projet

## Identité

- **Projet** : Car Studio, une interface de voiture Android (Kotlin + Jetpack Compose).
- **Dépôt** : `limonadeneon999-ux/wow`, **branche dédiée `car-studio`**.
  Le dépôt n'a pas pu être créé par l'agent (token sans droit `createRepository`),
  donc la branche sert de dépôt autonome (arbre orphelin, sans les fichiers de `main`).
- **Utilisateur** : francophone. L'interface et la documentation sont en français.
- **But** : interface d'accueil pour l'écran de voiture, très personnalisable, qui remplace
  *l'expérience* Android Auto (et non l'app Google). Voir `docs/ANALYSE.md`.

## Contrainte structurante (ne pas oublier)

Android Auto côté autoradio n'accepte que des composants signés par Google. On ne peut donc
pas remplacer l'app Google sur un autoradio d'origine. Les voies réelles sont :
1. miroir d'écran sur téléphone rooté (Screen2Auto, ScreenOnAuto, KM Auto…) ;
2. autoradio Android aftermarket / Android Automotive (AAOS) : installation native ;
3. récepteur ouvert type Crankshaft / OpenAuto (Raspberry Pi).

Toute proposition doit rester honnête sur cette limite. Ne jamais promettre un remplacement
de l'app Google sur un autoradio de série.

## Compiler

Environnement de développement de la session (Debian 13, x86_64) :

```bash
export ANDROID_HOME=/workspace/android-sdk
export ANDROID_SDK_ROOT=/workspace/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
/workspace/tools/gradle-8.9/bin/gradle assembleDebug --no-daemon --console=plain
# APK : app/build/outputs/apk/debug/app-debug.apk
```

Le dépôt ne contient pas de wrapper Gradle : la CI utilise
`gradle/actions/setup-gradle@v4` avec `gradle-version: '8.9'`.
Versions : AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.10.01, compileSdk 35, minSdk 26.

## Architecture

- `data/ThemeSpec.kt` : modèle de thème + presets + fonds d'écran.
- `data/CarSettings.kt` : réglages complets (sérialisés en JSON).
- `data/SettingsRepository.kt` : persistance SharedPreferences + kotlinx.serialization.
- `data/AppLoader.kt` : apps installées via PackageManager, couleur dominante de l'icône.
- `CarState.kt` : état observable + actions (lancer une app, favoris, import/export).
- `ui/theme/CarTheme.kt` : construction du ColorScheme, de la Typography et des Shapes.
- `ui/components/` : GlassCard, halos, AppTile, contrôles de réglages, fond d'écran.
- `ui/HomeScreen.kt` : trois dispositions (dock, split, focus).
- `ui/SettingsScreen.kt` : réglages groupés, recherche, aperçu, import/export.

## Conventions

- Textes visibles en français, ton direct, chaque réglage = titre + explication.
- Pas de dépendance hors androidx + kotlinx.serialization.
- Les couleurs sont stockées en `Long` ARGB (0xAARRGGBB) et converties via `Color(long)`.
- Éviter les API Material3 expérimentales : boutons et chips sont dessinés à la main.
- Vérifier la compilation avant tout commit.

## État

- v0.1 compile et produit un APK (`dist/car-studio-0.1.0-debug.apk`).
- Prochaines étapes : météo réelle, média via MediaSession, OBD-II, mode nuit auto.

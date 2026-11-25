package com.carstudio.app.ui

import androidx.compose.ui.platform.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carstudio.app.CarState
import com.carstudio.app.data.CarSettings
import com.carstudio.app.data.MediaRepository
import com.carstudio.app.data.Presets
import com.carstudio.app.data.ThemePreset
import com.carstudio.app.data.Wallpapers
import com.carstudio.app.ui.components.*
import com.carstudio.app.ui.theme.iconShapeFor

private data class SettingItem(
    val title: String,
    val description: String,
    val keywords: String = "",
    val body: @Composable () -> Unit
)

private data class SettingSection(
    val title: String,
    val hint: String?,
    val items: List<SettingItem>
)

@Composable
fun SettingsScreen(
    state: CarState,
    onClose: () -> Unit,
    onOpenAppPicker: () -> Unit = {}
) {
    val settings = state.settings
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val sections = settingsSections(state, settings, clipboard, context, onOpenAppPicker)
    val visible = sections.mapNotNull { section ->
        val matches = section.items.filter { item ->
            query.isBlank() || (item.title + " " + item.description + " " + item.keywords)
                .contains(query, ignoreCase = true)
        }
        if (matches.isEmpty()) null else section.copy(items = matches)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Réglages", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Chaque option est expliquée. Touche un titre pour la relire.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Fermer")
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CircleShape,
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            placeholder = { Text("Rechercher un réglage…") }
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            if (query.isBlank()) {
                item { PreviewCard(settings) }
            }
            visible.forEach { section ->
                item { SectionTitle(section.title, section.hint) }
                items(section.items) { item -> item.body() }
            }
            if (visible.isEmpty()) {
                item {
                    Text(
                        "Aucun réglage ne correspond à « $query ».",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun settingsSections(
    state: CarState,
    s: CarSettings,
    clipboard: ClipboardManager,
    context: Context,
    onOpenAppPicker: () -> Unit
): List<SettingSection> {
    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    val appearance = SettingSection(
        "Apparence",
        "Les couleurs, les formes et la typographie de toute l'interface.",
        listOf(
            SettingItem(
                "Style de départ",
                "Un thème complet en un geste. Tu peux ensuite modifier chaque couleur.",
                "preset thème palette"
            ) {
                SettingCard("Style de départ", "Choisis une base, puis affine plus bas.") {
                    PresetChips(s.theme.preset) { preset ->
                        state.update { it.copy(theme = preset.spec) }
                    }
                }
            },
            SettingItem(
                "Couleur d'accent",
                "La teinte des boutons, des halos et des éléments actifs. C'est la couleur qui donne le caractère de l'écran.",
                "couleur accent primaire"
            ) {
                SettingCard("Couleur d'accent", "La teinte principale de l'interface.") {
                    ColorSwatches(ACCENTS, s.theme.accent) { color ->
                        state.updateTheme { it.copy(accent = color) }
                    }
                }
            },
            SettingItem(
                "Couleur secondaire",
                "Utilisée dans les dégradés et les reflets, en complément de l'accent.",
                "couleur secondaire dégradé"
            ) {
                SettingCard("Couleur secondaire", "Pour les dégradés et les reflets.") {
                    ColorSwatches(ACCENTS, s.theme.secondary) { color ->
                        state.updateTheme { it.copy(secondary = color) }
                    }
                }
            },
            SettingItem(
                "Fond",
                "La couleur de base derrière les cartes. Un fond très sombre fait ressortir les halos.",
                "fond background noir"
            ) {
                SettingCard("Fond", "La base derrière les cartes.") {
                    ColorSwatches(BACKGROUNDS, s.theme.background) { color ->
                        state.updateTheme { it.copy(background = color) }
                    }
                }
            },
            SettingItem(
                "Angles",
                "Arrondi des cartes et des icônes. 0 = angles droits, 48 = très arrondi.",
                "angle arrondi rayon corner"
            ) {
                SliderRow(
                    "Angles",
                    "Arrondi des cartes et des icônes.",
                    s.theme.corner,
                    0f..48f,
                    "${s.theme.corner.toInt()} dp"
                ) { value -> state.updateTheme { it.copy(corner = value) } }
            },
            SettingItem(
                "Intensité des halos",
                "Ces lueurs colorées dans les coins. Plus c'est haut, plus l'écran paraît vivant ; 0 les éteint.",
                "halo lueur glow lumière"
            ) {
                SliderRow(
                    "Intensité des halos",
                    "Les lueurs colorées dans les coins.",
                    s.theme.glow,
                    0f..1f,
                    "${(s.theme.glow * 100).toInt()} %"
                ) { value -> state.updateTheme { it.copy(glow = value) } }
            },
            SettingItem(
                "Taille de l'interface",
                "Agrandit ou réduit tout le texte et les espacements. Utile si l'écran de la voiture est loin.",
                "taille échelle zoom police texte accessibilité"
            ) {
                SliderRow(
                    "Taille de l'interface",
                    "Agrandit tout le texte et les espacements.",
                    s.theme.uiScale,
                    0.8f..1.35f,
                    "${(s.theme.uiScale * 100).toInt()} %"
                ) { value -> state.updateTheme { it.copy(uiScale = value) } }
            },
            SettingItem(
                "Police",
                "Change la famille de caractères. La police système reste la plus lisible en voiture.",
                "police typographie font"
            ) {
                SettingCard("Police", "La famille de caractères de l'interface.") {
                    ChoiceRow(
                        listOf(
                            "sans" to "Sans",
                            "mono" to "Mono",
                            "serif" to "Serif",
                            "cursive" to "Manuscrite"
                        ),
                        s.theme.font
                    ) { value -> state.updateTheme { it.copy(font = value) } }
                }
            },
            SettingItem(
                "Forme des icônes",
                "Le contour des tuiles d'applications.",
                "icône forme carré cercle arrondi"
            ) {
                SettingCard("Forme des icônes", "Le contour des tuiles d'applications.") {
                    ChoiceRow(
                        listOf(
                            "squircle" to "Squircle",
                            "rounded" to "Arrondi",
                            "circle" to "Cercle",
                            "square" to "Carré"
                        ),
                        s.theme.iconShape
                    ) { value -> state.updateTheme { it.copy(iconShape = value) } }
                }
            },
            SettingItem(
                "Fond d'écran",
                "L'ambiance derrière les cartes. « Couleurs du thème » suit automatiquement ton accent.",
                "fond wallpaper décor"
            ) {
                SettingCard("Fond d'écran", "L'ambiance derrière les cartes.") {
                    Chips(Wallpapers.all, s.theme.wallpaper) { value ->
                        state.updateTheme { it.copy(wallpaper = value) }
                    }
                }
            },
            SettingItem(
                "Dégradé",
                "Fond dégradé (activé) ou uni (désactivé).",
                "dégradé gradient fond"
            ) {
                SwitchRow(
                    "Dégradé",
                    "Fond dégradé plutôt qu'uni.",
                    s.theme.gradient
                ) { value -> state.updateTheme { it.copy(gradient = value) } }
            },
            SettingItem(
                "Horloge",
                "Digitale (00:00), minimale (0:00) ou analogique (aiguilles).",
                "horloge montre clock heure"
            ) {
                SettingCard("Horloge", "Le style d'affichage de l'heure.") {
                    ChoiceRow(
                        listOf(
                            "digital" to "Digitale",
                            "minimal" to "Minimale",
                            "analog" to "Analogique"
                        ),
                        s.theme.clockStyle
                    ) { value -> state.updateTheme { it.copy(clockStyle = value) } }
                }
            }
        )
    )

    val home = SettingSection(
        "Accueil",
        "La disposition de l'écran et les widgets visibles.",
        listOf(
            SettingItem(
                "Disposition",
                "Dock : une grande carte média puis la grille. Split : colonne d'infos à gauche, apps à droite. Focus : une seule grande carte.",
                "disposition layout accueil grille"
            ) {
                SettingCard("Disposition", "Comment l'écran est organisé.") {
                    ChoiceRow(
                        listOf("dock" to "Dock", "split" to "Split", "focus" to "Focus"),
                        s.layout
                    ) { value -> state.update { it.copy(layout = value) } }
                }
            },
            SettingItem(
                "Apps par ligne",
                "Nombre de colonnes dans la grille. Moins de colonnes = icônes plus grosses, plus faciles à viser.",
                "colonnes grille apps ligne"
            ) {
                SliderRow(
                    "Apps par ligne",
                    "Moins de colonnes = icônes plus grosses.",
                    s.appsPerRow.toFloat(),
                    3f..6f,
                    s.appsPerRow.toString()
                ) { value -> state.update { it.copy(appsPerRow = value.toInt()) } }
            },
            SettingItem(
                "Nombre d'apps affichées",
                "Combien de tuiles au maximum sur l'accueil. Les apps favorites passent en premier.",
                "nombre maximum apps tuiles"
            ) {
                SliderRow(
                    "Nombre d'apps affichées",
                    "Le maximum de tuiles sur l'accueil.",
                    s.maxApps.toFloat(),
                    4f..20f,
                    s.maxApps.toString()
                ) { value -> state.update { it.copy(maxApps = value.toInt()) } }
            },
            SettingItem(
                "Afficher l'horloge",
                "L'heure et la date en haut de l'écran.",
                "horloge date"
            ) {
                SwitchRow("Afficher l'horloge", "L'heure et la date en haut.", s.showClock) { value ->
                    state.update { it.copy(showClock = value) }
                }
            },
            SettingItem(
                "Afficher le média",
                "La carte du lecteur de musique. Elle est vide tant qu'aucun lecteur n'est connecté.",
                "média musique lecteur"
            ) {
                SwitchRow("Afficher le média", "La carte du lecteur de musique.", s.showMedia) { value ->
                    state.update { it.copy(showMedia = value) }
                }
            },
            SettingItem(
                "Afficher la météo",
                "La carte météo. Les données arrivent dans la prochaine version.",
                "météo temps"
            ) {
                SwitchRow("Afficher la météo", "La carte météo.", s.showWeather) { value ->
                    state.update { it.copy(showWeather = value) }
                }
            },
            SettingItem(
                "Afficher le véhicule",
                "Vitesse, régime et températures via un adaptateur OBD-II (à venir).",
                "véhicule obd vitesse"
            ) {
                SwitchRow("Afficher le véhicule", "Les données OBD-II (à venir).", s.showVehicle) { value ->
                    state.update { it.copy(showVehicle = value) }
                }
            },
            SettingItem(
                "Afficher les actions rapides",
                "La rangée de raccourcis en bas : navigation, musique, téléphone, apps.",
                "actions rapides raccourcis barre"
            ) {
                SwitchRow(
                    "Afficher les actions rapides",
                    "La rangée de raccourcis en bas.",
                    s.showActions
                ) { value -> state.update { it.copy(showActions = value) } }
            },
            SettingItem(
                "Mode démo",
                "Affiche des apps factices pour juger le rendu sans voiture. À désactiver pour retrouver tes vraies apps.",
                "démo test aperçu"
            ) {
                SwitchRow(
                    "Mode démo",
                    "Des apps factices pour tester le rendu.",
                    s.demoMode
                ) { value -> state.update { it.copy(demoMode = value) } }
            }
        )
    )

    val applications = SettingSection(
        "Applications",
        "Choisis ce qui apparaît sur l'accueil et ce qui reste caché.",
        listOf(
            SettingItem(
                "Choisir les apps",
                "Ouvre la liste complète : une étoile pour les favoris, un œil pour afficher ou masquer une app.",
                "apps applications favoris masquer liste"
            ) {
                SettingCard(
                    "Choisir les apps",
                    "${state.allApps.size} apps détectées · ${s.favorites.size} favoris · ${s.hiddenApps.size} masquées"
                ) {
                    Button(onClick = onOpenAppPicker) {
                        Text("Ouvrir la liste")
                    }
                }
            }
        )
    )

    val weather = SettingSection(
        "Météo",
        "Des données réelles, mais légères : quelques kilo-octets par mise à jour.",
        listOf(
            SettingItem(
                "Ville",
                "La ville utilisée pour la météo. La position est mémorisée après la première recherche, donc une seule requête de géocodage.",
                "ville météo localisation"
            ) {
                var city by remember(s.weatherCity) { mutableStateOf(s.weatherCity) }
                SettingCard("Ville", "La position est mémorisée après la première recherche.") {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        singleLine = true,
                        label = { Text("Ville") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {
                        state.update { it.copy(weatherCity = city.trim().ifBlank { "Paris" }) }
                        toast("Ville enregistrée")
                    }) {
                        Text("Enregistrer")
                    }
                }
            },
            SettingItem(
                "Fréquence de mise à jour",
                "Plus c'est espacé, moins ça consomme. 60 minutes suffisent largement pour la météo.",
                "fréquence rafraîchir intervalle données"
            ) {
                SliderRow(
                    "Fréquence de mise à jour",
                    "Un appel réseau toutes les X minutes.",
                    s.weatherRefreshMinutes.toFloat(),
                    15f..180f,
                    "${s.weatherRefreshMinutes} min"
                ) { value -> state.update { it.copy(weatherRefreshMinutes = value.toInt()) } }
            },
            SettingItem(
                "Wi-Fi uniquement",
                "Activé, la météo ne se télécharge jamais en données mobiles : elle reste sur le dernier relevé hors Wi-Fi.",
                "wifi données mobiles économie"
            ) {
                SwitchRow(
                    "Wi-Fi uniquement",
                    "Aucune mise à jour en données mobiles.",
                    s.weatherWifiOnly
                ) { value -> state.update { it.copy(weatherWifiOnly = value) } }
            }
        )
    )

    val media = SettingSection(
        "Média",
        "Le lecteur en cours : YouTube Music, Spotify, VLC…",
        listOf(
            SettingItem(
                "Accès aux médias",
                "Car Studio lit uniquement le titre, l'artiste et la pochette du lecteur actif. Aucune notification n'est lue, stockée ou envoyée.",
                "média notification autorisation lecteur"
            ) {
                val granted = MediaRepository.hasAccess(context)
                SettingCard(
                    "Accès aux médias",
                    if (granted) "Accès accordé : le lecteur peut s'afficher."
                    else "Accès non accordé : la carte média restera vide."
                ) {
                    Text(
                        if (granted) "Tout est prêt." else "Ouvre les réglages, puis autorise Car Studio dans « Accès aux notifications ».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) {
                        Text(if (granted) "Vérifier l'autorisation" else "Ouvrir les réglages")
                    }
                }
            }
        )
    )

    val driving = SettingSection(
        "Conduite",
        "Sécurité et confort pendant que tu roules.",
        listOf(
            SettingItem(
                "Mode conduite",
                "Verrouille les interactions quand la voiture roule. Recommandé : la loi interdit de manipuler l'écran en mouvement.",
                "conduite sécurité verrou roulage"
            ) {
                SwitchRow(
                    "Mode conduite",
                    "Verrouille les interactions en roulant.",
                    s.drivingLock
                ) { value -> state.update { it.copy(drivingLock = value) } }
            },
            SettingItem(
                "Grandes cibles tactiles",
                "Agrandit les zones cliquables pour viser sans regarder.",
                "tactile gros doigt cible accessibilité"
            ) {
                SwitchRow(
                    "Grandes cibles tactiles",
                    "Agrandit les zones cliquables.",
                    s.bigTouch
                ) { value -> state.update { it.copy(bigTouch = value) } }
            },
            SettingItem(
                "Réduire les animations",
                "Supprime les transitions. Utile si l'écran saccade ou si tu préfères un rendu net.",
                "animation mouvement transition"
            ) {
                SwitchRow(
                    "Réduire les animations",
                    "Supprime les transitions.",
                    s.reduceMotion
                ) { value -> state.update { it.copy(reduceMotion = value) } }
            },
            SettingItem(
                "Gaucher",
                "Place les commandes principales à gauche pour une voiture à conduite à droite.",
                "gaucher gauche droite volant"
            ) {
                SwitchRow("Gaucher", "Commandes principales à gauche.", s.leftHanded) { value ->
                    state.update { it.copy(leftHanded = value) }
                }
            },
            SettingItem(
                "Jour / nuit",
                "Auto suit l'heure et la luminosité. Jour force le thème clair, nuit force le sombre.",
                "nuit jour sombre clair thème"
            ) {
                SettingCard("Jour / nuit", "Auto suit l'heure et la luminosité.") {
                    ChoiceRow(
                        listOf("auto" to "Auto", "day" to "Jour", "night" to "Nuit"),
                        s.nightMode
                    ) { value -> state.update { it.copy(nightMode = value) } }
                }
            }
        )
    )

    val data = SettingSection(
        "Données",
        "Sauvegarde ton thème ou remets tout à zéro.",
        listOf(
            SettingItem(
                "Exporter le thème",
                "Copie toute la configuration (thème + disposition) dans le presse-papiers, en JSON. Colle-la où tu veux.",
                "export sauvegarde copie json"
            ) {
                SettingCard("Exporter le thème", "Copie la configuration en JSON.") {
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(state.exportJson()))
                        toast("Configuration copiée")
                    }) {
                        Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copier le JSON")
                    }
                }
            },
            SettingItem(
                "Importer le thème",
                "Colle un JSON exporté depuis Car Studio pour retrouver un thème exact.",
                "import restauration colle json"
            ) {
                SettingCard("Importer le thème", "Colle un JSON exporté depuis Car Studio.") {
                    Button(onClick = {
                        val text = clipboard.getText()?.text
                        if (text.isNullOrBlank()) {
                            toast("Le presse-papiers est vide")
                        } else if (state.importJson(text)) {
                            toast("Thème importé")
                        } else {
                            toast("JSON invalide")
                        }
                    }) {
                        Icon(Icons.Rounded.ContentPaste, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Coller et appliquer")
                    }
                }
            },
            SettingItem(
                "Réinitialiser",
                "Efface tous les réglages et revient au thème Minuit d'origine. Action immédiate, sans confirmation.",
                "réinitialiser reset défaut"
            ) {
                SettingCard("Réinitialiser", "Revient au thème d'origine.") {
                    OutlinedButton(onClick = {
                        state.reset()
                        toast("Réglages réinitialisés")
                    }) {
                        Icon(Icons.Rounded.RestartAlt, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tout réinitialiser")
                    }
                }
            }
        )
    )

    val setup = SettingSection(
        "Mise en route",
        "Comment voir Car Studio sur l'écran de la voiture, quel que soit le modèle.",
        listOf(
            SettingItem(
                "Le principe",
                "Car Studio est une app normale. Android Auto, lui, n'accepte que des composants signés par Google : impossible de le remplacer sur un autoradio d'origine.",
                "android auto signature miroir principe"
            ) {
                SettingCard("Le principe", "Deux minutes pour comprendre.") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Android Auto dessine son interface sur l'autoradio et vérifie un certificat Google. On ne peut donc pas installer une autre app « à la place » d'Android Auto sur un autoradio de série.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            "La solution qui marche partout : afficher Car Studio en plein écran par-dessus Android Auto, avec une app de miroir. Android Auto gère alors le câble ou le sans-fil, et l'écran affiche Car Studio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            SettingItem(
                "Afficher sur l'écran",
                "La marche à suivre pour un Pixel rooté sous Magisk, avec n'importe quel autoradio filaire ou sans fil.",
                "miroir screenonauto km auto magisk root installer"
            ) {
                SettingCard("Afficher sur l'écran", "La marche à suivre, étape par étape.") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Step(1, "Garde Android Auto installé. Ne le désinstalle pas : c'est lui qui parle à la voiture.")
                        Step(2, "Dans Android Auto : à propos, tape 10 fois sur « Version » pour activer le mode développeur, puis coche « Sources inconnues ».")
                        Step(3, "Installe une app de miroir compatible avec Android 17 : ScreenOnAuto (sideload) ou KM Auto (KoalaMirror, root).")
                        Step(4, "Branche en USB ou connecte en sans-fil, lance le miroir, puis choisis Car Studio.")
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:com.google.android.projection.gearhead")
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.onFailure { toast("Android Auto n'est pas installé") }
                        }) {
                            Text("Ouvrir la fiche Android Auto")
                        }
                    }
                }
            },
            SettingItem(
                "Compatibilité",
                "Filaire, sans fil, tactile, tous les formats d'écran : c'est Android Auto qui gère le transport, Car Studio s'adapte à la taille.",
                "filaire sans fil tactile format écran compatibilité"
            ) {
                SettingCard("Compatibilité", "Ce qui est déjà géré.") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("• Filaire et sans fil : gérés par Android Auto.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text("• Tactile et molette : la grille et les cartes restent utilisables.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text("• Tous formats : l'accueil s'adapte en largeur (paysage large, portrait, split).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        )
    )

    val about = SettingSection(
        "À propos",
        null,
        listOf(
            SettingItem(
                "Comment ça marche",
                "Car Studio affiche ton interface à la place de l'écran Android Auto, pas à la place de l'app Google.",
                "à propos android auto google signature"
            ) {
                SettingCard("Comment ça marche", "L'essentiel, sans jargon.") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Android Auto dessine lui-même son interface sur l'autoradio et n'accepte que des composants signés par Google. Impossible donc de la remplacer sur un autoradio d'origine.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            "Car Studio contourne le problème : c'est une app normale, affichée en plein écran sur l'écran de la voiture via une app de miroir (Screen2Auto, ScreenOnAuto, KM Auto…) sur téléphone rooté. Elle marche aussi nativement sur un autoradio Android ou Android Automotive.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Info, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Ne manipule pas l'écran en roulant.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        )
    )

    return listOf(appearance, home, applications, weather, media, driving, data, setup, about)
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PreviewCard(s: CarSettings) {
    val shape = MaterialTheme.shapes.large
    val colors = wallpaperColors(s.theme.wallpaper, s.theme)
    val ink = if (s.theme.wallpaper == "daylight") Color(0xFF10131A) else Color.White

    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.top, colors.bottom)))
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("21:42", style = MaterialTheme.typography.headlineMedium, color = ink)
                Text(
                    "Aperçu en direct",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.6f)
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(iconShapeFor(s.theme.iconShape, s.theme.corner))
                                .background(Color(s.theme.accent).copy(alpha = 0.35f))
                                .border(
                                    1.dp,
                                    Color(s.theme.accent).copy(alpha = 0.55f),
                                    iconShapeFor(s.theme.iconShape, s.theme.corner)
                                )
                        )
                    }
                }
            }
            Column(
                Modifier.width(150.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(ink.copy(alpha = 0.10f))
                        .border(1.dp, ink.copy(alpha = 0.08f), MaterialTheme.shapes.medium)
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(iconShapeFor(s.theme.iconShape, s.theme.corner))
                                .background(Color(s.theme.accent).copy(alpha = 0.55f))
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Box(
                                Modifier
                                    .height(5.dp)
                                    .fillMaxWidth(0.85f)
                                    .clip(CircleShape)
                                    .background(ink.copy(alpha = 0.55f))
                            )
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier
                                    .height(4.dp)
                                    .fillMaxWidth(0.55f)
                                    .clip(CircleShape)
                                    .background(ink.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(ink.copy(alpha = 0.06f))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(3) {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .clip(iconShapeFor(s.theme.iconShape, s.theme.corner))
                                    .background(Color(s.theme.accent).copy(alpha = 0.35f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChips(selected: String, onSelect: (ThemePreset) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Presets.all.forEach { preset ->
            val isSelected = preset.id == selected
            Column(
                Modifier
                    .width(132.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        MaterialTheme.shapes.medium
                    )
                    .clickable { onSelect(preset) }
                    .padding(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier.size(14.dp).clip(CircleShape)
                            .background(Color(preset.spec.accent))
                    )
                    Box(
                        Modifier.size(14.dp).clip(CircleShape)
                            .background(Color(preset.spec.secondary))
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(preset.name, style = MaterialTheme.typography.labelLarge)
                Text(
                    preset.tagline,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun Chips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (id, label) ->
            val isSelected = id == selected
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    )
                    .clickable { onSelect(id) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private val ACCENTS = listOf(
    0xFF5B8CFF, 0xFF38D6C4, 0xFFB06BFF, 0xFFFF6B9A,
    0xFFFF8A3D, 0xFF7CE38B, 0xFFFFD166, 0xFFE8ECF8
)

private val BACKGROUNDS = listOf(
    0xFF07080D, 0xFF0B0A12, 0xFF0D0F14, 0xFF120D0A,
    0xFF0A1210, 0xFF111318, 0xFF050607, 0xFFF2F4FA
)

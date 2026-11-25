package com.carstudio.app.data

/**
 * Applications factices pour le mode démo : il permet de juger l'interface
 * sans voiture, et sert aux captures d'écran des tests.
 */
object DemoApps {
    val list: List<AppEntry> = listOf(
        AppEntry("demo.maps", "Maps", null, 0xFF4C8DFF),
        AppEntry("demo.music", "Musique", null, 0xFFFF6B9A),
        AppEntry("demo.phone", "Téléphone", null, 0xFF7CE38B),
        AppEntry("demo.podcasts", "Podcasts", null, 0xFFB06BFF),
        AppEntry("demo.radio", "Radio", null, 0xFFFF8A3D),
        AppEntry("demo.messages", "Messages", null, 0xFF38D6C4),
        AppEntry("demo.video", "Vidéo", null, 0xFFFF4D4D),
        AppEntry("demo.files", "Fichiers", null, 0xFFFFD166)
    )
}

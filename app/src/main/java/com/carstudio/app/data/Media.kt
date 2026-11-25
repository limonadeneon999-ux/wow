package com.carstudio.app.data

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.carstudio.app.media.MediaListenerService

data class MediaSnapshot(
    val title: String,
    val artist: String,
    val playing: Boolean,
    val art: ImageBitmap?
)

object MediaRepository {

    private fun component(context: Context) =
        ComponentName(context, MediaListenerService::class.java)

    fun hasAccess(context: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.isNotificationListenerAccessGranted(component(context))
        } else {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            enabled.contains(component(context).flattenToString())
        }
    }.getOrDefault(false)

    fun controller(context: Context): MediaController? = runCatching {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val sessions = manager.getActiveSessions(component(context))
        sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull { it.metadata != null }
    }.getOrNull()

    fun snapshot(context: Context): MediaSnapshot? {
        val controller = controller(context) ?: return null
        val metadata = controller.metadata ?: return null
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: return null
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: ""
        val art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        return MediaSnapshot(
            title = title,
            artist = artist,
            playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
            art = art?.asImageBitmap()
        )
    }

    fun playPause(context: Context) {
        val controller = controller(context) ?: return
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun next(context: Context) {
        controller(context)?.transportControls?.skipToNext()
    }

    fun previous(context: Context) {
        controller(context)?.transportControls?.skipToPrevious()
    }
}

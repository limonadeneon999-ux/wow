package com.carstudio.app.media

import android.service.notification.NotificationListenerService

/**
 * Service minimal : il ne lit aucune notification. Il sert uniquement de composant
 * autorisé pour que MediaSessionManager expose les sessions média actives.
 */
class MediaListenerService : NotificationListenerService()

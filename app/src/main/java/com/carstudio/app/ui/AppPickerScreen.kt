package com.carstudio.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carstudio.app.CarState
import com.carstudio.app.data.AppEntry
import com.carstudio.app.data.DemoApps
import com.carstudio.app.ui.components.AppIcon
import com.carstudio.app.ui.components.GlassCard
import com.carstudio.app.ui.theme.iconShapeFor

@Composable
fun AppPickerScreen(state: CarState, onClose: () -> Unit) {
    val settings = state.settings
    var query by remember { mutableStateOf("") }
    val source = if (settings.demoMode && state.allApps.isEmpty()) DemoApps.list else state.allApps
    val apps = remember(source, query) {
        if (query.isBlank()) source
        else source.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.pkg.contains(query, ignoreCase = true)
        }
    }
    val shape = iconShapeFor(settings.theme.iconShape, settings.theme.corner)

    Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Applications", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Étoile = favori · œil = afficher ou masquer",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Fermer")
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CircleShape,
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            placeholder = { Text("Rechercher une app…") }
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${apps.size} app" + if (apps.size > 1) "s" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${settings.favorites.size} favori" + if (settings.favorites.size > 1) "s" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(apps, key = { it.pkg }) { app ->
                AppPickerRow(
                    app = app,
                    shape = shape,
                    favorite = app.pkg in settings.favorites,
                    hidden = app.pkg in settings.hiddenApps,
                    onLaunch = { state.launch(app.pkg) },
                    onFavorite = { state.toggleFavorite(app.pkg) },
                    onHidden = { state.toggleHidden(app.pkg) }
                )
            }
        }
    }
}

@Composable
private fun AppPickerRow(
    app: AppEntry,
    shape: androidx.compose.ui.graphics.Shape,
    favorite: Boolean,
    hidden: Boolean,
    onLaunch: () -> Unit,
    onFavorite: () -> Unit,
    onHidden: () -> Unit
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    GlassCard(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app, 44.dp, shape, favorite = favorite)
            Spacer(Modifier.width(12.dp))
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onLaunch)
            ) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = onSurface.copy(alpha = if (hidden) 0.45f else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    app.pkg,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurface.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = if (favorite) "Retirer des favoris" else "Ajouter aux favoris",
                    modifier = Modifier.size(20.dp),
                    tint = if (favorite) primary else onSurface.copy(alpha = 0.4f)
                )
            }
            IconButton(onClick = onHidden) {
                Icon(
                    if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (hidden) "Afficher" else "Masquer",
                    modifier = Modifier.size(20.dp),
                    tint = if (hidden) primary else onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

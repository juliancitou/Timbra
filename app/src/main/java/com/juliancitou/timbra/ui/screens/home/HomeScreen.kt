package com.juliancitou.timbra.ui.screens.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassButtonStyle
import com.juliancitou.timbra.ui.theme.GlassCard
import com.juliancitou.timbra.ui.theme.GlassScreen

@Composable
fun HomeScreen(
    onCreateEqualization: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    GlassScreen(
        title = stringResource(R.string.app_name),
        subtitle = stringResource(R.string.home_subtitle),
        actions = {
            GlassButton(
                text = stringResource(R.string.action_create_equalization),
                onClick = onCreateEqualization,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassButton(
                text = stringResource(R.string.action_my_measurements),
                onClick = onOpenHistory,
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassButton(
                text = stringResource(R.string.action_settings),
                onClick = onOpenSettings,
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        GlassCard {
            Text(
                text = stringResource(R.string.home_how_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_how_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

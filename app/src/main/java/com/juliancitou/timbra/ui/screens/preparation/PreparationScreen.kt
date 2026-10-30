package com.juliancitou.timbra.ui.screens.preparation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassCard
import com.juliancitou.timbra.ui.theme.GlassChip
import com.juliancitou.timbra.ui.theme.GlassScreen

private val ChecklistItems = listOf(
    R.string.preparation_headphones,
    R.string.preparation_noise_cancelling,
    R.string.preparation_poweramp_off,
    R.string.preparation_quiet_place,
)

private val AllChecked = (1 shl ChecklistItems.size) - 1

@Composable
fun PreparationScreen(onContinue: () -> Unit, onBack: () -> Unit) {
    // Un bit por punto de la lista: sobrevive a rotaciones y a la recreación de la pantalla.
    var checkedMask by rememberSaveable { mutableIntStateOf(0) }

    GlassScreen(
        title = stringResource(R.string.preparation_title),
        subtitle = stringResource(R.string.preparation_subtitle),
        onBack = onBack,
        actions = {
            GlassButton(
                text = stringResource(R.string.action_continue),
                onClick = onContinue,
                enabled = checkedMask == AllChecked,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        GlassCard {
            ChecklistItems.forEachIndexed { index, labelRes ->
                val bit = 1 shl index
                GlassChip(
                    label = stringResource(labelRes),
                    selected = checkedMask and bit != 0,
                    onClick = { checkedMask = checkedMask xor bit },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        GlassCard {
            Text(
                text = stringResource(R.string.preparation_why),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

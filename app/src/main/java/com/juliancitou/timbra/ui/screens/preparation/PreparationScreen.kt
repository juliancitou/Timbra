package com.juliancitou.timbra.ui.screens.preparation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.CheckRow
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.Spacing
import com.juliancitou.timbra.ui.theme.TerminalButton
import com.juliancitou.timbra.ui.theme.TerminalScreen
import com.juliancitou.timbra.ui.theme.TerminalTextField
import com.juliancitou.timbra.ui.theme.staggeredEntrance

private val ChecklistItems = listOf(
    R.string.preparation_headphones,
    R.string.preparation_noise_cancelling,
    R.string.preparation_poweramp_off,
    R.string.preparation_quiet_place,
)

private val AllChecked = (1 shl ChecklistItems.size) - 1

/** Lista de preparación y el nombre de los audífonos, que será el nombre sugerido del preset. */
@Composable
fun PreparationScreen(
    headphonesName: String,
    onHeadphonesNameChange: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    // Un bit por punto de la lista: sobrevive a rotaciones y a la recreación de la pantalla.
    var checkedMask by rememberSaveable { mutableIntStateOf(0) }

    TerminalScreen(
        title = stringResource(R.string.preparation_title),
        lead = stringResource(R.string.preparation_lead),
        onBack = onBack,
        actions = {
            TerminalButton(
                text = stringResource(R.string.action_continue),
                onClick = onContinue,
                enabled = checkedMask == AllChecked,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column {
            ChecklistItems.forEachIndexed { index, labelRes ->
                val bit = 1 shl index
                CheckRow(
                    label = stringResource(labelRes),
                    checked = checkedMask and bit != 0,
                    onToggle = { checkedMask = checkedMask xor bit },
                    modifier = Modifier.staggeredEntrance(index),
                )
            }
        }
        TerminalTextField(
            value = headphonesName,
            onValueChange = onHeadphonesNameChange,
            label = stringResource(R.string.preparation_headphones_label),
            placeholder = stringResource(R.string.preparation_headphones_placeholder),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            SectionLabel(stringResource(R.string.preparation_section_why))
            DetailText(stringResource(R.string.preparation_why))
        }
    }
}

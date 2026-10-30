package com.juliancitou.timbra.ui.screens.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.juliancitou.timbra.BuildConfig
import com.juliancitou.timbra.R
import com.juliancitou.timbra.ui.theme.DetailText
import com.juliancitou.timbra.ui.theme.GlassButton
import com.juliancitou.timbra.ui.theme.GlassButtonStyle
import com.juliancitou.timbra.ui.theme.GlassCard
import com.juliancitou.timbra.ui.theme.GlassScreen
import com.juliancitou.timbra.ui.theme.Hairline
import com.juliancitou.timbra.ui.theme.LinkRow
import com.juliancitou.timbra.ui.theme.SectionLabel
import com.juliancitou.timbra.ui.theme.Spacing

/** Créditos, contacto y licencias. Se llega desde Ajustes → "Más información". */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    GlassScreen(
        title = stringResource(R.string.about_title),
        lead = stringResource(R.string.about_lead, CREATOR_ACCOUNT),
        onBack = onBack,
    ) {
        Section(R.string.about_section_contact) {
            LinkRow(label = stringResource(R.string.about_profile), url = CREATOR_PROFILE_URL)
            LinkRow(label = stringResource(R.string.about_repository), url = REPOSITORY_URL)
            LinkRow(label = stringResource(R.string.about_issues), url = ISSUES_URL)
        }
        Section(R.string.about_section_collaborate) {
            DetailText(stringResource(R.string.about_collaborate))
        }
        Section(R.string.about_section_libraries) {
            CreditList(Credits.libraries)
        }
        Section(R.string.about_section_references) {
            CreditList(Credits.references)
        }
        Section(R.string.about_section_licenses) {
            FontLicense()
        }
        DetailText(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
    }
}

@Composable
private fun Section(titleRes: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        SectionLabel(stringResource(titleRes))
        content()
    }
}

@Composable
private fun CreditList(credits: List<Credit>) {
    Column {
        credits.forEach { credit ->
            Hairline()
            LinkRow(label = "${credit.name} — ${credit.author} · ${credit.license}", url = credit.url)
        }
        Hairline()
    }
}

/** Texto completo de la licencia OFL de la tipografía, oculto hasta que se pide. */
@Composable
private fun FontLicense() {
    val context = LocalContext.current
    var visible by rememberSaveable { mutableStateOf(false) }
    val licenseText = remember(visible) {
        if (visible) context.assets.open(FONT_LICENSE_ASSET).bufferedReader().use { it.readText() } else ""
    }
    DetailText(stringResource(R.string.about_font_license_body))
    GlassButton(
        text = stringResource(if (visible) R.string.about_hide_license else R.string.about_show_license),
        onClick = { visible = !visible },
        style = GlassButtonStyle.Secondary,
        modifier = Modifier.fillMaxWidth(),
    )
    if (visible) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = licenseText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

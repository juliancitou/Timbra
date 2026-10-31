package com.juliancitou.timbra.ui.screens.about

/** Un proyecto o trabajo de terceros que Timbra usa. Nombres y licencias no se traducen. */
data class Credit(val name: String, val author: String, val license: String, val url: String)

const val CREATOR_ACCOUNT = "juliancitou"
const val CREATOR_PROFILE_URL = "https://github.com/juliancitou"
const val REPOSITORY_URL = "https://github.com/juliancitou/Timbra"
const val ISSUES_URL = "https://github.com/juliancitou/Timbra/issues"

/** Ruta de la licencia de la tipografía dentro de `assets/`, que la OFL exige distribuir con ella. */
const val FONT_LICENSE_ASSET = "licenses/cascadia-mono-OFL.txt"

/** Lo mismo que la sección "Agradecimientos" del README; si cambia uno, cambia el otro. */
object Credits {
    val libraries = listOf(
        Credit(
            "Jetpack Compose · Material 3",
            "Google / AndroidX",
            "Apache 2.0",
            "https://developer.android.com/compose",
        ),
        Credit(
            "Navigation Compose",
            "Google / AndroidX",
            "Apache 2.0",
            "https://developer.android.com/guide/navigation",
        ),
        Credit(
            "AndroidX Core · Activity · Lifecycle",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx",
        ),
        Credit("Kotlin · Coroutines · Serialization", "JetBrains", "Apache 2.0", "https://kotlinlang.org"),
        Credit(
            "Cascadia Mono",
            "Microsoft · vía Google Fonts",
            "SIL OFL 1.1",
            "https://github.com/microsoft/cascadia-code",
        ),
    )

    val references = listOf(
        Credit(
            "Digital Audio Resampling",
            "Julius O. Smith III",
            "Stanford CCRMA",
            "https://ccrma.stanford.edu/~jos/resample/",
        ),
        Credit("Audio EQ Cookbook", "Robert Bristow-Johnson", "W3C", "https://www.w3.org/TR/audio-eq-cookbook/"),
        Credit(
            "ITU-R BS.1770",
            "Unión Internacional de Telecomunicaciones",
            "ITU",
            "https://www.itu.int/rec/R-REC-BS.1770",
        ),
        Credit("Filtro de ruido rosa", "Paul Kellet · musicdsp.org", "Libre", "https://www.musicdsp.org/"),
        Credit(
            "Reverberación Schroeder · Freeverb",
            "Manfred Schroeder · Jezar at Dreampoint",
            "Dominio público",
            "https://ccrma.stanford.edu/~jos/pasp/Freeverb.html",
        ),
    )
}

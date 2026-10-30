package com.juliancitou.timbra

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.juliancitou.timbra.ui.screens.TimbraNavHost
import com.juliancitou.timbra.ui.theme.GlassBackground
import com.juliancitou.timbra.ui.theme.TimbraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            TimbraTheme {
                TimbraApp()
            }
        }
    }
}

/** Un solo fondo para toda la app: así la aurora no se reinicia ni se duplica al navegar. */
@Composable
fun TimbraApp() {
    GlassBackground {
        TimbraNavHost()
    }
}

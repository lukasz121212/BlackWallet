package pl.blackwallet

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    private val vm: WalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
        )
        setContent { BlackWalletTheme { App(vm) } }
    }
}

@Composable
fun BlackWalletTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFA8C7FA), onPrimary = Color(0xFF062E6F),
            background = Color.Black, onBackground = Color.White,
            surface = Color.Black, onSurface = Color.White,
            surfaceVariant = Color(0xFF1E1F20), onSurfaceVariant = Color(0xFFC4C7C5),
            surfaceContainerHigh = Color(0xFF1E1F20),
            primaryContainer = Color(0xFF0842A0), onPrimaryContainer = Color(0xFFD3E3FD),
            secondaryContainer = Color(0xFF2A3B55), onSecondaryContainer = Color.White,
        ),
        content = content,
    )
}

@Composable
fun App(vm: WalletViewModel) {
    val screen = vm.screen
    BackHandler(enabled = screen !is Screen.Home) {
        vm.screen = when (screen) {
            is Screen.Edit -> if (screen.id != null) Screen.Detail(screen.id) else Screen.Home
            else -> Screen.Home
        }
    }
    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 20 }) togetherWith fadeOut(tween(140))
            },
            label = "nav",
        ) { s ->
            when (s) {
                is Screen.Home -> HomeScreen(vm)
                is Screen.Detail -> DetailScreen(vm, s.id)
                is Screen.Edit -> EditorScreen(vm, s.id)
            }
        }
    }
}

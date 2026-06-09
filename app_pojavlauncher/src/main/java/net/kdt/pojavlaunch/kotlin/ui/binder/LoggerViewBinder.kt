package net.kdt.pojavlaunch.kotlin.ui.binder

import androidx.compose.ui.platform.ComposeView
import net.kdt.pojavlaunch.ui.screens.LoggerScreen
import net.kdt.pojavlaunch.ui.theme.PojavTheme

/**
 * A bridge to allow Java files to set Composable content without
 * running into functional interface issues.
 */
object LoggerViewBinder {
    @JvmStatic
    fun bind(composeView: ComposeView, onClose: Runnable) {
        composeView.setContent {
            PojavTheme {
                LoggerScreen(onClose = { onClose.run() })
            }
        }
    }
}
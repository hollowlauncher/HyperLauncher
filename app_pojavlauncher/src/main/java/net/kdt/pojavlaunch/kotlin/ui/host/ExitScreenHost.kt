package net.kdt.pojavlaunch.kotlin.ui.host

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import net.kdt.pojavlaunch.ExitActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.ui.screens.ExitScreen
import net.kdt.pojavlaunch.ui.theme.PojavTheme

object ExitScreenHost {
    @JvmStatic
    fun bind(
        view: ComposeView,
        activity: ExitActivity,
        title: String,
        logs: String,
        onCopyClick: Runnable,
        onRestartClick: Runnable,
        onOpenCrashReport: CrashReportListener
    ) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            PojavTheme {
                ExitScreen(
                    title = title,
                    logs = logs,
                    onShareClick = { Tools.shareLog(activity) },
                    onCopyClick = { onCopyClick.run() },
                    onRestartClick = { onRestartClick.run() },
                    onOpenCrashReport = { onOpenCrashReport.onOpenCrashReport(it) }
                )
            }
        }
    }

    fun interface CrashReportListener {
        fun onOpenCrashReport(path: String)
    }
}
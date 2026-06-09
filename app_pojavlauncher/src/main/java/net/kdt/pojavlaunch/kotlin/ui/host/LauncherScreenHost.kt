package net.kdt.pojavlaunch.kotlin.ui.host

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.FragmentManager
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.LauncherActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.fragments.MainMenuFragment
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener
import net.kdt.pojavlaunch.ui.screens.LauncherBackground
import net.kdt.pojavlaunch.ui.screens.LauncherScreen
import net.kdt.pojavlaunch.ui.theme.PojavTheme

object LauncherScreenHost {
    @JvmStatic
    fun bind(view: ComposeView, activity: LauncherActivity) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            PojavTheme {
                val fragmentManager = activity.supportFragmentManager

                fun checkFragmentOpen(): Boolean {
                    if (fragmentManager.backStackEntryCount > 0) return true
                    val currentFragment = fragmentManager.findFragmentById(R.id.container_fragment)
                    return currentFragment != null && currentFragment.tag != MainMenuFragment.Companion.TAG
                }

                var taskCount by remember { mutableIntStateOf(ProgressKeeper.getTaskCount()) }
                var isProgressVisible by rememberSaveable { mutableStateOf(false) }
                var isFragmentOpen by remember { mutableStateOf(checkFragmentOpen()) }

                DisposableEffect(fragmentManager) {
                    val taskCountListener = TaskCountListener { count ->
                        taskCount = count
                        false
                    }
                    val backStackListener = FragmentManager.OnBackStackChangedListener {
                        isFragmentOpen = checkFragmentOpen()
                    }

                    ProgressKeeper.addTaskCountListener(taskCountListener)
                    fragmentManager.addOnBackStackChangedListener(backStackListener)

                    onDispose {
                        ProgressKeeper.removeTaskCountListener(taskCountListener)
                        fragmentManager.removeOnBackStackChangedListener(backStackListener)
                    }
                }

                LauncherScreen(
                    onHomeRequest = { Tools.backToMainMenu(activity) },
                    onProgressClick = { isProgressVisible = !isProgressVisible },
                    isProgressVisible = isProgressVisible,
                    taskCount = taskCount,
                    isFragmentOpen = isFragmentOpen
                )
            }
        }
    }

    @JvmStatic
    fun bindBackground(view: ComposeView) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            PojavTheme {
                LauncherBackground()
            }
        }
    }
}
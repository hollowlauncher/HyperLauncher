package net.kdt.pojavlaunch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import net.kdt.pojavlaunch.ui.components.*

@Composable
fun QuickSettingComposeDialog(
    onResolutionChanged: () -> Unit,
    onGyroStateChanged: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val supportsGyro = remember { Tools.deviceSupportsGyro(context) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.quick_setting_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            var resolutionRatio by remember { mutableFloatStateOf(LauncherPreferences.PREF_SCALE_FACTOR * 100f) }
            PreferenceSlider(
                title = stringResource(R.string.mcl_setting_title_resolution_scaler),
                value = resolutionRatio,
                onValueChange = {
                    resolutionRatio = it
                    LauncherPreferences.PREF_SCALE_FACTOR = it / 100f
                    LauncherPreferences.DEFAULT_PREF?.edit { putInt("resolutionRatio", it.toInt()) }
                    onResolutionChanged()
                },
                valueRange = 25f..100f,
                summary = "${resolutionRatio.toInt()}%"
            )

            if (supportsGyro) {
                var enableGyro by remember { mutableStateOf(LauncherPreferences.PREF_ENABLE_GYRO) }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_enable_gyro_title),
                    checked = enableGyro,
                    onCheckedChange = {
                        enableGyro = it
                        LauncherPreferences.PREF_ENABLE_GYRO = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("enableGyro", it) }
                        onGyroStateChanged()
                    }
                )

                if (enableGyro) {
                    var gyroInvertX by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_INVERT_X) }
                    PreferenceSwitch(
                        title = stringResource(R.string.preference_gyro_invert_x_axis),
                        checked = gyroInvertX,
                        onCheckedChange = {
                            gyroInvertX = it
                            LauncherPreferences.PREF_GYRO_INVERT_X = it
                            LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("gyroInvertX", it) }
                            onGyroStateChanged()
                        }
                    )

                    var gyroInvertY by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_INVERT_Y) }
                    PreferenceSwitch(
                        title = stringResource(R.string.preference_gyro_invert_y_axis),
                        checked = gyroInvertY,
                        onCheckedChange = {
                            gyroInvertY = it
                            LauncherPreferences.PREF_GYRO_INVERT_Y = it
                            LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("gyroInvertY", it) }
                            onGyroStateChanged()
                        }
                    )

                    var gyroSensitivity by remember { mutableFloatStateOf(LauncherPreferences.PREF_GYRO_SENSITIVITY * 100f) }
                    PreferenceSlider(
                        title = stringResource(R.string.preference_gyro_sensitivity_title),
                        value = gyroSensitivity,
                        onValueChange = {
                            gyroSensitivity = it
                            LauncherPreferences.PREF_GYRO_SENSITIVITY = it / 100f
                            LauncherPreferences.DEFAULT_PREF?.edit { putInt("gyroSensitivity", it.toInt()) }
                        },
                        valueRange = 10f..300f,
                        summary = "${gyroSensitivity.toInt()}%"
                    )
                }
            }

            var mouseSpeed by remember { mutableFloatStateOf(LauncherPreferences.PREF_MOUSESPEED * 100f) }
            PreferenceSlider(
                title = stringResource(R.string.mcl_setting_title_mousespeed),
                value = mouseSpeed,
                onValueChange = {
                    mouseSpeed = it
                    LauncherPreferences.PREF_MOUSESPEED = it / 100f
                    LauncherPreferences.DEFAULT_PREF?.edit { putInt("mousespeed", it.toInt()) }
                },
                valueRange = 25f..300f,
                summary = "${mouseSpeed.toInt()}%"
            )

            var disableGestures by remember { mutableStateOf(LauncherPreferences.PREF_DISABLE_GESTURES) }
            PreferenceSwitch(
                title = stringResource(R.string.mcl_disable_gestures),
                checked = disableGestures,
                onCheckedChange = {
                    disableGestures = it
                    LauncherPreferences.PREF_DISABLE_GESTURES = it
                    LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("disableGestures", it) }
                }
            )

            if (!disableGestures) {
                var gestureDelay by remember { mutableFloatStateOf(LauncherPreferences.PREF_LONGPRESS_TRIGGER.toFloat()) }
                PreferenceSlider(
                    title = stringResource(R.string.mcl_setting_title_longpresstrigger),
                    value = gestureDelay,
                    onValueChange = {
                        gestureDelay = it
                        LauncherPreferences.PREF_LONGPRESS_TRIGGER = it.toInt()
                        LauncherPreferences.DEFAULT_PREF?.edit { putInt("timeLongPressTrigger", it.toInt()) }
                    },
                    valueRange = 100f..1000f,
                    summary = "${gestureDelay.toInt()} ms"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

object QuickSettingComposeBridge {
    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        onResolutionChanged: Runnable,
        onGyroStateChanged: Runnable
    ) {
        composeView.setContent {
            PojavTheme {
                QuickSettingComposeDialog(
                    onResolutionChanged = { onResolutionChanged.run() },
                    onGyroStateChanged = { onGyroStateChanged.run() }
                )
            }
        }
    }
}

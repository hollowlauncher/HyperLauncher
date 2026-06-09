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
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.EfficientAndroidLWJGLKeycode
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface
import net.kdt.pojavlaunch.customcontrols.buttons.ControlJoystick
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import net.kdt.pojavlaunch.ui.components.*

@Composable
fun EditControlComposeDialog(
    controlInterface: ControlInterface,
    onColorPick: (Boolean, Int, (Int) -> Unit) -> Unit,
    onBitmapPick: () -> Unit
) {
    val data = controlInterface.properties

    var name by remember(controlInterface) { mutableStateOf(data.name ?: "") }
    var width by remember(controlInterface) { mutableStateOf(data.getWidth().toString()) }
    var height by remember(controlInterface) { mutableStateOf(data.getHeight().toString()) }
    var isToggle by remember(controlInterface) { mutableStateOf(data.isToggle) }
    var passThru by remember(controlInterface) { mutableStateOf(data.passThruEnabled) }
    var isSwipeable by remember(controlInterface) { mutableStateOf(data.isSwipeable) }
    var displayInGame by remember(controlInterface) { mutableStateOf(data.displayInGame) }
    var displayInMenu by remember(controlInterface) { mutableStateOf(data.displayInMenu) }

    var opacity by remember(controlInterface) { mutableFloatStateOf(data.opacity) }
    var strokeWidth by remember(controlInterface) { mutableFloatStateOf(data.strokeWidth) }
    var cornerRadius by remember(controlInterface) { mutableFloatStateOf(data.cornerRadius) }

    val specialKeys = remember { ControlData.buildSpecialButtonArray() }
    val allKeyNames = remember { specialKeys + EfficientAndroidLWJGLKeycode.generateKeyName() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Edit Control",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (controlInterface !is ControlJoystick) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        data.name = it
                        controlInterface.setProperties(data, false)
                    },
                    label = { Text(stringResource(R.string.global_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = width,
                    onValueChange = {
                        width = it
                        it.toFloatOrNull()?.let { w ->
                            data.setWidth(w)
                            if (controlInterface is ControlJoystick) data.setHeight(w)
                            controlInterface.updateProperties()
                        }
                    },
                    label = { Text("Width") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = {
                        height = it
                        it.toFloatOrNull()?.let { h ->
                            data.setHeight(h)
                            if (controlInterface is ControlJoystick) data.setWidth(h)
                            controlInterface.updateProperties()
                        }
                    },
                    label = { Text("Height") },
                    modifier = Modifier.weight(1f)
                )
            }

            if (controlInterface !is ControlDrawer && controlInterface !is ControlJoystick) {
                Text(stringResource(R.string.customctrl_mapping), fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                    if (data.keycodes.size < 5) {
                        val newKeycodes = IntArray(5)
                        System.arraycopy(data.keycodes, 0, newKeycodes, 0, data.keycodes.size)
                        data.keycodes = newKeycodes
                    }

                    for (i in 0 until 5) {
                        val currentKeycode = data.keycodes[i]
                        val selectedIndex = if (currentKeycode < 0) {
                            currentKeycode + specialKeys.size
                        } else {
                            EfficientAndroidLWJGLKeycode.getIndexByValue(currentKeycode) + specialKeys.size
                        }

                        var expanded by remember(controlInterface) { mutableStateOf(false) }

                        Box {
                            OutlinedCard(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = allKeyNames.getOrElse(selectedIndex) { "Unknown" },
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                allKeyNames.forEachIndexed { index, keyName ->
                                    DropdownMenuItem(
                                        text = { Text(keyName) },
                                        onClick = {
                                            if (index < specialKeys.size) {
                                                data.keycodes[i] = index - specialKeys.size
                                            } else {
                                                data.keycodes[i] = EfficientAndroidLWJGLKeycode.getValueByIndex(index - specialKeys.size).toInt()
                                            }
                                            expanded = false
                                            controlInterface.updateProperties()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (controlInterface is ControlDrawer) {
                var expanded by remember(controlInterface) { mutableStateOf(false) }
                val orientations = remember { ControlDrawerData.getOrientations() }
                Text(stringResource(R.string.customctrl_orientation), fontWeight = FontWeight.Bold)
                Box {
                    OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(text = controlInterface.drawerData.orientation.toString(), modifier = Modifier.padding(12.dp))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        orientations.forEach { orientation ->
                            DropdownMenuItem(
                                text = { Text(orientation.toString()) },
                                onClick = {
                                    controlInterface.drawerData.orientation = orientation
                                    controlInterface.syncButtons()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            PreferenceGroup {
                if (controlInterface !is ControlDrawer && controlInterface !is ControlJoystick) {
                    PreferenceSwitch(
                        title = stringResource(R.string.customctrl_toggle),
                        checked = isToggle,
                        onCheckedChange = { isToggle = it; data.isToggle = it }
                    )
                    PreferenceSwitch(
                        title = stringResource(R.string.customctrl_passthru),
                        checked = passThru,
                        onCheckedChange = { passThru = it; data.passThruEnabled = it }
                    )
                    PreferenceSwitch(
                        title = stringResource(R.string.customctrl_swipeable),
                        checked = isSwipeable,
                        onCheckedChange = { isSwipeable = it; data.isSwipeable = it }
                    )
                }

                if (data is ControlJoystickData) {
                    var forwardLock by remember(controlInterface) { mutableStateOf(data.forwardLock) }
                    var absolute by remember(controlInterface) { mutableStateOf(data.absolute) }
                    PreferenceSwitch(
                        title = stringResource(R.string.customctrl_forward_lock),
                        checked = forwardLock,
                        onCheckedChange = { forwardLock = it; data.forwardLock = it; controlInterface.updateProperties() }
                    )
                    PreferenceSwitch(
                        title = stringResource(R.string.customctrl_absolute_tracking),
                        checked = absolute,
                        onCheckedChange = { absolute = it; data.absolute = it; controlInterface.updateProperties() }
                    )
                }
            }

            PreferenceGroup(title = "Appearance") {
                PreferenceSlider(
                    title = stringResource(R.string.customctrl_button_opacity),
                    value = opacity * 100f,
                    onValueChange = {
                        opacity = it / 100f
                        data.opacity = opacity
                        controlInterface.controlView.alpha = opacity
                    },
                    valueRange = 0f..100f
                )

                if (data.bitmapTag == null) {
                    PreferenceSlider(
                        title = stringResource(R.string.customctrl_stroke_width),
                        value = strokeWidth * 10f,
                        onValueChange = {
                            strokeWidth = it / 10f
                            data.strokeWidth = strokeWidth
                            controlInterface.setBackground()
                        },
                        valueRange = 0f..100f
                    )
                    PreferenceSlider(
                        title = stringResource(R.string.customctrl_corner_radius),
                        value = cornerRadius,
                        onValueChange = {
                            cornerRadius = it
                            data.cornerRadius = it
                            controlInterface.setBackground()
                        },
                        valueRange = 0f..100f
                    )
                }
            }

            PreferenceGroup(title = "Colors & Bitmap") {
                PreferenceItem(
                    title = stringResource(R.string.customctrl_background_color),
                    onClick = {
                        onColorPick(true, data.bgColor) { color ->
                            data.bitmapTag = null
                            data.bgColor = color
                            controlInterface.setBackground()
                        }
                    }
                )
                if (data.bitmapTag == null) {
                    PreferenceItem(
                        title = stringResource(R.string.customctrl_stroke_color),
                        onClick = {
                            onColorPick(false, data.strokeColor) { color ->
                                data.strokeColor = color
                                controlInterface.setBackground()
                            }
                        }
                    )
                }
                PreferenceItem(
                    title = stringResource(R.string.customctrl_background_bitmap),
                    onClick = onBitmapPick
                )
            }

            PreferenceGroup(title = stringResource(R.string.customctrl_visibility_title)) {
                PreferenceSwitch(
                    title = stringResource(R.string.customctrl_visibility_ingame),
                    checked = displayInGame,
                    onCheckedChange = { displayInGame = it; data.isHideable = true; data.displayInGame = it }
                )
                PreferenceSwitch(
                    title = stringResource(R.string.customctrl_visibility_in_menus),
                    checked = displayInMenu,
                    onCheckedChange = { displayInMenu = it; data.isHideable = true; data.displayInMenu = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

object EditControlComposeBridge {
    interface OnColorPickListener {
        fun onColorPick(isBg: Boolean, color: Int, resultListener: OnColorResultListener)
    }

    interface OnColorResultListener {
        fun onResult(color: Int)
    }

    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        controlInterface: ControlInterface,
        onColorPickListener: OnColorPickListener,
        onBitmapPick: Runnable
    ) {
        composeView.setContent {
            PojavTheme {
                EditControlComposeDialog(
                    controlInterface = controlInterface,
                    onColorPick = { isBg, color, onResult ->
                        onColorPickListener.onColorPick(isBg, color, object : OnColorResultListener {
                            override fun onResult(color: Int) {
                                onResult(color)
                            }
                        })
                    },
                    onBitmapPick = { onBitmapPick.run() }
                )
            }
        }
    }
}

package org.lwjgl.glfw;

import android.content.ClipData;
import android.content.ClipDescription;
import android.util.Log;
import android.view.Choreographer;
import android.view.KeyEvent;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.MainActivity;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.customcontrols.gamepad.direct.DirectGamepadEnableHandler;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import git.artdeell.dnbootstrap.glfw.GLFW;

public class CallbackBridge {
    public static final Choreographer sChoreographer = Choreographer.getInstance();
    // Use a weak reference here to avoid possibly statically referencing a Context.
    private static @Nullable WeakReference<DirectGamepadEnableHandler> sDirectGamepadEnableHandler;

    public static final int CLIPBOARD_COPY = 2000;
    public static final int CLIPBOARD_PASTE = 2001;
    public static final int CLIPBOARD_OPEN = 2002;

    public static volatile int windowWidth, windowHeight;
    public volatile static boolean holdingAlt, holdingCapslock, holdingCtrl,
            holdingNumlock, holdingShift;

    public static final ByteBuffer sGamepadButtonBuffer;
    public static final FloatBuffer sGamepadAxisBuffer;
    public static boolean sGamepadDirectInput = false;

    public static void performClick(int button) {
        double ox = GLFW.cursorX, oy = GLFW.cursorY;
        GLFW.sendMouseEvent(button, 1, CallbackBridge.getCurrentMods());
        sChoreographer.postFrameCallbackDelayed(l -> {
            GLFW.cursorX = ox;
            GLFW.cursorY = oy;
            GLFW.sendMouseEvent(button, 0, CallbackBridge.getCurrentMods());
        }, 33);
    }

    public static void sendKeyPress(int keyCode, int modifiers, boolean status) {
        GLFW.sendKeyEvent(keyCode, status, modifiers);
    }


    public static void sendKeyPress(int keyCode) {
        GLFW.sendKeyEvent(keyCode, true, getCurrentMods());
        GLFW.sendKeyEvent(keyCode, false, getCurrentMods());
    }

    public static void sendMouseButton(int button, boolean status) {
        CallbackBridge.sendMouseKeycode(button, CallbackBridge.getCurrentMods(), status);
    }

    public static void sendMouseKeycode(int button, int modifiers, boolean isDown) {
        GLFW.sendMouseEvent(button, isDown ? 1 : 0, modifiers);
    }

    public static void sendScroll(double xoffset, double yoffset) {
        GLFW.sendScrollEvent(xoffset, yoffset);
    }

    public static boolean isGrabbing() {
        // Avoid going through the JNI each time.
        return GLFW.isGrabbing();
    }

    // Called from JRE side
    @SuppressWarnings("unused")
    @Keep
    public static @Nullable String accessAndroidClipboard(int type, String copy) {
        switch (type) {
            case CLIPBOARD_COPY:
                MainActivity.GLOBAL_CLIPBOARD.setPrimaryClip(ClipData.newPlainText("Copy", copy));
                return null;

            case CLIPBOARD_PASTE:
                if (MainActivity.GLOBAL_CLIPBOARD.hasPrimaryClip() && MainActivity.GLOBAL_CLIPBOARD.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    return MainActivity.GLOBAL_CLIPBOARD.getPrimaryClip().getItemAt(0).getText().toString();
                } else {
                    return "";
                }

            case CLIPBOARD_OPEN:
                MainActivity.openLink(copy);
                return null;
            default: return null;
        }
    }


    public static int getCurrentMods() {
        int currMods = 0;
        if (holdingAlt) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_ALT;
        } if (holdingCapslock) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_CAPS_LOCK;
        } if (holdingCtrl) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_CONTROL;
        } if (holdingNumlock) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_NUM_LOCK;
        } if (holdingShift) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_SHIFT;
        }
        return currMods;
    }

    public static void setModifiers(KeyEvent keyEvent) {
        CallbackBridge.holdingAlt = keyEvent.isAltPressed();
        CallbackBridge.holdingCapslock = keyEvent.isCapsLockOn();
        CallbackBridge.holdingCtrl = keyEvent.isCtrlPressed();
        CallbackBridge.holdingNumlock = keyEvent.isNumLockOn();
        CallbackBridge.holdingShift = keyEvent.isShiftPressed();
    }

    public static void setModifiers(int keyCode, boolean isDown){
        switch (keyCode){
            case LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT:
                CallbackBridge.holdingShift = isDown;
                return;

            case LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL:
                CallbackBridge.holdingCtrl = isDown;
                return;

            case LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT:
                CallbackBridge.holdingAlt = isDown;
                return;

            case LwjglGlfwKeycode.GLFW_KEY_CAPS_LOCK:
                CallbackBridge.holdingCapslock = isDown;
                return;

            case LwjglGlfwKeycode.GLFW_KEY_NUM_LOCK:
                CallbackBridge.holdingNumlock = isDown;
        }
    }

    @Keep
    private static void onDirectInputEnable() {
        Log.i("CallbackBridge", "onDirectInputEnable()");
        DirectGamepadEnableHandler enableHandler = Tools.getWeakReference(sDirectGamepadEnableHandler);
        if(enableHandler != null) enableHandler.onDirectGamepadEnabled();
        sGamepadDirectInput = true;
    }

    public static FloatBuffer createGamepadAxisBuffer() {
        ByteBuffer axisByteBuffer = ByteBuffer.allocate(50); // TODO;
        // NOTE: hardcoded order (also in jre_lwjgl3glfw CallbackBridge)
        return axisByteBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
    }

    public static void setDirectGamepadEnableHandler(DirectGamepadEnableHandler h) {
        sDirectGamepadEnableHandler = new WeakReference<>(h);
    }

    static {
        System.loadLibrary("pojavexec");
        sGamepadButtonBuffer = ByteBuffer.allocate(50); // TODO
        sGamepadAxisBuffer = createGamepadAxisBuffer();
    }
}


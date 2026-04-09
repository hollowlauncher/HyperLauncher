package net.kdt.pojavlaunch;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Choreographer;
import android.view.KeyEvent;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.customcontrols.gamepad.direct.DirectGamepadEnableHandler;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import git.artdeell.dnbootstrap.glfw.GLFW;

public class CallbackBridge {
    public static final Choreographer sChoreographer = Choreographer.getInstance();
    // Use a weak reference here to avoid possibly statically referencing a Context.
    private static @Nullable WeakReference<DirectGamepadEnableHandler> sDirectGamepadEnableHandler;

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
    public static void openLink(String link) {
        ContextExecutor.executeActivity(ctx->{
            try {
                if(link.startsWith("file:")) {
                    int truncLength = 5;
                    if(link.startsWith("file://")) truncLength = 7;
                    String path = link.substring(truncLength);
                    Tools.openPath(ctx, new File(path), false);
                }else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(link), "*/*");
                    ctx.startActivity(intent);
                }
            } catch (Throwable th) {
                Tools.showError(ctx, th);
            }
        });
    }

    @SuppressWarnings("unused") //TODO: actually use it
    public static void openPath(String path) {
        ContextExecutor.executeActivity(ctx->{
            try {
                Tools.openPath(ctx, new File(path), false);
            } catch (Throwable th) {
                Tools.showError(ctx, th);
            }
        });
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

    public static native void minibridgeInit();

    static {
        System.loadLibrary("pojavexec");
        minibridgeInit();
        sGamepadButtonBuffer = ByteBuffer.allocate(50); // TODO
        sGamepadAxisBuffer = createGamepadAxisBuffer();
    }
}


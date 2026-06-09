package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Tools.dialogForceClose;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_ENABLE_GYRO;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_SUSTAINED_PERFORMANCE;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_USE_ALTERNATE_SURFACE;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_VIRTUAL_MOUSE_START;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.compose.ui.platform.ComposeView;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.kdt.LoggerView;

import net.kdt.pojavlaunch.authenticator.accounts.Accounts;
import net.kdt.pojavlaunch.customcontrols.ControlButtonMenuListener;
import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData;
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.CustomControls;
import net.kdt.pojavlaunch.customcontrols.EditorExitable;
import net.kdt.pojavlaunch.customcontrols.keyboard.LwjglCharSender;
import net.kdt.pojavlaunch.customcontrols.keyboard.TouchCharInput;
import net.kdt.pojavlaunch.customcontrols.mouse.GyroControl;
import net.kdt.pojavlaunch.customcontrols.mouse.HotbarView;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.prefs.QuickSettingSideDialog;
import net.kdt.pojavlaunch.services.GameService;
import net.kdt.pojavlaunch.tasks.AsyncAssetManager;
import net.kdt.pojavlaunch.utils.JREUtils;
import net.kdt.pojavlaunch.utils.MCOptionUtils;
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount;
import net.kdt.pojavlaunch.utils.RendererCompatUtil;
import net.kdt.pojavlaunch.utils.jre.GameRunner;
import net.kdt.pojavlaunch.kotlin.ui.host.LauncherScreenHost;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Objects;

import git.artdeell.dnbootstrap.glfw.AndroidClipboardProvider;

import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.GLFWCursorView;
import net.ashmeet.hyperlauncher.R;

public class MainActivity extends BaseActivity implements ControlButtonMenuListener, EditorExitable, ServiceConnection, NavigationView.OnNavigationItemSelectedListener {
    public static final String INTENT_MINECRAFT_VERSION = "intent_version";
    public static final String INTENT_MINECRAFT_CLASSPATH = "intent_classpath";

    public static TouchCharInput touchCharInput;
    private MinecraftGLSurface minecraftGLView;
    private static WeakReference<GLFWCursorView> weakCursor;
    private GLFWCursorView cursor;
    private LoggerView loggerView;
    private DrawerLayout drawerLayout;
    private NavigationView navDrawer;
    private View mDrawerPullButton;
    private GyroControl mGyroControl = null;
    private ControlLayout mControlLayout;
    private HotbarView mHotbarView;
    private volatile AndroidClipboardProvider mClipboardProvider;

    Instance instance;
    MinecraftAccount minecraftAccount;

    private GameService.LocalBinder mServiceBinder;

    private QuickSettingSideDialog mQuickSettingSideDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState) ;
        instance = Instances.loadSelectedInstance();
        minecraftAccount = Accounts.getCurrent();
        if(instance == null) {
            Toast.makeText(this, R.string.instance_dir_missing, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        AsyncAssetManager.extractDefaultSettings(this, instance.getGameDirectory());
        MCOptionUtils.load(instance.getGameDirectory().getAbsolutePath());

        Intent gameServiceIntent = new Intent(this, GameService.class);

        ContextCompat.startForegroundService(this, gameServiceIntent);
        initLayout(R.layout.activity_basemain);
        GLFW.addGrabListener(minecraftGLView);

        mGyroControl = new GyroControl(this);

        if(PREF_USE_ALTERNATE_SURFACE) getWindow().setBackgroundDrawable(null);
        else getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            getWindow().setSustainedPerformanceMode(PREF_SUSTAINED_PERFORMANCE);

        MCOptionUtils.MCOptionListener optionListener = MCOptionUtils::getMcScale;
        MCOptionUtils.addMCOptionListener(optionListener);
        mControlLayout.setModifiable(false);

        ContextExecutor.setActivity(this);

        bindService(gameServiceIntent, this, 0);
    }

    protected void initLayout(int resId) {
        setContentView(resId);
        bindValues();

        drawerLayout.setFitsSystemWindows(false);

        drawerLayout.setOnApplyWindowInsetsListener((v, insets) -> insets.consumeSystemWindowInsets());

        mControlLayout.setMenuListener(this);

        mDrawerPullButton.setOnClickListener(v -> onClickedMenu());
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        try {
            File latestLogFile = new File(Tools.DIR_GAME_HOME, "latestlog.txt");
            if(!latestLogFile.exists() && !latestLogFile.createNewFile())
                throw new IOException("Failed to create a new log file");
            Logger.begin(latestLogFile.getAbsolutePath());

            mClipboardProvider = new AndroidClipboardProvider(getApplicationContext());
            GLFW.setClipboardImpl(mClipboardProvider);

            touchCharInput.setCharacterSender(new LwjglCharSender());

            Bundle extras = Objects.requireNonNull(getIntent().getExtras());
            String version = extras.getString(INTENT_MINECRAFT_VERSION);
            File[] classpath = (File[]) extras.getSerializable(INTENT_MINECRAFT_CLASSPATH);

            setTitle("Minecraft " + version);

            navDrawer.setNavigationItemSelectedListener(this);
            drawerLayout.closeDrawers();

            minecraftGLView.setSurfaceReadyListener(() -> {
                try {
                    if(!PREF_VIRTUAL_MOUSE_START) cursor.setVisibility(View.GONE);
                    runCraft(version, classpath);
                }catch (Throwable e){
                    Tools.showErrorRemote(e);
                }
            });
        } catch (Throwable e) {
            Tools.showError(this, e, true);
        }
    }

    private void loadControls() {
        try {

            mControlLayout.loadLayout(instance.getLaunchControls());
        } catch(IOException e) {
            try {
                Log.w("MainActivity", "Unable to load the control file, loading the default now", e);
                mControlLayout.loadLayout(Tools.CTRLDEF_FILE);
            } catch (IOException ioException) {
                Tools.showError(this, ioException);
            }
        } catch (Throwable th) {
            Tools.showError(this, th);
        }
        mDrawerPullButton.setVisibility(mControlLayout.hasMenuButton() ? View.GONE : View.VISIBLE);
        mControlLayout.toggleControlVisible();
    }

    @Override
    public void onAttachedToWindow() {

        mControlLayout.post(()->{
            Tools.getDisplayMetrics(this);
            loadControls();
        });
    }

    /** Boilerplate binding */
    private void bindValues(){
        mControlLayout = findViewById(R.id.main_control_layout);
        minecraftGLView = findViewById(R.id.main_game_render_view);
        cursor = findViewById(R.id.main_touchpad);
        weakCursor = new WeakReference<>(cursor);
        drawerLayout = findViewById(R.id.main_drawer_options);
        navDrawer = findViewById(R.id.main_navigation_view);
        loggerView = findViewById(R.id.mainLoggerView);
        touchCharInput = findViewById(R.id.mainTouchCharInput);
        mDrawerPullButton = findViewById(R.id.drawer_button);
        mHotbarView = findViewById(R.id.hotbar_view);

        ComposeView backgroundCompose = findViewById(R.id.main_background_compose);
        if (backgroundCompose != null) {
            LauncherScreenHost.bindBackground(backgroundCompose);
        }
        updateCursorFromPrefs();
    }

    private void updateCursorFromPrefs() {
        if (cursor == null) return;
        cursor.setCursorScale(LauncherPreferences.PREF_MOUSESCALE);
        String path = LauncherPreferences.PREF_MOUSE_ICON_PATH;
        if (path != null && new File(path).exists()) {
            Drawable d = Drawable.createFromPath(path);
            if (d != null) {
                float hotX = LauncherPreferences.PREF_MOUSE_HOTSPOT_X / 100f * d.getIntrinsicWidth();
                float hotY = LauncherPreferences.PREF_MOUSE_HOTSPOT_Y / 100f * d.getIntrinsicHeight();
                cursor.setCustomCursor(d, hotX, hotY);
            }
        } else {
            cursor.setCustomCursor(null, 0, 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ContextExecutor.setActivity(this);
        if(PREF_ENABLE_GYRO) mGyroControl.enable();
        updateCursorFromPrefs();

    }

    @Override
    protected void onPause() {
        ContextExecutor.clearActivity();
        mGyroControl.disable();

        if (GLFW.isGrabbing()){
            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE);
        }
        if(mQuickSettingSideDialog != null) {
            mQuickSettingSideDialog.cancel();
        }

        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ContextExecutor.clearActivity();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(mGyroControl != null) mGyroControl.updateOrientation();

        if(mControlLayout == null) return;
        mControlLayout.requestLayout();
        mControlLayout.post(()->{

            minecraftGLView.refreshSize();
            mControlLayout.refreshControlButtonPositions();
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(minecraftGLView != null)
            Tools.MAIN_HANDLER.postDelayed(() -> minecraftGLView.refreshSize(), 500);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {

            if(!Tools.checkStorageRoot(this)) return;
            LauncherPreferences.loadPreferences(getApplicationContext());
            try {
                mControlLayout.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH);
            } catch (IOException e) {
                e.printStackTrace();
            }
            updateCursorFromPrefs();
        }
    }

    private void runCraft(String versionId, File[] classpath) throws Throwable {
        String renderer = instance.getLaunchRenderer();
        if(!RendererCompatUtil.checkRendererCompatible(this, renderer)) {
            RendererCompatUtil.RenderersList renderersList = RendererCompatUtil.getCompatibleRenderers(this);
            String firstCompatibleRenderer = renderersList.rendererIds.get(0);
            Log.w("runCraft","Incompatible renderer "+renderer+ " will be replaced with "+firstCompatibleRenderer);
            renderer = firstCompatibleRenderer;
        }
        Logger.appendToLog("--------- Starting game with Launcher Debug!");
        Tools.printLauncherInfo(versionId, instance.getLaunchArgs(), renderer);
        JREUtils.redirectAndPrintJRELog();
        GameRunner.launchMinecraft(this, minecraftAccount, instance, versionId, classpath, renderer);

        Tools.runOnUiThread(()-> {
            if (mServiceBinder != null) mServiceBinder.isActive = false;
        });
    }

    private void dialogSendCustomKey() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.control_customkey)
                .setItems(EfficientAndroidLWJGLKeycode.generateKeyName(), (dInterface, position) -> EfficientAndroidLWJGLKeycode.execKeyIndex(position))
                .show();
    }

    boolean isInEditor;
    private void openCustomControls() {
        mControlLayout.setModifiable(true);
        navDrawer.getMenu().clear();
        navDrawer.inflateMenu(R.menu.drawer_custom_controls);

        mDrawerPullButton.setVisibility(View.VISIBLE);
        isInEditor = true;
    }

    private void openLogOutput() {
        loggerView.setVisibility(View.VISIBLE);
    }

    private void openQuickSettings() {
        if(mQuickSettingSideDialog == null) {
            mQuickSettingSideDialog = new QuickSettingSideDialog(this, mControlLayout) {
                @Override
                public void onResolutionChanged() {
                    minecraftGLView.refreshSize();
                    mHotbarView.onResolutionChanged();
                }

                @Override
                public void onGyroStateChanged() {
                    mGyroControl.updateOrientation();
                    if (PREF_ENABLE_GYRO) {
                        mGyroControl.enable();
                    } else {
                        mGyroControl.disable();
                    }
                }
            };
        }
        mQuickSettingSideDialog.appear(true);
    }

    public static void toggleMouse(Context ctx) {

        if (GLFW.isGrabbing()) return;
        GLFWCursorView cursorView = Tools.getWeakReference(weakCursor);
        if(cursorView == null) return;
        int toastString = 0;
        switch (cursorView.getVisibility()) {
            case View.GONE:
            case View.INVISIBLE:
                toastString = R.string.control_mouseon;
                cursorView.setVisibility(View.VISIBLE);
                break;
            case View.VISIBLE:
                toastString = R.string.control_mouseoff;
                cursorView.setVisibility(View.GONE);
                break;
        }

        if(toastString != 0) Toast.makeText(ctx, toastString, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(isInEditor) {
            if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if(event.getAction() == KeyEvent.ACTION_DOWN) mControlLayout.askToExit(this);
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        boolean handleEvent;
        if(!(handleEvent = minecraftGLView.processKeyEvent(event))) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && !touchCharInput.isEnabled()) {
                if(event.getAction() != KeyEvent.ACTION_UP) return true;
                CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE);
                return true;
            }
        }
        return handleEvent;
    }

    public static void switchKeyboardState() {
        if(touchCharInput != null) touchCharInput.switchKeyboardState();
    }

    @Override
    public void onClickedMenu() {
        drawerLayout.openDrawer(navDrawer);
    }

    @Override
    public void exitEditor() {
        try {
            mControlLayout.loadLayout((CustomControls)null);
            mControlLayout.setModifiable(false);
            System.gc();
            mControlLayout.loadLayout(instance.getLaunchControls());
            mDrawerPullButton.setVisibility(mControlLayout.hasMenuButton() ? View.GONE : View.VISIBLE);
        } catch (Exception e) {
            Tools.showError(this,e);
        }

        navDrawer.getMenu().clear();
        navDrawer.inflateMenu(R.menu.drawer_main);
        isInEditor = false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        GameService.LocalBinder localBinder = (GameService.LocalBinder) service;
        mServiceBinder = localBinder;
        minecraftGLView.start(localBinder.isActive, cursor);
        localBinder.isActive = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_force_close) {
            dialogForceClose(this);
        } else if (id == R.id.menu_view_output) {
            openLogOutput();
        } else if (id == R.id.menu_custom_key) {
            dialogSendCustomKey();
        } else if (id == R.id.menu_quick_settings) {
            openQuickSettings();
        } else if (id == R.id.menu_custom_controls) {
            openCustomControls();
        } else if (id == R.id.menu_add_button) {
            mControlLayout.addControlButton(new ControlData("New"));
        } else if (id == R.id.menu_add_drawer) {
            mControlLayout.addDrawer(new ControlDrawerData());
        } else if (id == R.id.menu_add_joystick) {
            mControlLayout.addJoystickButton(new ControlJoystickData());
        } else if (id == R.id.menu_load_layout) {
            mControlLayout.openLoadDialog();
        } else if (id == R.id.menu_save_layout) {
            mControlLayout.openSaveDialog(this);
        } else if (id == R.id.menu_select_default) {
            mControlLayout.openSetDefaultDialog();
        } else if (id == R.id.menu_exit_editor) {
            mControlLayout.openExitDialog(this);
        }

        drawerLayout.closeDrawers();
        return true;
    }

    /*
     * Android 14 (or some devices, at least) seems to dispatch the the captured mouse events as trackball events
     * due to a bug(?) somewhere(????)
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean checkCaptureDispatchConditions(MotionEvent event) {
        int eventSource = event.getSource();

        return (eventSource & InputDevice.SOURCE_MOUSE_RELATIVE) != 0 ||
                (eventSource & InputDevice.SOURCE_MOUSE) != 0;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        if(Tools.isAndroid8OrHigher() && checkCaptureDispatchConditions(ev))
            return minecraftGLView.dispatchGenericMotionEvent(ev);
        else return super.dispatchTrackballEvent(ev);
    }
}

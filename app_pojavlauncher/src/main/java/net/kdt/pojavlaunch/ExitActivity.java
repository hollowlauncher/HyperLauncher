package net.kdt.pojavlaunch;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.platform.ComposeView;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.kotlin.ui.host.ExitScreenHost;

import java.io.File;
import java.io.IOException;

@Keep
public class ExitActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        int code = -1;
        boolean isSignal = false;
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            code = extras.getInt("code",-1);
            isSignal = extras.getBoolean("isSignal", false);
        }

        String title = isSignal ? getString(R.string.mcn_abort_title) : getString(R.string.mcn_exit_title, code);
        String logs = readLogs();

        ComposeView composeView = new ComposeView(this);
        setContentView(composeView);

        ExitScreenHost.bind(
                composeView,
                this,
                title,
                logs,
                () -> copyToClipboard(logs),
                this::restartLauncher,
                this::openCrashReport
        );
    }

    private String readLogs() {
        try {
            File logFile = new File(Tools.DIR_GAME_HOME, "latestlog.txt");
            if (logFile.exists()) {
                return Tools.read(logFile);
            } else {
                return "Log file not found.";
            }
        } catch (IOException e) {
            return "Failed to read logs: " + e.getMessage();
        }
    }

    public void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("PojavLauncher Logs", text);
        clipboard.setPrimaryClip(clip);
    }

    public void restartLauncher() {
        Intent intent = new Intent(this, LauncherActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    public void openCrashReport(String path) {
        File file = new File(path);
        if (file.exists()) {
            Tools.openPath(this, file, false);
        }
    }

    @SuppressWarnings("unused")
    public static void showExitMessage(Context ctx, int code, boolean isSignal) {
        if((!isSignal && code == 0) || ctx == null) {
            System.exit(0);
            return;
        }

        Object lock = new Object();
        Tools.runOnUiThread(()->{
            Intent i = new Intent(ctx, ExitActivity.class);
            i.putExtra("code", code);
            i.putExtra("isSignal", isSignal);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            synchronized (lock) {
                lock.notify();
            }
        });
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Log.e("ExitActivity", "Waiting on lock failed: "+e);
            }
        }
        System.exit(0);
    }

}

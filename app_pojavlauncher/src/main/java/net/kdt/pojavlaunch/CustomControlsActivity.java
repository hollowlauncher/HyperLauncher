package net.kdt.pojavlaunch;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.compose.ui.platform.ComposeView;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.JsonSyntaxException;

import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData;
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.EditorExitable;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.CropperUtils;
import net.kdt.pojavlaunch.kotlin.ui.host.LauncherScreenHost;

import java.io.IOException;

import net.ashmeet.hyperlauncher.R;

public class CustomControlsActivity extends BaseActivity implements EditorExitable, CropperUtils.CropperReceiver {
	private DrawerLayout mDrawerLayout;
	private NavigationView mDrawerNavigationView;
	private ControlLayout mControlLayout;
	private CropperUtils.CropperReceiver mCropperReceiver;
	private ActivityResultLauncher<?> mCropperLauncher;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCropperLauncher = CropperUtils.registerCropper(this, this);

		setContentView(R.layout.activity_custom_controls);

		mControlLayout = findViewById(R.id.customctrl_controllayout);
		mDrawerLayout = findViewById(R.id.customctrl_drawerlayout);
		mDrawerNavigationView = findViewById(R.id.customctrl_navigation_view);
		View mPullDrawerButton = findViewById(R.id.drawer_button);

		ComposeView backgroundCompose = findViewById(R.id.customctrl_background_compose);
		if (backgroundCompose != null) {
			LauncherScreenHost.bindBackground(backgroundCompose);
		}

		mPullDrawerButton.setOnClickListener(v -> mDrawerLayout.openDrawer(mDrawerNavigationView));
		mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		mDrawerNavigationView.setNavigationItemSelectedListener(item -> {
			int id = item.getItemId();
			if (id == R.id.menu_add_button) {
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
			} else if (id == R.id.menu_export_layout) {
				try {
					Uri contentUri = DocumentsContract.buildDocumentUri(getString(R.string.storageProviderAuthorities), mControlLayout.saveToDirectory(mControlLayout.mLayoutFileName));

					Intent shareIntent = new Intent();
					shareIntent.setAction(Intent.ACTION_SEND);
					shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
					shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					shareIntent.setType("application/json");
					startActivity(shareIntent);

					Intent sendIntent = Intent.createChooser(shareIntent, mControlLayout.mLayoutFileName);
					startActivity(sendIntent);
				} catch (Exception e) {
					Tools.showError(this, e);
				}
			} else if (id == R.id.menu_exit_editor) {
				mControlLayout.openExitDialog(this);
			}
			mDrawerLayout.closeDrawers();
			return true;
		});
		mControlLayout.setModifiable(true);
	}

	@Override
	public void onAttachedToWindow() {
		mControlLayout.post(()->{
			try {
				mControlLayout.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH);
			}catch (IOException | JsonSyntaxException e) {
				Tools.showError(this, e);
			}
		});
	}

	public void startCropping(CropperUtils.CropperReceiver cropperReceiver) {
		mCropperReceiver = cropperReceiver;
		CropperUtils.startCropper(mCropperLauncher);
	}

	@Override
	public void onBackPressed() {
		mControlLayout.askToExit(this);
	}

	@Override
	public void exitEditor() {
		super.onBackPressed();
	}

	@Override
	public float getAspectRatio() {
		if(mCropperReceiver != null) return mCropperReceiver.getAspectRatio();
		return 1f;
	}

	@Override
	public int getTargetMaxSide() {
		if(mCropperReceiver != null) return mCropperReceiver.getTargetMaxSide();
		return 128;
	}

	@Override
	public void onCropped(Bitmap contentBitmap) {
		if(mCropperReceiver != null) mCropperReceiver.onCropped(contentBitmap);
	}

	@Override
	public void onFailed(Exception exception) {
		if(mCropperReceiver != null) onFailed(exception);
	}
}

package net.kdt.pojavlaunch.customcontrols.handleview;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;

public class DeleteButton extends MaterialButton implements ActionButtonInterface {
    public DeleteButton(Context context) {super(context); init();}
    public DeleteButton(Context context, @Nullable AttributeSet attrs) {super(context, attrs); init();}

    public void init() {
        setOnClickListener(this);
        setIcon(null);
        setText(R.string.global_delete);
        setPadding(0, 0, 0, 0);
        setInsetBottom(0);
        setInsetTop(0);

        // Apply error color to match the "terminate" button style in MainMenuScreen
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorError, typedValue, true);
        setBackgroundTintList(ColorStateList.valueOf(typedValue.data));

        getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnError, typedValue, true);
        setTextColor(typedValue.data);
    }

    private ControlInterface mCurrentlySelectedButton = null;

    @Override
    public boolean shouldBeVisible() {
        return mCurrentlySelectedButton != null;
    }

    @Override
    public void setFollowedView(ControlInterface view) {
        mCurrentlySelectedButton = view;
    }

    @Override
    public void onClick() {
        if(mCurrentlySelectedButton == null) return;

        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.global_delete)
                .setMessage(R.string.mcl_delete_control)
                .setPositiveButton(R.string.global_delete, (dialog, which) -> {
                    mCurrentlySelectedButton.removeButton();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}

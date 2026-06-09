package com.kdt;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.constraintlayout.widget.ConstraintLayout;
import net.kdt.pojavlaunch.kotlin.ui.binder.LoggerViewBinder;

/**
 * A class able to display logs to the user.
 * Now using Jetpack Compose for the UI.
 */
public class LoggerView extends ConstraintLayout {

    public LoggerView(@NonNull Context context) {
        this(context, null);
    }

    public LoggerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        ComposeView composeView = new ComposeView(getContext());
        LoggerViewBinder.bind(composeView, () -> LoggerView.this.setVisibility(GONE));
        addView(composeView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }
}

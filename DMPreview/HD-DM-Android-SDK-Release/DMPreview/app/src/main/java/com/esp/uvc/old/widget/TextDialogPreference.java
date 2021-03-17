package com.esp.uvc.old.widget;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

/**
 * Created by Gray.Jao on 2018/2/26.
 */

public class TextDialogPreference extends DialogPreference {

    public TextDialogPreference(Context context) {
        this(context,null);
    }

    public TextDialogPreference(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public TextDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setNegativeButtonText("OK");
    }

    public TextDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setNegativeButtonText("OK");
    }

    /**
     * Hide the cancel button
     */
//    @Override
//    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
//        super.onPrepareDialogBuilder(builder);
//        builder.setNegativeButton(null, null);
//    }

    @Override
    protected void onClick() {
//        showDialog(null);
    }

    public void showDialog() {

        if (getDialog() != null && getDialog().isShowing()) return;

        showDialog(null);
    }
}

package com.esp.uvc.main.common;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioGroup;

import androidx.appcompat.widget.AppCompatRadioButton;

public class UncheckableRadioButton extends AppCompatRadioButton {

    public UncheckableRadioButton(Context context) {
        super(context);
    }

    public UncheckableRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UncheckableRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void toggle() {
        if (isChecked()) {
            if (getParent() != null && getParent() instanceof RadioGroup) {
                ((RadioGroup) getParent()).clearCheck();
            }
        } else {
            super.toggle();
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return UncheckableRadioButton.class.getName();
    }
}

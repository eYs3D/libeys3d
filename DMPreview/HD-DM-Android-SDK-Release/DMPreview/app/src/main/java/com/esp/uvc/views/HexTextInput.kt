package com.esp.uvc.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputEditText
import java.util.regex.Pattern

class HexTextInput : TextInputEditText, View.OnFocusChangeListener {

    private val prefix = "0x"
    private val pattern = "$prefix[a-fA-F0-9]*"

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize()
    }

    private fun initialize() {
        onFocusChangeListener = this
        addTextChangedListener {
            val regex = Pattern.compile(pattern)
            var previousText = prefix
            var ignore = false
            doAfterTextChanged {
                if (ignore) return@doAfterTextChanged
                ignore = true
                if (regex.matcher(it).matches()) {
                    previousText = it.toString()
                } else {
                    if (isFocused) {
                        setText(previousText)
                    }
                }
                ignore = false
            }
        }
    }

    override fun onFocusChange(view: View?, focused: Boolean) {
        if (focused && !text?.startsWith(prefix)!!) {
            setText("$prefix${text.toString()}")
        }

        if (!focused && text.toString().equals(prefix)) {
            setText("")
        }
    }

    fun getNumber(): Int {
        if (text!!.isEmpty() || text.toString() == prefix) return 0
        return text!!.substring(prefix.length).toInt(16)
    }

    fun setNumber(value: Int) {
        setText("$prefix${Integer.toHexString(value)}")
    }
}
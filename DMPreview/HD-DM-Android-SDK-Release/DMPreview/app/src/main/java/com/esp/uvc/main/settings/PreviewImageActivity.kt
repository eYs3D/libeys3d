package com.esp.uvc.main.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.esp.uvc.BaseFragment
import com.esp.uvc.R
import com.esp.uvc.utils.roUI
import com.google.android.material.snackbar.Snackbar
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.android.synthetic.main.activity_preview_image_settings.*
import kotlinx.android.synthetic.main.layout_ir.*
import org.koin.android.ext.android.inject

class PreviewImageActivity : AppCompatActivity(), PreviewImageContract.View {
    override val presenter: PreviewImageContract.Presenter by inject()

    override fun updateUISelection(colorStreamEnabled: Boolean, depthStreamEnabled: Boolean, cameraIndex: Int, depthCameraIndex: Int, depthDataTypeIndex: Int, colorFrameRate: Int, depthFrameRate: Int, postProcessingEnabled: Boolean, monitorFrameRate: Boolean, mZFar: Int, irOverride: Boolean, mirrorEnabled: Boolean, landscapeEnabled: Boolean, upsideDownEnabled: Boolean, roiSize: Int) = roUI {
        enable_color_stream_checkbox.isChecked = colorStreamEnabled
        enable_depth_stream_checkbox.isChecked = depthStreamEnabled
        color_stream_spinner.setSelection(cameraIndex)
        depth_stream_spinner.setSelection(depthCameraIndex)
        depth_data_type_spinner.setSelection(depthDataTypeIndex)
        color_frame_rate_text_input.setText(colorFrameRate.toString())
        depth_frame_rate_text_input.setText(depthFrameRate.toString())
        enable_show_fps_checkbox.isChecked = monitorFrameRate
        z_far_text_input.setText(mZFar.toString())
        enable_ir_override.isChecked = irOverride
        enable_180_flip_checkbox.isChecked = upsideDownEnabled
        enable_landscape_checkbox.isChecked = landscapeEnabled
        enable_mirror_checkbox.isChecked = mirrorEnabled
        roiSeekbar.setProgress(roiSize.toFloat())
        enable_post_process_checkbox.isChecked = postProcessingEnabled
    }

    override fun updateUILists(colorEntries: ArrayList<CharSequence>, depthEntries: ArrayList<CharSequence>, depthDataTypeEntries: ArrayList<CharSequence>) = roUI {
        val tmpColorEntries = colorEntries.clone() as ArrayList<CharSequence>
        color_stream_spinner.adapter = ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, tmpColorEntries).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        color_stream_spinner.invalidate()
        color_stream_spinner.isEnabled = tmpColorEntries.isNotEmpty()
        val tmpDepthEntries = depthEntries.clone() as ArrayList<CharSequence>
        depth_stream_spinner.adapter = ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, tmpDepthEntries).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        depth_stream_spinner.invalidate()
        depth_stream_spinner.isEnabled = tmpDepthEntries.isNotEmpty()
        val tmpDepthDataTypeEntries = depthDataTypeEntries.clone() as ArrayList<CharSequence>
        depth_data_type_spinner.adapter = ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, tmpDepthDataTypeEntries).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        depth_data_type_spinner.invalidate()
        depth_data_type_spinner.isEnabled = tmpDepthDataTypeEntries.isNotEmpty()
    }

    override fun updateFpsOptions(colorFpsOptions: ArrayList<String>?, depthFpsOptions: ArrayList<String>?) = roUI {
        colorFpsSpinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, colorFpsOptions).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        colorFpsSpinner.invalidate()
        colorFpsSpinner.isEnabled = !colorFpsOptions.isNullOrEmpty()
        depthFpsSpinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, depthFpsOptions).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        depthFpsSpinner.invalidate()
        depthFpsSpinner.isEnabled = !depthFpsOptions.isNullOrEmpty()
    }

    override fun updateFpsSelection(colorFpsIndex: Int, depthFpsIndex: Int) = roUI {
        colorFpsSpinner.setSelection(colorFpsIndex)
        depthFpsSpinner.setSelection(depthFpsIndex)
    }

    override fun updateDepthDataTypeList(depthDataTypeEntries: ArrayList<CharSequence>) = roUI {
        depth_data_type_spinner.adapter = ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, depthDataTypeEntries).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        depth_data_type_spinner.postInvalidate()
    }

    override fun updatePresetList(presets: ArrayList<String>, selection: Int) = roUI {
        presetSpinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, presets).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        presetSpinner.postInvalidate()
        presetSpinner.setSelection(selection)
    }

    override fun getViewValues(): ArrayList<Any?> {
        return arrayListOf(depth_data_type_spinner.selectedItemPosition, enable_color_stream_checkbox.isChecked, enable_depth_stream_checkbox.isChecked, color_stream_spinner.selectedItemPosition, depth_stream_spinner.selectedItemPosition, color_frame_rate_text_input.text, depth_frame_rate_text_input.text, enable_show_fps_checkbox.isChecked, z_far_text_input.text, enable_ir_override.isChecked, enable_landscape_checkbox.isChecked, enable_180_flip_checkbox.isChecked, enable_mirror_checkbox.isChecked, depthRangeSeekbar.progress)
    }

    override fun getRoiSizeValue(): Int {
        return roiSeekbar.progress
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                if (presenter.canNavigate()) {
                    super.onOptionsItemSelected(item)
                } else {
                    true
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun updateDepthRangeValues(values: Array<String>, selection: Int) = roUI {
        depthRangeContainer.visibility = if (values.isNotEmpty()) View.VISIBLE else View.GONE
        depthRangeSeekbar.customTickTexts(values)
        if (values.isNotEmpty()) {
            depthRangeSeekbar.max = (values.size - 1).toFloat()
        }
        depthRangeSeekbar.setProgress(selection.toFloat())
    }

    override fun switchPresetMode(preset: Boolean) = roUI {
        if (preset) {
            presetRadioButton.isChecked = true
        } else {
            manualRadioButton.isChecked = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        BaseFragment.applyScreenRotation(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_image_settings)
        title = getString(R.string.preview_image_title)
        if (!enable_post_process_checkbox.isEnabled) {
            enable_post_process_layout.children.forEach {
                (it as TextView).setTextColor(resources.getColor(R.color.text_gray_out))
            }
        }
        presenter.attach(this)

        initUI()
    }

    override fun onBackPressed() {
        if (presenter.canNavigate()) {
            super.onBackPressed()
        }
    }

    fun initUI() {
        color_stream_spinner.isEnabled = !color_stream_spinner.adapter.isEmpty
        depth_stream_spinner.isEnabled = !depth_stream_spinner.adapter.isEmpty
        depth_data_type_spinner.isEnabled = !depth_data_type_spinner.adapter.isEmpty
        depth_data_type_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                presenter.onDepthDataTypeSelected(position)
            }
        }

        presetRadioButton.setOnClickListener { presenter.usePreset(true) }
        manualRadioButton.setOnClickListener { presenter.usePreset(false) }

        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                presenter.onPresetSelected(position)
            }
        }

        colorFpsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                presenter.onColorFpsSelected(position)
            }
        }

        depthFpsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                presenter.onDepthFpsSelected(position)
            }
        }

        roiSeekbar.setIndicatorTextFormat("\${TICK_TEXT}")
        depthRangeSeekbar.setIndicatorTextFormat("\${TICK_TEXT}")

        presenter.usePreset(presetRadioButton.isChecked)

        ir_value_seekbar.setIndicatorTextFormat("\${TICK_TEXT}")
        ir_value_seekbar.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams?) = Unit

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {
                presenter.onIrValueChanged(seekBar!!.progress)
            }
        }
        ir_range_extended_checkbox.setOnClickListener { presenter.onExtendedSelectionChanged((it as CheckBox).isChecked) }
        enable_post_process_checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            presenter.onPostProcessChanged(isChecked)
        }
    }

    override fun toast(text: String, length: Int) = roUI {
        Toast.makeText(applicationContext, text, length).show()
    }

    override fun updateUsingPresetSelection(isUsingPreset: Boolean) = roUI {
        if (isUsingPreset) presetRadioButton.isChecked = true
        else manualRadioButton.isChecked = true
    }

    override fun updateFpsValues(colorFps: Int, depthFps: Int) = roUI {
        color_frame_rate_text_input.setText(colorFps.toString())
        depth_frame_rate_text_input.setText(depthFps.toString())
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun onPause() {
        super.onPause()
        presenter.onPause()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun enablePresetSelection(enabled: Boolean) = roUI {
        presetSpinner.isEnabled = enabled
    }

    override fun enableManualColorFpsInput(enabled: Boolean) = roUI {
        color_frame_rate_text_input.isEnabled = enabled
        color_frame_rate_layout.visibility = if (enabled) View.VISIBLE else View.GONE
        colorFpsSpinnerContainer.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    override fun enableManualDepthFpsInput(enabled: Boolean) = roUI {
        depth_frame_rate_text_input.isEnabled = enabled
        depth_frame_rate_layout.visibility = if (enabled) View.VISIBLE else View.GONE
        depthFpsSpinnerContainer.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    override fun enableColorResolutionSelection(enabled: Boolean) = roUI {
        color_stream_spinner.isEnabled = if (!enabled) false else !color_stream_spinner.adapter.isEmpty
    }

    override fun enableDepthResolutionSelection(enabled: Boolean) = roUI {
        depth_stream_spinner.isEnabled = if (!enabled) false else !depth_stream_spinner.adapter.isEmpty
    }

    override fun enableDepthFrameRateInputSelection(enabled: Boolean) = roUI {
        depth_frame_rate_text_input.isEnabled = enabled
    }

    override fun enableColorStreamCheckbox(enabled: Boolean) = roUI {
        enable_color_stream_checkbox.isEnabled = enabled
    }

    override fun enableDepthStreamCheckbox(enabled: Boolean) = roUI {
        enable_depth_stream_checkbox.isEnabled = enabled
    }

    override fun updateRoiValues(values: Array<String>) = roUI {
        roiSeekbar.max = values.size.toFloat() - 1
        roiSeekbar.tickCount = values.size
        roiSeekbar.customTickTexts(values)
    }

    override fun enableIRSection(enabled: Boolean) {
        ir_range_extended_checkbox.isEnabled = enabled
        ir_value_seekbar.isEnabled = enabled
    }

    override fun setDepthStreamSelection(on: Boolean) = roUI {
        enable_depth_stream_checkbox?.isChecked = on
    }

    override fun showSnack(text: String, length: Int, actionText: String, onClick: (() -> Unit)?) = roUI {
        val view = findViewById<View>(android.R.id.content).rootView ?: return@roUI
        if (onClick != null) {
            Snackbar.make(view, text, length).setAction(actionText) {
                onClick()
            }.show()
        } else {
            Snackbar.make(view, text, length).show()
        }
    }

    override fun enablePresetMode(enabled: Boolean) = roUI {
        presetRadioButton.isEnabled = enabled
        manualRadioButton.isEnabled = enabled
    }

    override fun showExtendedCheckbox(visible: Boolean) = roUI {
        ir_range_extended_checkbox.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun updateIrValue(value: Int) = roUI {
        ir_value_seekbar.setProgress(value.toFloat())
    }

    override fun updateExtendedSelection(selected: Boolean) = roUI {
        ir_range_extended_checkbox.isChecked = selected
    }

    override fun updateIrRange(min: Int, max: Int) = roUI {
        ir_value_seekbar.min = min.toFloat()
        ir_value_seekbar.max = max.toFloat()
        ir_value_seekbar.tickCount = max - min + 1
    }

    companion object {
        fun startInstance(context: Context) {
            context.startActivity(Intent(context, PreviewImageActivity::class.java))
        }
    }
}

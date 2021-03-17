package com.esp.uvc.main.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.esp.uvc.R
import kotlinx.android.synthetic.main.dialog_region_accuracy.view.*

class RegionAccuracyDialogFragment : DialogFragment() {

    private lateinit var mView: View

    private lateinit var mAdapter: ArrayAdapter<String>

    private lateinit var mROIList: List<String>
    private var mROIIndex = 0
    private var mEnableGroundTruth = false
    private var mGroundTruth = 0f

    private var mListener: OnListener? = null

    interface OnListener {

        fun onDismiss()

        fun onROI(index: Int)

        fun onGroundTruth(enabled: Boolean)

        fun onGroundTruthValue(value: Float)

        fun onGroundTruthValue(add: Boolean)

    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mView = activity!!.layoutInflater.inflate(R.layout.dialog_region_accuracy, null)
        val builder = AlertDialog.Builder(activity!!)
        builder.setView(mView)
        setupUI()
        return builder.create()
    }

    // Full screen and bg no dim
    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val window = dialog.window
            if (window != null) {
                window.attributes?.gravity = Gravity.TOP
                window.attributes?.dimAmount = 0f
                window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mListener?.onDismiss()
    }

    fun setConfiguration(
        listener: OnListener,
        roiList: List<String>,
        roiIndex: Int,
        enableGroundTruth: Boolean,
        groundTruth: Float
    ) {
        mListener = listener
        mROIList = roiList
        mROIIndex = roiIndex
        mEnableGroundTruth = enableGroundTruth
        mGroundTruth = groundTruth
    }

    fun onAccuracyInfo(
        distance: Float,
        fillRate: Float,
        zAccuracy: Float
    ) {
        if (context == null) return
        mView.tv_distance_value.text =
            resources.getString(R.string.dialog_region_accuracy_mm_value, distance)
        mView.tv_fill_rate_value.text =
            resources.getString(R.string.dialog_region_accuracy_percent_value, fillRate)
        mView.tv_z_accuracy_value.text =
            resources.getString(R.string.dialog_region_accuracy_percent_value, zAccuracy)
    }

    fun onTemporalNoise(temporalNoise: Float) {
        if (context == null) return
        mView.tv_temporal_noise_value.text =
            resources.getString(R.string.dialog_region_accuracy_percent_value, temporalNoise)
    }

    fun onSpatialNoise(spatialNoise: Float, angle: Float, angleX: Float, angleY: Float) {
        if (context == null) return
        mView.tv_spatial_noise_value.text =
            resources.getString(R.string.dialog_region_accuracy_percent_value, spatialNoise)
        mView.tv_angle_value.text =
            resources.getString(R.string.dialog_region_accuracy_angle_value, angle)
        mView.tv_angle_x_value.text =
            resources.getString(R.string.dialog_region_accuracy_angle_x_value, angleX)
        mView.tv_angle_y_value.text =
            resources.getString(R.string.dialog_region_accuracy_angle_y_value, angleY)
    }

    fun onGroundTruth(groundTruth: Float) {
        if (context == null) return
        mView.et_ground_truth_value.setText(
            resources.getString(
                R.string.dialog_region_accuracy_float_value,
                groundTruth
            )
        )
    }

    private fun setupUI() {
        mAdapter = ArrayAdapter(
            activity!!,
            android.R.layout.simple_spinner_item,
            ArrayList<String>()
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        mAdapter.addAll(mROIList)
        mView.spinner_region_of_interest.adapter = mAdapter
        mView.spinner_region_of_interest.setSelection(mROIIndex)
        mView.spinner_region_of_interest.onItemSelectedListener = mOnItemSelectedListener
        mView.cb_ground_truth.isChecked = mEnableGroundTruth
        mView.cb_ground_truth.setOnCheckedChangeListener(mOnCheckedChangedListener)
        mView.et_ground_truth_value.isEnabled = mEnableGroundTruth
        mView.et_ground_truth_value.setText(
            resources.getString(
                R.string.dialog_region_accuracy_float_value,
                mGroundTruth
            )
        )
        mView.et_ground_truth_value.addTextChangedListener(mOnTextChangeListener)
        mView.ib_arrow_up.isEnabled = mEnableGroundTruth
        mView.ib_arrow_down.isEnabled = mEnableGroundTruth
        mView.ib_arrow_up.setOnClickListener(mOnClickListener)
        mView.ib_arrow_down.setOnClickListener(mOnClickListener)
    }

    private val mOnItemSelectedListener = object : AdapterView.OnItemSelectedListener {

        override fun onNothingSelected(p0: AdapterView<*>?) {
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            mListener?.onROI(position)
        }
    }

    private val mOnTextChangeListener = object : TextWatcher {

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            val value = s.toString().toFloatOrNull()
            if (value != null)
                mListener?.onGroundTruthValue(value)
        }

    }

    private val mOnCheckedChangedListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            mView.et_ground_truth_value.isEnabled = isChecked
            mView.ib_arrow_up.isEnabled = isChecked
            mView.ib_arrow_down.isEnabled = isChecked
            mListener?.onGroundTruth(
                isChecked
            )
        }

    private val mOnClickListener = View.OnClickListener { v ->
        mListener?.onGroundTruthValue(v!!.id == R.id.ib_arrow_up)
    }
}
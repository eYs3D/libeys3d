package com.esp.uvc.usbcamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.Spinner;

import com.esp.android.usb.camera.core.DeviceFilter;
import com.esp.android.usb.camera.core.USBMonitor;
import com.esp.uvc.R;

import java.util.ArrayList;
import java.util.List;

public class CameraDialog extends DialogFragment {
    private static final String TAG = CameraDialog.class.getSimpleName();

    public static CameraDialog showDialog(final Activity parent, USBMonitor usbMonitor) {
        CameraDialog dialog = newInstance();
        mUSBMonitor = usbMonitor;
        try {
            dialog.show(parent.getFragmentManager(), TAG);
        } catch (final IllegalStateException e) {
            dialog = null;
        }
        return dialog;
    }

    public static CameraDialog newInstance() {
        final CameraDialog dialog = new CameraDialog();
        final Bundle args = new Bundle();
        dialog.setArguments(args);
        return dialog;
    }

    private static USBMonitor mUSBMonitor = null;
    private Spinner mSpinner;
    private DeviceListAdapter mDeviceListAdapter;

    public CameraDialog() {
        // Fragment need default constructor
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
       /*if (mUSBMonitor == null)
        try {
            mUSBMonitor = (activity).getUSBMonitor();
        } catch (final ClassCastException e) {
        } catch (final NullPointerException e) {
        }*/
        if (mUSBMonitor == null) {
            throw new ClassCastException(activity.toString() + " must implement #getUSBController");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null)
            savedInstanceState = getArguments();
    }

    @Override
    public void onSaveInstanceState(final Bundle saveInstanceState) {
        final Bundle args = getArguments();
        if (args != null)
            saveInstanceState.putAll(args);
        super.onSaveInstanceState(saveInstanceState);
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(initView());
        builder.setTitle(R.string.select);
        builder.setPositiveButton(android.R.string.ok, mOnDialogClickListener);
        builder.setNegativeButton(android.R.string.cancel , mOnDialogClickListener);
        builder.setNeutralButton(R.string.refresh, null);
        final Dialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    private final View initView() {
        final View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_camera, null);
        mSpinner = (Spinner)rootView.findViewById(R.id.spinner1);
        final View empty = rootView.findViewById(android.R.id.empty);
        mSpinner.setEmptyView(empty);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDevices();
        final Button button = (Button)getDialog().findViewById(android.R.id.button3);
        if (button != null) {
            button.setOnClickListener(mOnClickListener);
        }
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            switch (v.getId()) {
            case android.R.id.button3:
                updateDevices();
                break;
            }
        }
    };

    private final DialogInterface.OnClickListener mOnDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                final Object item = mSpinner.getSelectedItem();
                int Position = mSpinner.getSelectedItemPosition();
                Log.d(TAG, "getSelectedItemPosition:" + Position );
                if (item instanceof UsbDevice) {
                    mUSBMonitor.requestPermission((UsbDevice)item);
                }
                break;
            }
        }
    };

    public void updateDevices() {
        final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(getActivity(), R.xml.device_filter);
        mDeviceListAdapter = new DeviceListAdapter(getActivity(), mUSBMonitor.getDeviceList(filter.get(0)));
        mSpinner.setAdapter(mDeviceListAdapter);
    }

    private static final class DeviceListAdapter extends BaseAdapter {

        private final LayoutInflater mInflater;
        private final List<UsbDevice> mList;

        public DeviceListAdapter(final Context context, final List<UsbDevice>list) {
            mInflater = LayoutInflater.from(context);
            mList = list != null ? list : new ArrayList<UsbDevice>();
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public UsbDevice getItem(final int position) {
            if ((position >= 0) && (position < mList.size()))
                return mList.get(position);
            else
                return null;
        }

        @Override
        public long getItemId(final int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.listitem_device, parent, false);
            }
            if (convertView instanceof CheckedTextView) {
                final UsbDevice device = getItem(position);
                ((CheckedTextView)convertView).setText(
                    String.format("UVC Camera:(%x:%x)", device.getVendorId(), device.getProductId()));
            }
            return convertView;
        }
    }
}

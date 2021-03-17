package com.esp.uvc.usbcamera;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.esp.uvc.R;

public class MainAdapter extends BaseAdapter {
    private LayoutInflater myInflater;
    private Context mContext;
    private int mCurrent = 0;
    private boolean mOnlyColor = true;
    private int Color_W = 0;
    private int Color_H = 0;
    private int Depth_W = 0;
    private int Depth_H = 0;
    private String[] mListTitle = {
        "Show Color and Depth(Demo)",
        "Show Color and Depth",
        "Show Color and Depth",
        "Show Color and Depth",
        "Show Color and Depth",
        "Show Only Color(Demo)",
        "Show Only Color",
        "Show Only Color",
        "Show Only Color"
    };

    private String[] mListSummary = {
            "Color640*480Mfps30_Depth320*480Yfps30",
            "Color640*400Mfps30_Depth320*400Yfps30",
            "Color320*240Mfps90_Depth160*240Yfps90",
            "Color640*240Mfps90_Depth160*240Yfps90",
            "Color800*600Mfps30_Depth320*480Yfps30",
            "Color640*480Mfps30",
            "Color2560*720Mfps30",
            "Color1280*480Mfps30",
            "Color1280*720Mfps30"
    };

    public MainAdapter(Context context) {
        myInflater = LayoutInflater.from(context);
        mContext = context;
    }

    @Override
    public int getCount() {
        return mListTitle.length;
    }

    @Override
    public Object getItem(int position) {
        return mListTitle[position];
    }

    @Override
    public long getItemId(int position) {
       return position;
    }

    @Override
    public View getView(int position,View convertView,ViewGroup parent) {
        ViewTag viewTag;
        if (convertView == null) {
            convertView = myInflater.inflate(R.layout.adapter, null);
            viewTag = new ViewTag(
                (TextView) convertView.findViewById(R.id.title),
                (TextView) convertView.findViewById(R.id.summary),
                (CheckBox) convertView.findViewById(R.id.check));
            convertView.setTag(viewTag);
        } else
            viewTag = (ViewTag) convertView.getTag();

        String title = mListTitle[position];
        String summary = mListSummary[position];
        viewTag.title.setText(title);
        viewTag.summary.setText(summary);
        viewTag.cbx.setChecked(isChecked(position));
        viewTag.cbx.setClickable(false);
        viewTag.cbx.setFocusable(false);
        return convertView;
    }

    public class ViewTag {
        TextView title;
        TextView summary;
        CheckBox cbx;

        public ViewTag(TextView title, TextView summary,  CheckBox cbx) {
            this.title = title;
            this.summary = summary;
            this.cbx = cbx;
        }
    }

    public boolean isChecked(int position) {
        boolean check = false;
        if (position == mCurrent) check = true;

        return check;
    }

    public void readPositionState() {

        AppSettings appSettings = AppSettings.getInstance(mContext);
        mCurrent = appSettings.get(AppSettings.CURRENT,0);
    }

    public void writePositionState(int position) {

        AppSettings appSettings = AppSettings.getInstance(mContext);
        if (position == 0) {
            mOnlyColor = false;
            Color_W = 640;
            Color_H = 480;
            Depth_W = 320;
            Depth_H = 480;
        } else if (position == 1) {
            mOnlyColor = false;
            Color_W = 640;
            Color_H = 400;
            Depth_W = 320;
            Depth_H = 400;
        } else if (position == 2) {
            mOnlyColor = false;
            Color_W = 320;
            Color_H = 240;
            Depth_W = 160;
            Depth_H = 240;
        } else if (position == 3) {
            mOnlyColor = false;
            Color_W = 640;
            Color_H = 240;
            Depth_W = 160;
            Depth_H = 240;
        } else if (position == 4) {
            mOnlyColor = false;
            Color_W = 800;
            Color_H = 600;
            Depth_W = 320;
            Depth_H = 480;
        } else if (position == 5) {
            mOnlyColor = true;
            Color_W = 640;
            Color_H = 480;
            Depth_W = 0;
            Depth_H = 0;
        } else if (position == 6) {
            mOnlyColor = true;
            Color_W = 2560;
            Color_H = 720;
            Depth_W = 0;
            Depth_H = 0;
        } else if (position == 7) {
            mOnlyColor = true;
            Color_W = 1280;
            Color_H = 480;
            Depth_W = 0;
            Depth_H = 0;
        } else if (position == 8) {
            mOnlyColor = true;
            Color_W = 1280;
            Color_H = 720;
            Depth_W = 0;
            Depth_H = 0;
        }

        appSettings.put(AppSettings.CURRENT, position);
        appSettings.put(AppSettings.ONLY_COLOR, mOnlyColor);
        appSettings.put(AppSettings.COLOR_W, Color_W);
        appSettings.put(AppSettings.COLOR_H, Color_H);
        appSettings.put(AppSettings.DEPTH_W, Depth_W);
        appSettings.put(AppSettings.Depth_H, Depth_H);
        appSettings.saveAll();
    }

    public void selectHandle(int position) {
        mCurrent = position;
        writePositionState(mCurrent);
        MainAdapter.this.notifyDataSetChanged();
    }

    public void onResume() {
        readPositionState();
        MainAdapter.this.notifyDataSetChanged();
    }

}

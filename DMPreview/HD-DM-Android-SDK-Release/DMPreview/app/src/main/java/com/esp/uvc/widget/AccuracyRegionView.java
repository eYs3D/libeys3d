package com.esp.uvc.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class AccuracyRegionView extends View {

    private Paint mPaint;
    private Rect mRect;

    public AccuracyRegionView(Context context) {
        super(context);
    }

    public AccuracyRegionView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AccuracyRegionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
    }

    public void setRect(int left, int top, int right, int bottom) {
        mRect = new Rect(left, top, right, bottom);
        invalidate();
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setStrokeWidth(6);
        mPaint.setColor(Color.YELLOW);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mRect == null) return;
        canvas.drawRect(mRect, mPaint);
    }
}

package com.sdpdigital.glassblockbar.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

import androidx.appcompat.widget.AppCompatSeekBar;

public class VerticalSeekBar extends AppCompatSeekBar {

    private VerticalProgressChangedListener verticalListener = null;
    public void setVerticalListener(VerticalProgressChangedListener listener) {
        verticalListener = listener;
    }

    public VerticalSeekBar(Context context) {
        super(context);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    public synchronized void setProgress(int progress)  // it is necessary for calling setProgress on click of a button
    {
        super.setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);

    }
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);

        super.onDraw(c);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        int newProgress = getMax() - (int) (getMax() * event.getY() / getHeight());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                setProgress(newProgress);
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                if (verticalListener != null) {
                    verticalListener.onTouchUp(this, newProgress);
                }
                break;
            case MotionEvent.ACTION_UP: {
                setProgress(newProgress);
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                if (verticalListener != null) {
                    verticalListener.onTouchUp(this, newProgress);
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }

    public interface VerticalProgressChangedListener {
        void progressChanged(VerticalSeekBar seekbar, int progress);
        void onTouchUp(VerticalSeekBar seekbar, int progress);
    }
}
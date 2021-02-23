package com.sdpdigital.glassblockbar.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;

import java.util.List;

/**
  * <br>Copyright Michael DePhillips
 *
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation (see COPYING).
 *
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details. on 2/25/14. on 2/27/14.
 */
public class ColorPaletteView extends androidx.appcompat.widget.AppCompatImageView {

    private static final String LOGGING_TAG = ColorPaletteView.class.getSimpleName();

    private ColorPalette mColorPalette;

    private int mFullWidth;
    private int mSpacing;

    public void setColorPalette(ColorPalette colorPalette) {
        mColorPalette = colorPalette;
        refreshWidth();
    }

    public ColorPaletteView(Context context) {
        super(context);
        init(context);
    }

    public ColorPaletteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mSpacing =(int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1 , context.getResources()
                .getDisplayMetrics());
    }

    @Override
    protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed,left,top,right,bottom);
        // avoid calling this every time, since refreshWidth() will call onLayout() again
        if( mFullWidth != this.getWidth() ) {
            mFullWidth = this.getWidth();
            refreshWidth();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(LOGGING_TAG, "Detached from Window");
    }

    private void refreshWidth() {
        if( mFullWidth != 0 ) {
            int w = mFullWidth, h = getHeight();
            Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
            Bitmap bitmap = Bitmap.createBitmap(w, h, conf); // this creates a MUTABLE bitmap
            Canvas canvas = new Canvas(bitmap);
            final RectF rect = new RectF(0, 0, w, h);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);

            List<Integer> colors = mColorPalette.getColors();
            int colorWidth = mFullWidth / colors.size();
            for(int i = 0; i < colors.size(); i++) {
                rect.left = i * colorWidth + mSpacing;
                rect.right = (i+1) * colorWidth - mSpacing;
                paint.setColor(colors.get(i));
                canvas.drawRect(rect, paint);
            }
            setImageBitmap(bitmap);
        }
    }
}

package com.sdpdigital.glassblockbar.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.sdpdigital.glassblockbar.R;

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
public class ColorPaletteSpinnerAdapter extends ArrayAdapter<ColorPalette> {

    private LayoutInflater mInflater;

    public ColorPaletteSpinnerAdapter(Context context, int resource, List<ColorPalette> objects) {
        super(context, resource, objects);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getColorView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getColorView(position, convertView, parent);
    }

    public View getColorView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.spinner_color_palette_row, parent, false);
        }
        ColorPaletteView colorPaletteView = (ColorPaletteView)convertView.findViewById(R.id.color_palette_view);
        colorPaletteView.setColorPalette(getItem(position));
        return convertView;
    }
}

package com.sdpdigital.glassblockbar.view;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

/**
 Colors * <br>Copyright Michael DePhillips
 *
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation (see COPYING).
 *
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details. on 2/25/14. on 2/24/14.
 */
public class ColorPalette {

    private int mColorIndex;
    private List<Integer> mColors;
    public List<Integer> getColors() { return mColors; }
    public void setColors(List<Integer> colors) { mColors = colors; }

    private int mBaseColor;
    public int getBaseColor() { return mBaseColor; }
    public void setBaseColor(int baseColor) { mBaseColor = baseColor; }

    public ColorPalette(List<Integer> colors, int baseColor) {
        mColors = colors;
        mBaseColor = baseColor;
        mColorIndex = 0;
    }

    public int getCurrentColor() {
        return mColors.get(mColorIndex);
    }

    public int nextColor() {
        mColorIndex++;
        mColorIndex %= mColors.size();
        return mColors.get(mColorIndex);
    }

    public int randomColor() {
        int randomIndex = (int)(Math.random() * mColors.size());
        return mColors.get(randomIndex);
    }

    public void reset() {
        mColorIndex = 0;
    }

    public static int RED = 0xFFFF0000;
    public static int ORANGE = 0xFFFF7F00;
    public static int YELLOW = 0xFFFFFF00;
    public static int GREEN = 0xFF00FF00;
    public static int BLUE = 0xFF0000FF;
    public static int INDIGO = 0xFF4B0082;
    public static int VIOLET = 0xFF8F00FF;
    public static int PURPLE = 0xFFFF00FF;
    public static int BLACK = 0xFF000000;
    public static int WHITE = 0xFFFFFFFF;

    public static int LIGHT_GREY = 0xFFB3B1B2;
    public static int DARK_GREY = 0xFF47423F;
    public static int DARK_TAN = 0xFFADA692;
    public static int DARK_GREEN = 0xFF527578;
    public static int DARK_LIGHTER_GREEN = 0xFF84978F;

	public static int LIGHT_TAN = 0xFFCCCC99;
	public static int SOFT_PURPLE = 0xFF666699;
	public static int DARK_BLUE = 0xFF003366;

    public static int PINK_TAN = 0xFFDDC0B2;
    public static int RED_PINK = 0xFFC94591;
    public static int INDIGO_PINK = 0xFF601B4A;
    public static int BRIGHT_PINK = 0xFFDF75DA;
    public static int REDDER_PINK = 0xFFC03552;

    public static int YELLOW_GREEN = 0xFFBED661;
    public static int LIGHT_GREEN = 0xFF89E894;
    public static int COOL_BLUE = 0xFF78D5E3;
    public static int CYAN = 0xFF34DDDD;
    public static int GREEN_BLUE = 0xFF93E2D5;


    public static ColorPalette createRgbColorPalette() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(RED);
        colors.add(GREEN);
        colors.add(BLUE);
        int baseColor = Color.BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static ColorPalette createOgyColorPalette() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(ORANGE);
        colors.add(GREEN);
        colors.add(YELLOW);
        int baseColor = Color.BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static ColorPalette createIndiansPalette() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(WHITE);
        colors.add(RED);
        colors.add(GREEN);
        colors.add(YELLOW);
        int baseColor = Color.BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static List<ColorPalette> createComplimentaryColors() {
        List<ColorPalette> palettes = new ArrayList<ColorPalette>();
        palettes.add(createBlueOrangePalette());
        palettes.add(createPurpleYellow());
        palettes.add(createGreenRedPalette());
        return palettes;
    }

    public static ColorPalette createBlueOrangePalette() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(BLUE);
        colors.add(ORANGE);
        int baseColor = Color.BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static ColorPalette createGreenRedPalette() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(GREEN);
        colors.add(RED);
        int baseColor = Color.BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static ColorPalette createPurpleYellow() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(PURPLE);
        colors.add(YELLOW);
        int baseColor = BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static List<ColorPalette> createComplimentaryColorsWithWhite() {
        List<ColorPalette> palettes = new ArrayList<ColorPalette>();
        palettes.add(createBlueOrangeWithWhitePalette());
        palettes.add(createPurpleYellowWithWhitePalette());
        palettes.add(createGreenRedWithWhitePalette());
        return palettes;
    }

    public static ColorPalette createBlueOrangeWithWhitePalette() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(BLUE);
        colors.add(WHITE);
        colors.add(ORANGE);
        int baseColor = Color.BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static ColorPalette createGreenRedWithWhitePalette() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(GREEN);
        colors.add(WHITE);
        colors.add(RED);
        int baseColor = Color.BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static ColorPalette createPurpleYellowWithWhitePalette() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(PURPLE);
        colors.add(WHITE);
        colors.add(YELLOW);
        int baseColor = BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static List<ColorPalette> createComplimentaryColorsCombos() {
        List<ColorPalette> palettes = new ArrayList<ColorPalette>();
        palettes.add(createBlueOrangePurpleYellowPalette());
        palettes.add(createBlueOrangeGreenRedPalette());
        palettes.add(createPurpleYellowGreenRedPalette());
        return palettes;
    }

    public static ColorPalette createBlueOrangePurpleYellowPalette() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(BLUE);
        colors.add(ORANGE);
        colors.add(PURPLE);
        colors.add(YELLOW);
        int baseColor = Color.BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static ColorPalette createBlueOrangeGreenRedPalette() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(BLUE);
        colors.add(ORANGE);
        colors.add(GREEN);
        colors.add(RED);
        int baseColor = Color.BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static ColorPalette createPurpleYellowGreenRedPalette() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(PURPLE);
        colors.add(YELLOW);
        colors.add(GREEN);
        colors.add(RED);
        int baseColor = Color.BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static ColorPalette createRainbowColorPalette() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(RED);
        colors.add(ORANGE);
        colors.add(YELLOW);
        colors.add(GREEN);
        colors.add(BLUE);
        colors.add(PURPLE);
        int baseColor = BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static ColorPalette createPinkTones() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(PINK_TAN);
        colors.add(RED_PINK);
        colors.add(INDIGO_PINK);
        colors.add(BRIGHT_PINK);
        colors.add(REDDER_PINK);
        int baseColor = BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static ColorPalette createCoolWinterTones() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(YELLOW_GREEN);
        colors.add(LIGHT_GREEN);
        colors.add(COOL_BLUE);
        colors.add(BLUE);
        colors.add(WHITE);
        colors.add(CYAN);
        colors.add(GREEN_BLUE);
        int baseColor = BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static ColorPalette createDiscoTones() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(WHITE);
        colors.add(YELLOW);
        colors.add(GREEN);
        colors.add(COOL_BLUE);
        colors.add(RED_PINK);
        colors.add(VIOLET);
        colors.add(BLUE);
        int baseColor = BLACK;
        return new ColorPalette(colors, baseColor);
    }

    public static ColorPalette createSplashColorPalette() {
        List<Integer> colors = new ArrayList<Integer>();
        colors.add(RED);
        colors.add(ORANGE);
        colors.add(YELLOW);
        colors.add(GREEN);
        colors.add(BLUE);
        colors.add(INDIGO);
        colors.add(VIOLET);
        int baseColor = WHITE;
        return new ColorPalette(colors, baseColor);
    }
	public static ColorPalette createArmyTonesPalette() {
		List<Integer> colors = new ArrayList<Integer>();
		colors.add(LIGHT_GREY);
		colors.add(DARK_LIGHTER_GREEN);
		colors.add(DARK_TAN);
		colors.add(DARK_GREEN);
		colors.add(DARK_GREY);
		return new ColorPalette(colors, BLACK);
	}

	public static List<ColorPalette> createOnlinePalettes() {
		List<ColorPalette> retVal = new ArrayList<ColorPalette>();

		List<Integer> colors = new ArrayList<Integer>();
		colors.add(DARK_GREEN);
		colors.add(LIGHT_TAN);
		colors.add(SOFT_PURPLE);
		colors.add(DARK_BLUE);
		retVal.add(new ColorPalette(colors, BLACK));

		List<Integer> colors2 = new ArrayList<Integer>();
		colors2.add(BRIGHT_PINK);
		colors2.add(WHITE);
		colors2.add(PURPLE);
		retVal.add(new ColorPalette(colors2, BLACK));

		List<Integer> colors3 = new ArrayList<Integer>();
		colors3.add(0xFF727B84);
		colors3.add(0xFFDF9496);
		colors3.add(0xFFF6F4DA);
		colors3.add(0xFFF4F3EE);
		colors3.add(0xFFD9E2E1);
		colors3.add(0xFFA2ADBC);
		retVal.add(new ColorPalette(colors3, BLACK));

		List<Integer> colors4 = new ArrayList<Integer>();
		colors4.add(0xFF421C52);
		colors4.add(0xFF732C7B);
		colors4.add(0xFF9C8AA5);
		colors4.add(0xFFBDAEC6);
		colors4.add(0xFFFFFFFF);
		retVal.add(new ColorPalette(colors4, BLACK));

		List<Integer> colors5 = new ArrayList<Integer>();
		colors5.add(0xFF092E20);
		colors5.add(0xFF234F32);
		colors5.add(0xFF326342);
		colors5.add(0xFF92CC47);
		colors5.add(0xFF9AEF3F);
		colors5.add(0xFFFFE761);
		retVal.add(new ColorPalette(colors5, BLACK));

		return retVal;
	}
}

package com.benco.mapping;

public class SectionStyle {
    private final float width;
    private final int color;

    public SectionStyle(float width, int color) {
        this.width = width;
        this.color = color;
    }

    public float getWidth() {
        return width;
    }

    public int getColor() {
        return color;
    }
}

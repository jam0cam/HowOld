package com.jiacorp.howold;

/**
 * Created by jitse on 5/14/15.
 */
public class Box {

    public float left;
    public float top;
    public float right;
    public float bottom;

    public Box(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public float getWidth() {
        return right - left;
    }
}

package com.jiacorp.howold;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.ViewAnimationUtils;

/**
 * Created by jitse on 5/14/15.
 */
public class AnimatorUtils {


    private static final String TAG = AnimatorUtils.class.getName();

    public static void revealAnimationIn(View myView) {

        if (!UIUtils.atLeastLollipop() || !myView.isAttachedToWindow()) {
            return;
        }

        int cx = myView.getWidth()/2;
        int cy = myView.getHeight()/2;

        // get the final radius for the clipping circle
        int finalRadius = myView.getWidth()/2;

        // create the animator for this view (the start radius is zero)
        Animator anim = ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, finalRadius);

        // make the view visible and start the animation
        myView.setVisibility(View.VISIBLE);
        anim.start();
    }

    public static void revealAnimationOut(View myView) {
        if (!UIUtils.atLeastLollipop() || !myView.isAttachedToWindow()) {
            return;
        }

        int cx = myView.getWidth()/2;
        int cy = myView.getHeight()/2;

        // get the initial radius for the clipping circle
        int initialRadius = myView.getWidth()/2;

        android.util.Log.d(TAG, "CenterX: " + cx + "   CenterY: " + cy);

        // create the animation (the final radius is zero)
        Animator anim = ViewAnimationUtils.createCircularReveal(myView, cx, cy, initialRadius, 0);

        // make the view invisible when the animation is done
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                myView.setVisibility(View.INVISIBLE);
            }
        });

        anim.start();
    }
}

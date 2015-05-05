package com.jiacorp.howold;

import android.os.Build;

/**
 * Created by jitse on 5/4/15.
 */
public class Util {

    public static boolean atLeastLollipop(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}

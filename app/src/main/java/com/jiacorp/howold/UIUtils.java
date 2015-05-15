package com.jiacorp.howold;

import android.os.Build;

/**
 * Created by jitse on 5/14/15.
 */
public class UIUtils {
    public static boolean atLeastLollipop(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}

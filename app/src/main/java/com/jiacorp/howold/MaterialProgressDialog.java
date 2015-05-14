package com.jiacorp.howold;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Created by jitse on 5/13/15.
 */
public class MaterialProgressDialog extends ProgressDialog {
    public MaterialProgressDialog(Context context) {
        super(context);

        getWindow().getAttributes().windowAnimations = R.style.DimensionDialogAnimation;

    }


}

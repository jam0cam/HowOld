package com.jiacorp.howold;

import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class FaceActivity extends AppCompatActivity {

    @InjectView(R.id.image_view)
    ImageView mImageView;

    String mPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face);

        ButterKnife.inject(this);

        mPath = getIntent().getExtras().getString("path");

        ActivityCompat.postponeEnterTransition(this);

        Glide.with(this)
                .load(mPath)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        ActivityCompat.startPostponedEnterTransition(FaceActivity.this);
                        return false;
                    }
                })
                .into(mImageView);
    }

}

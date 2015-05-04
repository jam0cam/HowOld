package com.jiacorp.howold;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Created by jitse on 5/3/15.
 */
public class GridAdapter extends ArrayAdapter<String> {

    public GridAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            rowView = ((Activity)getContext()).getLayoutInflater().inflate(R.layout.grid_item, parent, false);
        }

        Glide.with(getContext())
                .load(getItem(position))
                .into((ImageView) rowView);

        return rowView;

    }
}

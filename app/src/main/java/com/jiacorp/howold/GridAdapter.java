package com.jiacorp.howold;

import android.app.Activity;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jitse on 5/3/15.
 */
public class GridAdapter extends ArrayAdapter<String> {

    private List<String> items;
    private SparseBooleanArray selectedItems;

    public GridAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);

        items = objects;
        selectedItems = new SparseBooleanArray();
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
                .into((ImageView) rowView.findViewById(R.id.image_view));


        if (selectedItems.get(position, false)) {
            rowView.findViewById(R.id.selection_tint).setVisibility(View.VISIBLE);
        } else {
            rowView.findViewById(R.id.selection_tint).setVisibility(View.GONE);
        }

        return rowView;

    }

    public ArrayList<Integer> getSelectedItems() {
        ArrayList<Integer> items = new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    public void setSelectedItems(List<Integer> items) {
        selectedItems.clear();
        for (Integer i : items) {
            selectedItems.put(i, true);
        }

        notifyDataSetChanged();
    }

    public void toggleSelection(int pos) {
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
        }
        else {
            selectedItems.put(pos, true);
        }
        notifyDataSetChanged();
    }

    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    /**
     * Removes the item that currently is at the passed in position from the
     * underlying data set.
     *
     * @param position The index of the item to remove.
     */
    public void removeData(int position) {
        items.remove(position);
        notifyDataSetChanged();
    }
}

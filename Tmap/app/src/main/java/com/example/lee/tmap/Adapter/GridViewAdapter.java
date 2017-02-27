package com.example.lee.tmap.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.lee.tmap.Activity.MainActivity;
import com.example.lee.tmap.R;

import java.util.ArrayList;

/**
 * Created by Lee on 2016-11-11.
 */
public class GridViewAdapter extends BaseAdapter {

    public static final String TAG = "GridViewAdapter";

    private final String[] result;
    private Context context;
    private static LayoutInflater inflater = null;

    public GridViewAdapter(Context context, String[] result){
        this.context = context;
        this.result = result;
    }

    public GridViewAdapter(MainActivity mainActivity, String[] POINameList){
        Log.i(TAG, "[ POI Name List ] : " + POINameList);
        result = POINameList;
        context = mainActivity;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return result.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View gridView;

        if( convertView == null ){
            gridView = new View(context);
            gridView = inflater.inflate(R.layout.gridview_item, null);

            TextView textView = (TextView) gridView.findViewById(R.id.tv_destination);
            textView.setText(result[position]);
        }else{
            gridView = (View) convertView;
        }
        return gridView;
    }   // getView
}

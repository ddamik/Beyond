package com.example.lee.tmap;

import android.app.ListActivity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.skp.Tmap.TMapPoint;

import java.util.ArrayList;

/**
 * Created by Lee on 2016-10-20.
 */
public class ListViewAdapter extends BaseAdapter {

    private Context mContext = null;
    private ArrayList<POIItem> mListData = new ArrayList<POIItem>();

    public ListViewAdapter(){}

    public ListViewAdapter(Context mContext) {
        super();
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return mListData.size();
    }

    @Override
    public Object getItem(int position) {
        return mListData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final Context context = parent.getContext();

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listview_item, parent, false);
        }

        TextView tv_name = (TextView) convertView.findViewById(R.id.tv_name);
        TextView tv_address = (TextView) convertView.findViewById(R.id.tv_address);
        TextView tv_distance = (TextView) convertView.findViewById(R.id.tv_distance);

        POIItem poi = mListData.get(pos);

        tv_name.setText(poi.getmName());
        tv_address.setText(poi.getmAddress());
        tv_distance.setText(poi.getmDistance());


        return convertView;
    }

    public void addItem(String name, String address, TMapPoint point) {
        POIItem item = new POIItem();

        item.setmName(name);
        item.setmAddress(address);
        item.setPoint(point);

        mListData.add(item);
    }

    public void autoListAddItem(String name){
        POIItem item = new POIItem();
        item.setmName(name);
        mListData.add(item);
    }
}

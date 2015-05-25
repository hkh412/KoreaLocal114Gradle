package com.hkh.korealocal114.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.hkh.korealocal114.R;
import com.hkh.korealocal114.data.Region;

public class ActionBarDropDownAdapter extends ArrayAdapter<Region>{
	Context mContext;

    public ActionBarDropDownAdapter (Context context, int resource, List<Region> objects) {
        super(context, resource, objects);
        mContext = context;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent){
        View row = convertView;
        if (row == null) {
        	LayoutInflater inflater=(LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	row = inflater.inflate(R.layout.layout_region_list_item, null);
        }
    	TextView tv = (TextView) row.findViewById(R.id.tv_region_name);
    	String regionNm = getItem(position).getName();
    	tv.setText(regionNm);
        return row;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View row = convertView;
        if (row == null) {
        	LayoutInflater inflater=(LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	row = inflater.inflate(R.layout.layout_region_button, null);
        }
        TextView tv = (TextView) row.findViewById(R.id.tv_selected_region);
        String regionNm = getItem(position).getName();
    	tv.setText(regionNm);
        return row;
    }
}

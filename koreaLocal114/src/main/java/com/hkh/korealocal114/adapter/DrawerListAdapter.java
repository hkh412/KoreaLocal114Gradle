package com.hkh.korealocal114.adapter;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.hkh.korealocal114.R;
import com.hkh.korealocal114.data.MenuData;

/**
 * Expandable Drawer ListView Adapter
 * @author hkh
 */
public class DrawerListAdapter extends ArrayAdapter<MenuData> {
	
	private static String TAG = DrawerListAdapter.class.getSimpleName();
	Context mContext;
	List<MenuData> parentNodes;
	List<ArrayList<MenuData>> childNodes;
	LayoutInflater inflater;
	
	public DrawerListAdapter(Context context, int resource, ArrayList<MenuData> objects) {
		super(context, resource, objects);
		mContext = context;
		inflater = ((Activity)mContext).getLayoutInflater();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		/**
//		 * 메뉴명
//		 */
		TextView textView = null;
		if (view == null) {
			view = inflater.inflate(R.layout.drawer_item, null);
		}
		MenuData menuData = getItem(position);
		textView = (TextView) view.findViewById(R.id.list_header);
		textView.setText(menuData.getName());
		return view;
	}
}

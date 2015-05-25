package com.hkh.korealocal114.adapter;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.androidquery.AQuery;
import com.hkh.korealocal114.R;
import com.hkh.korealocal114.data.DataHashMap;
import com.hkh.korealocal114.events.OnFavoriteChangeListener;
import com.hkh.korealocal114.manager.PostStateManager;

public class BoardAdapter extends ArrayAdapter<DataHashMap>
	implements OnCheckedChangeListener, OnClickListener {

	Context mContext;
	AQuery aq;
	int resourceId;
	PostStateManager psm = null;
	List<DataHashMap> data;
	ArrayList<String> visitedList = null;
	List<String> favoriteList = null;
	OnFavoriteChangeListener mFavoriteChangeListener;
	
	// 내가 쓴글: mypost, 즐겨찾기: favorite
	String menuType = null;
	
	public BoardAdapter(Context context, int resource, List<DataHashMap> data,
			ArrayList<String> visitedList) {
		super(context, resource, data);
		
		this.resourceId = resource;
		this.mContext = context;
		this.data = data;
		this.visitedList = visitedList;
		aq = new AQuery(mContext);
		
		psm = PostStateManager.getInstance(mContext);
		favoriteList = psm.getFavoriteList();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		BoardHolder holder = null;
		if (row == null) {
			LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
			row = inflater.inflate(resourceId, parent, false);
			
			holder = new BoardHolder();
			holder.txtCategory = (TextView) row.findViewById(R.id.tv_board_category);
			holder.txtCity = (TextView) row.findViewById(R.id.tv_city);
			holder.txtTitle = (TextView) row.findViewById(R.id.tv_board_title);
			holder.txtDate = (TextView) row.findViewById(R.id.tv_board_date);
			holder.tgbFavorite = (ToggleButton) row.findViewById(R.id.tgb_favorite);
			holder.viewMask = (View) row.findViewById(R.id.view_read_mask);
			holder.tgbFavorite.setOnClickListener(this);
			row.setTag(holder);
			
		} else {
			holder = (BoardHolder)row.getTag();
		}
		
		DataHashMap map = data.get(position);
		if (map == null) {
			return row;
		}
		
		String postId = map.get("postId");
		String category = map.get("category");
		String title = map.get("title");
		String date = map.get("date");
		String city = map.get("city");
		
		holder.txtCategory.setText(category);
		holder.txtTitle.setText(title);
		holder.txtDate.setText(date);
		holder.txtCity.setText(city);
		
		holder.tgbFavorite.setTag(Integer.valueOf(position));
		
		if (favoriteList.indexOf(postId)>=0) {
			holder.tgbFavorite.setChecked(true);
		} else {
			holder.tgbFavorite.setChecked(false);
		}
		
		int visibility = visitedList.contains(postId) ? View.VISIBLE : View.GONE;
		if ("mypost".equals(menuType) || "favorite".equals(menuType)) {
			holder.viewMask.setVisibility(View.GONE);
		} else {
			holder.viewMask.setVisibility(visibility);
		}
		
		return row;
	}

	static class BoardHolder
	{
		TextView txtTitle;
		TextView txtCategory;
		TextView txtDate;
		TextView txtCity;
		ToggleButton tgbFavorite;
		View viewMask;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		Integer position = (Integer)buttonView.getTag();
		DataHashMap data = getItem(position);
		favoriteList = PostStateManager.getInstance(mContext).addFavoritePost(data.get("postId"), data);
		PostStateManager.getInstance(mContext).saveFavoritePost();
//		Log.d("BoardAdapter", "즐겨찾기 클릭: "+position);
	}
	
	@Override
	public void onClick(View buttonView) {
		if (buttonView instanceof ToggleButton) {
			ToggleButton tgb = (ToggleButton) buttonView;
			Integer position = (Integer)buttonView.getTag();
			DataHashMap data = getItem(position);
			String postId = data.get("postId");
			if (tgb.isChecked()) {
				favoriteList = PostStateManager.getInstance(mContext).addFavoritePost(postId, data);
			} else {
				favoriteList = PostStateManager.getInstance(mContext).removeFavoritePost(postId);
			}
			PostStateManager.getInstance(mContext).saveFavoritePost();
			
			if (mFavoriteChangeListener!=null) {
				mFavoriteChangeListener.onFavoriteChanged();
			}
		}
	}
	
	public void setMenuType(String type) {
		menuType = type;
	}
	
	public void setOnFavoriteChangeListener(OnFavoriteChangeListener listener) {
		mFavoriteChangeListener = listener;
	}
}

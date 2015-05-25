package com.hkh.korealocal114.adapter;

import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.androidquery.AQuery;
import com.hkh.korealocal114.R;
import com.hkh.korealocal114.data.DataHashMap;
import com.hkh.korealocal114.data.HeaderData;
import com.hkh.korealocal114.data.PostItem;
import com.hkh.korealocal114.events.OnFavoriteChangeListener;
import com.hkh.korealocal114.events.OnScaleImageViewListener;
import com.hkh.korealocal114.manager.PostStateManager;
import com.hkh.korealocal114.views.CustomImageView;

public class PostViewAdapter extends ArrayAdapter<PostItem> {

	private static String TAG = PostViewAdapter.class.getSimpleName();
	Context mContext;
	int resourceId;
	AQuery aq;
	OnScaleImageViewListener mScaleImageListener;
	OnFavoriteChangeListener mFavoriteChangeListener;
	List<String> favoriteList = null;

	public PostViewAdapter(Context context, int resource, List<PostItem> objects) {
		super(context, resource, objects);
		this.resourceId = resource;
		this.mContext = context;
		aq = new AQuery(mContext);
		favoriteList = PostStateManager.getInstance(mContext).getFavoriteList();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		View view = convertView;
		
		if (view == null) {
			view = ((Activity) mContext).getLayoutInflater().inflate(resourceId, parent, false);
			holder = new ViewHolder();
			holder.textView = (TextView) view.findViewById(R.id.textview);
			holder.headerView = (LinearLayout) view.findViewById(R.id.layout_header);
			holder.imageView = (CustomImageView) view.findViewById(R.id.imageview);
			holder.imageView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (v instanceof CustomImageView && mScaleImageListener != null) {
						String imageUrl = ((CustomImageView)v).getImageUrl();
						mScaleImageListener.onScaleImageView(imageUrl);
					}
				}
			});			
			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}
		
		holder.imageView.setVisibility(View.GONE);
		holder.textView.setVisibility(View.GONE);
		holder.headerView.setVisibility(View.GONE);
		
		PostItem item = this.getItem(position);
		
		switch (item.type) {
		case PostItem.TYPE_HEADER_VIEW:
			holder.headerView.setVisibility(View.VISIBLE);
			TextView tvCity = (TextView) holder.headerView.findViewById(R.id.tv_city_detail);
			TextView tvCategory = (TextView) holder.headerView.findViewById(R.id.tv_detail_category);
			TextView tvTitle = (TextView) holder.headerView.findViewById(R.id.tv_board_cont_title);
			TextView tvDate = (TextView) holder.headerView.findViewById(R.id.tv_board_cont_date);
			TextView tvViewCnt = (TextView) holder.headerView.findViewById(R.id.tv_head_viewcnt);
			ToggleButton tgbFavorite = (ToggleButton) holder.headerView.findViewById(R.id.tgb_detail_favorite);
			
			final HeaderData headerData = item.headerData;
			tvCity.setText(headerData.city);
			tvCategory.setText(headerData.category);
			tvTitle.setText(headerData.title);
			tvDate.setText(headerData.date);
			String viewCntText = mContext.getString(R.string.title_view_count);
			tvViewCnt.setText(String.format(viewCntText, headerData.viewCnt));
			
			if (favoriteList.indexOf(headerData.postId)>=0) {
				tgbFavorite.setChecked(true);
			} else {
				tgbFavorite.setChecked(false);
			}
			tgbFavorite.setOnClickListener(null);
			tgbFavorite.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					if (view instanceof ToggleButton) {
						ToggleButton tgb = (ToggleButton) view;
						DataHashMap data = convertHeaderData(headerData);
						if (tgb.isChecked()) {
							favoriteList = PostStateManager.getInstance(mContext).addFavoritePost(headerData.postId, data);
						} else {
							favoriteList = PostStateManager.getInstance(mContext).removeFavoritePost(headerData.postId);
						}
						PostStateManager.getInstance(mContext).saveFavoritePost();
						if (mFavoriteChangeListener!=null) {
							mFavoriteChangeListener.onFavoriteChanged();
						}
					}
				}
			});
			
			break;
			
		case PostItem.TYPE_TEXT_VIEW:
			holder.textView.setVisibility(View.VISIBLE);	
			holder.textView.setText(Html.fromHtml(item.contText));
			holder.textView.setOnClickListener(null);
			break;
			
		case PostItem.TYPE_IMAGE_NO_LINK:
			holder.imageView.setVisibility(View.VISIBLE);
			holder.imageView.loadImage(item.imageUrl);
			break;
			
		case PostItem.TYPE_IMAGE_WITH_LINK:
			holder.imageView.setVisibility(View.VISIBLE);
			holder.imageView.loadImage(item.imageLink);
			final String imgLink = item.hyperLink;
			holder.imageView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(imgLink));
						mContext.startActivity(intent);
					} catch (ActivityNotFoundException e) {
						Toast.makeText(mContext, 
								mContext.getString(R.string.message_link_invalid), Toast.LENGTH_SHORT).show();
						Log.e(TAG, e.getMessage());
					}
				}
			});
			break;
			
		case PostItem.TYPE_HREF_LINK:
			holder.textView.setVisibility(View.VISIBLE);
			holder.textView.setText(Html.fromHtml(item.contText));
			final String link = item.hyperLink;
			holder.textView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
						mContext.startActivity(intent);
					} catch (ActivityNotFoundException e) {
						Log.e(TAG, e.getMessage());
					}
				}
			});
			break;
			
		default:
			break;
		}
		return view;
	}
	
	static class ViewHolder {
		LinearLayout headerView;
		TextView textView;
		CustomImageView imageView;
	}
	
	public void setOnScaleImageViewListener(OnScaleImageViewListener listener) {
		mScaleImageListener = listener;
	}
	public void setOnFavoriteChangeListener(OnFavoriteChangeListener listener) {
		mFavoriteChangeListener = listener;
	}
	
	private DataHashMap convertHeaderData(HeaderData headerData) {
		DataHashMap map = new DataHashMap();
		map.put("postId", headerData.postId);
		map.put("title", headerData.title);
		map.put("category", headerData.category);
		map.put("city", headerData.city);
		map.put("date", headerData.listDate);
		map.put("link", headerData.link);
		return map;
	}
	
	public void notifyFavoriteChange() {
		favoriteList = PostStateManager.getInstance(mContext).getFavoriteList();
		notifyDataSetChanged();
	}
}

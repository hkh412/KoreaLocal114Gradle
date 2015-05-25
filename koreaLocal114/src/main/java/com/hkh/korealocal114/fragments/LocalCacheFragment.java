package com.hkh.korealocal114.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.androidquery.callback.AjaxStatus;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.hkh.korealocal114.MainActivity;
import com.hkh.korealocal114.PagerActivity;
import com.hkh.korealocal114.R;
import com.hkh.korealocal114.WriteActivity;
import com.hkh.korealocal114.adapter.BoardAdapter;
import com.hkh.korealocal114.config.Config;
import com.hkh.korealocal114.config.Links;
import com.hkh.korealocal114.data.DataHashMap;
import com.hkh.korealocal114.events.OnFavoriteChangeListener;
import com.hkh.korealocal114.manager.PostStateManager;
import com.hkh.korealocal114.service.DeletePostService;
import com.hkh.korealocal114.service.RepostService;
import com.hkh.korealocal114.util.Util;

public class LocalCacheFragment extends StateFragment
	implements OnItemClickListener, OnFavoriteChangeListener {

	private static String TAG = LocalCacheFragment.class.getSimpleName();
	Context mContext;
	
	PullToRefreshListView boardList;
	ArrayList<DataHashMap> data = null;
	BoardAdapter boardAdapter = null;
	boolean dataLoaded = false;
	
	ArrayList<String> visitedList = null;
	String city = null;
	String type = "favorite";
	
	DataHashMap delPost = null;
	
	public LocalCacheFragment() {
		data = new ArrayList<DataHashMap>();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		mContext = getActivity();
		
		View rootView = inflater.inflate(R.layout.fragment_board, container, false);
		boardList = (PullToRefreshListView) rootView.findViewById(R.id.list_board);
		boardList.setMode(Mode.PULL_FROM_START);
		boardList.setOnRefreshListener(new OnRefreshListener2<ListView>() {
			@Override
			public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
				fetchLocalData();
			}

			@Override
			public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
			}
		});
		boardList.setOnItemClickListener(this);
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (dataLoaded) {
			boardList.setAdapter(boardAdapter);
			return;
		}
		
		Bundle bundle = getArguments();
		city = bundle.getString("city");
		type = bundle.getString("type");
		
		visitedList = PostStateManager.getInstance(mContext).getVisitedList(type);
		boardAdapter = new BoardAdapter(mContext, R.layout.list_item_board, data, visitedList);
		boardList.setAdapter(boardAdapter);
		boardAdapter.setMenuType(type);
		boardAdapter.setOnFavoriteChangeListener(this);

		// 내가 쓴글일 경우 수정, 삭제 기능 추가
		if (type.equals("mypost")) {
			registerForContextMenu(boardList.getRefreshableView());
		}
		
		fetchLocalData();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = ((Activity) mContext).getMenuInflater();
		inflater.inflate(R.menu.context_menu_mypost, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.action_repost:
			// 재등록
			repost((int)info.id);
			return true;
			
		case R.id.action_edit_post:
			// 글 수정
			editPost((int)info.id);
			return true;
			
		case R.id.action_delete_post:
			// 글 삭제
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setPositiveButton(R.string.label_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							deletePost((int)info.id);
						}
					});
			builder.setNegativeButton(R.string.label_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
						}
					});
			builder.setMessage(mContext.getString(R.string.message_confirm_delete_post));
			builder.create().show();
			return true;
			
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		DataHashMap map = (DataHashMap) parent.getItemAtPosition(position);
		if (map == null) {
			return;
		}
		
		String link = map.get("link");
		String postId = map.get("postId");
		
		BoardDetailFragment detailFragment = new BoardDetailFragment();
		Bundle bundle = new Bundle();
		bundle.putString("link", link+"&city1="+city);
		
		// 방문한 post id 저장
		Util.addUniqueItem2LimitedSize(visitedList, postId,
				Config.LIST_LIMIT);
		
		detailFragment.setArguments(bundle);
		((PagerActivity)mContext).setDetailFragment(detailFragment);
	}
	
	/**
	 * 글 재등록
	 * @param position
	 */
	private void repost(int position) {
		((PagerActivity)mContext).showLoadingIndicator();
		
		DataHashMap map = data.get(position);
		String link = map.get("link");
		String postId = map.get("postId");
		String bo_table = Util.getValueFromUrl(link, "bo_table");
		String url = Links.REFRESH_POST+"?bo_table="+bo_table+"&id="+postId+"&mode=1";
		
		RepostService service = new RepostService(mContext);
		service.setService(url, this, "onRepostComplete");
		service.request();
	}
	
	public void onRepostComplete(String url, String html, AjaxStatus ajaxStatus) {
		((PagerActivity)mContext).hideLoadingIndicator();
		if (ajaxStatus.getCode() == 200) {
			// capturing xxxx in "<script>...alert('xxxx')...</script>"
			Pattern pattern = Pattern.compile("\\('(.*?)'\\)");
			Matcher matcher = pattern.matcher(html);
			
			if (matcher.find()){
				String msg = matcher.group(1);
				Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
			} else {
			}

		} else {
			Toast.makeText(mContext, mContext.getString(R.string.message_fail_to_repost),
					Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * 글 수정
	 * @param position
	 */
	private void editPost(int position) {
		DataHashMap map = data.get(position);
		Intent intent = new Intent(mContext, WriteActivity.class);
		Bundle bundle = Util.mapToBundle(map);
		bundle.putInt("requestCode", Config.REQUEST_CODE_EDIT);
		intent.putExtras(bundle);
		startActivityForResult(intent, Config.REQUEST_CODE_EDIT);
	}
	
	/**
	 * 글 삭제
	 * @param position
	 */
	private void deletePost(int position) {
		((PagerActivity)mContext).showLoadingIndicator();
		
		DataHashMap map = data.get(position);
		delPost = map;
		
		String link = map.get("link");
		String postId = map.get("postId");
		String bo_table = Util.getValueFromUrl(link, "bo_table");
		String url = Links.DELETE_POST+"?bo_table="+bo_table+"&sel="+postId;
		
		DeletePostService service = new DeletePostService(mContext);
		service.setService(url, this, "onDeletePost");
		service.request();
	}
	
	public void onDeletePost(String url, String html, AjaxStatus ajaxStatus) {
		((PagerActivity)mContext).hideLoadingIndicator();
		if (ajaxStatus.getCode() == 200) {
			// post deleted from server successfully
			// delete from local cache
			if (delPost != null) {
				String postId = delPost.get("postId");
				PostStateManager.getInstance(mContext).removeMyPost(postId);
				PostStateManager.getInstance(mContext).saveMyPost();
				
				data.remove(delPost);
				boardAdapter.notifyDataSetChanged();
			}
			Toast.makeText(mContext, mContext.getString(R.string.message_post_deleted),
					Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(mContext, mContext.getString(R.string.message_fail_to_delete_post),
					Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * local cache 에서 내가 쓴글 조회
	 */
	public void fetchLocalData() {
		List<DataHashMap> postList = new ArrayList<DataHashMap>();
		if (type.equals("favorite")) {
			postList = PostStateManager.getInstance(mContext).getFavoritePostList();
		} else if (type.equals("mypost")) {
			postList = PostStateManager.getInstance(mContext).getMyPostList();
		} else {
		}
		
		data.clear();
		dataLoaded = true;
        data.addAll(postList);
        boardAdapter.notifyDataSetChanged();
        
        TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				((PagerActivity)mContext).runOnUiThread(new Runnable(){
					@Override
					public void run() {
						boardList.onRefreshComplete();
					}
				});
			}
		};
		Timer timer = new Timer();
		timer.schedule(timerTask, 500);
        
        // 좌측메뉴 최초 한번 로딩
        if (getActivity() instanceof MainActivity) {
        	((MainActivity) getActivity()).openDrawerList();
        }
	}
	
	@Override
	public void notifyPageActivated() {
		super.notifyPageActivated();
		// 내가 쓴글일 경우 수정, 삭제 기능 추가
		if (type.equals("mypost") && boardList != null) {
			registerForContextMenu(boardList.getRefreshableView());
		}
	}

	public void notifyFavoriteChange() {
		if (boardAdapter != null) {
			 boardAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onFavoriteChanged() {
		((PagerActivity)getActivity()).notifyFavoriteChange();		
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Config.REQUEST_CODE_EDIT) {
			if (resultCode == Activity.RESULT_OK) {
			}
		}
	}
}

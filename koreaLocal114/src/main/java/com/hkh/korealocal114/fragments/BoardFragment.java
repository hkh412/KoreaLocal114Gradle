package com.hkh.korealocal114.fragments;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnLastItemVisibleListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.hkh.korealocal114.MainActivity;
import com.hkh.korealocal114.PagerActivity;
import com.hkh.korealocal114.R;
import com.hkh.korealocal114.adapter.BoardAdapter;
import com.hkh.korealocal114.config.Config;
import com.hkh.korealocal114.config.Links;
import com.hkh.korealocal114.data.DataHashMap;
import com.hkh.korealocal114.events.OnFavoriteChangeListener;
import com.hkh.korealocal114.manager.CookieManager;
import com.hkh.korealocal114.manager.PostStateManager;
import com.hkh.korealocal114.service.QueryListService;
import com.hkh.korealocal114.util.ParseUtil;
import com.hkh.korealocal114.util.Util;

public class BoardFragment extends SearchableFragment 
	implements OnItemClickListener, OnFavoriteChangeListener {
	
	private static String TAG = BoardFragment.class.getSimpleName();
	Context mContext;
	AQuery aq;
	
	PullToRefreshListView boardList;
	ArrayList<DataHashMap> data = null;
	BoardAdapter boardAdapter = null;
	boolean dataLoaded = false;
	boolean mergeData = false;
	
	/**
	 * 게시판 조회하는 url
	 */
	String boardUrl = null;
	
	ArrayList<String> visitedList = null;
	
	int currentPage=1;

	public BoardFragment() {
		data = new ArrayList<DataHashMap>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		mContext = getActivity();
		aq = new AQuery(mContext);
		
		View rootView = inflater.inflate(R.layout.fragment_board, container, false);
		boardList = (PullToRefreshListView) rootView.findViewById(R.id.list_board);
		boardList.setOnRefreshListener(new OnRefreshListener2<ListView>() {
			@Override
			public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
				mergeData = false;
				currentPage = 1;
				queryData(boardUrl);
			}

			@Override
			public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
				mergeData = true;
				currentPage++;
				queryData(boardUrl+"&page="+currentPage);
			}
		});
		boardList.setOnLastItemVisibleListener(new OnLastItemVisibleListener() {
			@Override
			public void onLastItemVisible() {
				boardList.setCurrentMode(Mode.PULL_FROM_END);
				boardList.setRefreshing(true);
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
		boardUrl = Links.DOMAIN_URL+bundle.getString("url");
		
		/**
		 * 방문한 페이지 저장
		 */
		PostStateManager psmgr = PostStateManager.getInstance(mContext);
		visitedList = psmgr.getVisitedList(boardUrl);
		
		boardAdapter = new BoardAdapter(mContext, R.layout.list_item_board, data, visitedList);
		boardList.setAdapter(boardAdapter);
		boardAdapter.setOnFavoriteChangeListener(this);
		
		if (searchMode) {
			querySearchData();
		} else {
			queryData(boardUrl);
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		DataHashMap map = data.get(position-1);
		String link = map.get("link");
		String postId = map.get("postId");
		String city = Util.getValueFromUrl(boardUrl, "city1");
		
		BoardDetailFragment detailFragment = new BoardDetailFragment();
		Bundle bundle = new Bundle();
		bundle.putString("link", link+"&city1="+city);
		
		// 방문한 post id 저장
		Util.addUniqueItem2LimitedSize(visitedList, postId,
				Config.LIST_LIMIT);
		
		detailFragment.setArguments(bundle);
		((PagerActivity)mContext).setDetailFragment(detailFragment);
	}
	
	@Override
	public void queryData(String url) {
		((PagerActivity)mContext).showLoadingIndicator();
		QueryListService service = new QueryListService(mContext);
		service.setService(url, this, "onQueryResult");
		service.request();
	}
	
	public void onQueryResult(String url, String html, AjaxStatus ajaxStatus) {
		((PagerActivity)mContext).hideLoadingIndicator();
		boardList.onRefreshComplete();
		boardList.setCurrentMode(Mode.PULL_FROM_START);
		
		if (html == null) {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_network_unavailable), Toast.LENGTH_SHORT).show();
			return;
		}
		
		// set cookies
		if (!CookieManager.getInstance(mContext).hasCookies()) {
			List<Cookie> cookies = ajaxStatus.getCookies();
			CookieManager.getInstance(mContext).setCookies(cookies);
		}
		
		dataLoaded = true;
		Element body = Jsoup.parse(html).body();
        Elements elements = body.select("ul.list-wrap li");
        ArrayList<DataHashMap> newList = ParseUtil.parseBoardElements(elements);	
        
        if (mergeData) {
            ParseUtil.mergeList(data, newList);
        } else {
            data.clear();
            data.addAll(newList);
        }
        boardAdapter.notifyDataSetChanged();
        
        // 좌측메뉴 최초 한번 로딩
        if (getActivity() instanceof MainActivity) {
        	((MainActivity) getActivity()).openDrawerList();
        }
	}

	@Override
	public void querySearchData() {
		String url = boardUrl+"&sf=0&stx="+searchQuery;
		querySearchData(url);
	}

	@Override
	public void querySearchData(int page) {
		String url = boardUrl+"&sf=0&stx="+searchQuery+"&page="+page;
		querySearchData(url);
	}

	@Override
	public void querySearchData(String url) {
		((PagerActivity)mContext).showLoadingIndicator();
		AjaxCallback<String> cb = new AjaxCallback<String>();           
		cb.url(url).type(String.class).weakHandler(this, "onQueryResult");
		aq.ajax(cb);
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
}

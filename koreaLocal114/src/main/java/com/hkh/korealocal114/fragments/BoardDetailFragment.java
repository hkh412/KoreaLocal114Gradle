package com.hkh.korealocal114.fragments;

import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.androidquery.callback.AjaxStatus;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.hkh.korealocal114.PagerActivity;
import com.hkh.korealocal114.R;
import com.hkh.korealocal114.ScaleImageActivity;
import com.hkh.korealocal114.adapter.PostViewAdapter;
import com.hkh.korealocal114.config.HTMLAttrs;
import com.hkh.korealocal114.config.HTMLTags;
import com.hkh.korealocal114.config.Links;
import com.hkh.korealocal114.data.HeaderData;
import com.hkh.korealocal114.data.PostItem;
import com.hkh.korealocal114.events.OnFavoriteChangeListener;
import com.hkh.korealocal114.events.OnScaleImageViewListener;
import com.hkh.korealocal114.service.QueryDetailPageService;
import com.hkh.korealocal114.util.Util;

public class BoardDetailFragment extends StateFragment 
	implements OnClickListener, OnScaleImageViewListener, OnFavoriteChangeListener {

	private final String TAG = BoardDetailFragment.class.getSimpleName();
	private final String IMAGE_URL_KEY = "image_url_key";
	
	Context mContext;
	
	InputMethodManager imm;
	
	/**
	 * 상세뷰를 기본이 되는 ListView
	 */
	PullToRefreshListView ptrListView;
	
	/**
	 * 상세뷰 ListView Adapter
	 */
	PostViewAdapter postAdapter;
	
	/**
	 * 댓글 쓰기
	 */
	LinearLayout layoutWriteCmt;
	EditText etComment;
	Button btnSendComment;
	
	/**
	 * 이미지 url 주소
	 */
	ArrayList<String> imageUrls;
	
	/**
	 * 화면구성 데이터
	 */
	ArrayList<PostItem> items;
	
	/**
	 * 게시글  링크
	 */
	String link;
	
	StringBuffer plainText = new StringBuffer();
	
	public BoardDetailFragment() {
		items = new ArrayList<PostItem>();
		imageUrls = new ArrayList<String>();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mContext = getActivity();
		View rootView = inflater.inflate(R.layout.fragment_board_detail, container, false);
		ptrListView = (PullToRefreshListView) rootView.findViewById(R.id.layout_root);
		ptrListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				requestPage(link);
			}
		});
		registerForContextMenu(ptrListView);
		
		postAdapter = new PostViewAdapter(mContext,
				R.layout.list_item_post, items);
		postAdapter.setOnFavoriteChangeListener(this);
		postAdapter.setOnScaleImageViewListener(this);
		ptrListView.setAdapter(postAdapter);
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Bundle bundle = getArguments();
		link = bundle.getString("link");
		requestPage(link);
	}

	@Override
	public void onScaleImageView(String imageUrl) {
		Intent intent = new Intent(mContext, ScaleImageActivity.class);
		Bundle bundle = new Bundle();
		bundle.putStringArrayList(IMAGE_URL_KEY, imageUrls);
		bundle.putInt("position", imageUrls.indexOf(imageUrl));
		intent.putExtras(bundle);

		startActivity(intent);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = ((Activity) mContext).getMenuInflater();
	    inflater.inflate(R.menu.context_menu, menu);
	}

	public void requestPage(String link) {
		((PagerActivity)mContext).showLoadingIndicator();
		
		QueryDetailPageService service = new QueryDetailPageService(mContext);
		service.setService(link, this, "onQueryResult");
		service.request();
	}
	
	public void onQueryResult(String url, String html, AjaxStatus ajaxStatus) {
		items.removeAll(items);
		
		((PagerActivity)mContext).hideLoadingIndicator();
		ptrListView.onRefreshComplete();
		
		if (html == null) {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_network_unavailable), Toast.LENGTH_SHORT).show();
			redirectToList();
			return;
		}
        Document document = Jsoup.parse(html);
        Element body = document.body();
        
        // 자바 스크립트 제거
        body.select("script, jscript").remove();
        Elements contElm = body.select("div.layout");
        if (contElm.size() > 0) {
        	Element element = contElm.get(contElm.size()-1);
        	parseHeaderElement(element);
        	
        	Elements conts = element.select("div.article-detail div.content");
        	if (conts.size() > 0) {
        		Element cont = conts.get(0);
        		parseContentElement(cont);
        	}
        	
        	Elements gallery = element.select("div.article-detail div.gallery-view");
        	if (gallery.size() > 0) {
        		Elements imgElms = gallery.select("ul.small-img li");
        		parseImageElement(imgElms);
        	}
        } else {
        	Toast.makeText(mContext, mContext.getString(R.string.message_post_not_exist), 
        			Toast.LENGTH_LONG).show();
        	redirectToList();
        	return;
        }
        
		// 리스트 display
		postAdapter.notifyDataSetChanged();
	}
	
	/**
	 * 게시글 목록으로 이동
	 */
	private void redirectToList() {
		if (mContext != null) {
			((PagerActivity)mContext).redirectToList();
		}
	}
	
	/**
	 * 헤더 파싱
	 * @param elements
	 */
	private void parseHeaderElement(Element element) {
        HeaderData headerData = new HeaderData();
        headerData.title = element.select("div.article-title h1").text();
        String category = headerData.title.substring(headerData.title.lastIndexOf('-')+2);
        category = category.replace("(", "").replace(")", "");
        String city = category.split("\\s{1}")[0];
        
        headerData.postId = Util.getValueFromUrl(link, "id");
        headerData.title = headerData.title.substring(0, headerData.title.lastIndexOf('-'));
        headerData.city = city;
        headerData.category = category;
        headerData.link = link;
        
        Elements metaElms = element.select("ul.article-meta li");
        if (metaElms.size()>2) {
        	String[] dateTmps = metaElms.get(0).text().split("[\\s]");
        	headerData.date = dateTmps[1]+" "+dateTmps[2];
        	headerData.listDate = dateTmps[1].substring(dateTmps[1].indexOf("-")+1);
        	headerData.viewCnt = metaElms.get(2).text().split("[\\s]")[1];
        }
        
        PostItem headerItem = new PostItem();
        headerItem.type = PostItem.TYPE_HEADER_VIEW;
        headerItem.headerData = headerData;
        items.add(headerItem);
	}
	
	/**
	 * 갤러리 파싱
	 * @param element
	 */
	private void parseImageElement(Elements elements) {
		for (int i=0; i<elements.size(); i++) {
			Element imgElm = elements.get(i).select("img").first();
			String imgUrl = imgElm.attr("data-img").split("[@]")[0];
			createImageView(Links.DOMAIN_URL+imgUrl);
		}
	}
	
	/**
	 * 컨텐츠 영역 파싱
	 * @param elements
	 */
	private void parseContentElement(Element element) {
		String[] lines = element.html().split("\n");
		for (int i=0; i<lines.length; i++) {
			Element body = Jsoup.parse(lines[i]).body();
			
			// plaintext 인 경우 text buffer에 추가
			int size = body.children().size();
			if (size <= 0) {
				plainText.append(lines[i]);
				continue;
				
			} else if (size == 1) {
				// 하위 노드가 하나뿐인경우 파싱시도
				Element e = body.child(0);
				// 태그 비교
				if (e.tagName().equals(HTMLTags.CENTER)) {
					// recursive
					parseContentElement(e);
				} else if (e.tagName().equals(HTMLTags.P)) {
					// recursive
					parseContentElement(e);
					
				} else if (e.tagName().equals(HTMLTags.B)) {
					// recursive
					parseContentElement(e);
					
				} else if (e.tagName().equals(HTMLTags.FONT)) {
					if (e.select("a").size() > 0) {
						createTextView(plainText);
						createHyperLinkView(e.select("a").get(0));
					} else {
						plainText.append(lines[i]);
					}
				} 
				else if (e.tagName().equals(HTMLTags.SPAN)
						&& e.children().size() > 0 
						&& Util.checkChildrenTag(e, HTMLTags.IMG)) {
					// SPAN 하위에 IMG 태그가 있는 경우 img 파싱을 위해 재귀호출한다.
					plainText.append(e.text());
					parseContentElement(e.child(0));
				} 
				else if (e.tagName().equals(HTMLTags.A)) {
					createTextView(plainText);
					createHyperLinkView(e);
					
				} else {
					plainText.append(lines[i]);
					if (plainText.length() > 1500) {
						createTextView(plainText);
					}
				}
				
			} else {
				// 하위노드 1개이상일 경우 재귀호출
				parseContentElement(body);
			}
		} // end for
		createTextView(plainText);
	}
	
	private void createTextView(StringBuffer plainText) {
		if (plainText.length() <= 0) {
			return;
		}
		PostItem item = new PostItem();
		item.type = PostItem.TYPE_TEXT_VIEW;
		item.contText = plainText.toString().trim();
		items.add(item);
		plainText.delete(0, plainText.length());
	}
	
	private void createImageView(String imageUrl) {
		Util.addUniqueItem(imageUrls, imageUrl);
		PostItem item = new PostItem();
		item.type = PostItem.TYPE_IMAGE_NO_LINK;
		item.imageUrl = imageUrl;
		items.add(item);
	}
	
	private void createHyperLinkView(Element e) {
		PostItem item = new PostItem();
		Elements children = e.children();
		Element child = null;
		if (children.size() > 0) {
			child = children.get(0);
		}
		if (child != null && child.tagName().equals(HTMLTags.IMG)) {
			item.imageLink = child.attr("src");
			item.type = PostItem.TYPE_IMAGE_WITH_LINK;
		} else {
			item.contText = e.toString();
			item.type = PostItem.TYPE_HREF_LINK;
		}
		item.element = e;
		item.hyperLink = e.attr(HTMLAttrs.HREF);
		items.add(item);
	}
	
	@SuppressWarnings("unused")
	private void createWebView(Element e) {
		PostItem item = new PostItem();
		item.element = e;
		item.type = PostItem.TYPE_WEB_VIEW;
		items.add(item);
	}
	
	private void createAudioView(Element e) {
		PostItem item = new PostItem();
		item.type = PostItem.TYPE_AUDIO_VIEW;
		item.audioLink = e.attr("src");
		items.add(item);
	}
	
	/**
	 * 상세 ContextMenu operation 처리
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ClipboardManager cpm = null;
		ClipData clipData = null;
		int id = item.getItemId();
		if (R.id.action_copy_contents == id) {
			// 게시글 내용복사
        	String contents = copyPostContents();
        	cpm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        	clipData = ClipData.newPlainText("post contents", contents);
        	cpm.setPrimaryClip(clipData);
        	Toast.makeText(mContext,
        			mContext.getString(R.string.message_copy_contents_success), Toast.LENGTH_SHORT).show();
            return true;
            
		} else if (R.id.action_view_in_browser == id) {
			// 웹브라우저에서 보기
        	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
			mContext.startActivity(intent);
            return true;
            
		} else if (R.id.action_copy_url == id) {
			// 게시글 링크복사
        	cpm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        	clipData = ClipData.newPlainText("post url", link);
        	cpm.setPrimaryClip(clipData);
        	Toast.makeText(mContext,
        			mContext.getString(R.string.message_copy_url_success), Toast.LENGTH_SHORT).show();
        	return true;
        	
		} else if (R.id.action_propose == id) {
			// 건의하기
			Intent intent = Util.reportBugs(mContext);
            startActivity(Intent.createChooser(intent, "Email 건의 및 버그 신고하기"));
        	return true;
        	
		} else {
            return super.onContextItemSelected(item);
	    }
	}
	
	public void hideSoftKeyboard(Context context) {
		if (context != null) {
			imm = (InputMethodManager)context.getSystemService(
					Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(etComment.getWindowToken(), 0);
		}
	}
	
	/**
	 * 게시글 내용복사
	 */
	private String copyPostContents() {
		StringBuffer sb = new StringBuffer();
		for (PostItem item : items) {
			if (item.type == PostItem.TYPE_TEXT_VIEW) {
				if (item.contText != null && item.contText.length() > 0) {
					sb.append(item.contText);
				}
			} else if (item.type == PostItem.TYPE_HREF_LINK) {
				if (item.hyperLink != null && item.contText.length() > 0) {
					sb.append(item.hyperLink);
				}
			}
		}
		return Html.fromHtml(sb.toString()).toString();
	}
	
	@Override
	public void onClick(View view) {
		
	}

	@Override
	public void onFavoriteChanged() {
		((PagerActivity)getActivity()).notifyFavoriteChange();
	}
	
	@Override
	public void notifyPageActivated() {
		super.notifyPageActivated();
		if (ptrListView != null) {
			registerForContextMenu(ptrListView);
		}
	}

	public void notifyFavoriteChange() {
		if (postAdapter!=null) {
			postAdapter.notifyFavoriteChange();
		}
	}
}

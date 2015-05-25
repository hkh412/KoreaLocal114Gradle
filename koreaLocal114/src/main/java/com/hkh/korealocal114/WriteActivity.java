package com.hkh.korealocal114;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.hkh.korealocal114.config.Config;
import com.hkh.korealocal114.config.Links;
import com.hkh.korealocal114.data.DataHashMap;
import com.hkh.korealocal114.data.MenuData;
import com.hkh.korealocal114.manager.CookieManager;
import com.hkh.korealocal114.manager.PostStateManager;
import com.hkh.korealocal114.service.QueryCategoryService;
import com.hkh.korealocal114.service.QueryDetailPageService;
import com.hkh.korealocal114.service.WritePostService;
import com.hkh.korealocal114.util.ParseUtil;
import com.hkh.korealocal114.util.Util;

public class WriteActivity extends Activity 
	implements OnItemSelectedListener {
	
	Context mContext = null;
	
	// Activity request code
	int requestCode = -1;
	
	String domainUrl = null;
	
	LinearLayout mLoadingLayout;
	
	LinearLayout layoutRegion;
	LinearLayout layoutCategory;
	
	Spinner spinnerCity1;
	Spinner spinnerCity2;
	Spinner spinnerCate1;
	Spinner spinnerCate2;
	
	Button btnOk;
	Button btnCancel;
	
	EditText etTitle;
	EditText etContent;
	
	ArrayAdapter<String> city2Adapter = null;
	ArrayList<String> city2Default = null;
	
	String menuId = null;			// job
	String menuName = null;			// 구인정보
	
	/**
	 * 글 수정시 이전 글에 등록된 도시 정보
	 */
	String city1 = null;			// 시/도 (경기도)
	String city2 = null;			// 시/구/군 (경기 안성시) parsing 필요함
	String postId = "";
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_write);
		
		mContext = this;
		mLoadingLayout = (LinearLayout) findViewById(R.id.layout_indicator);
		layoutRegion = (LinearLayout)findViewById(R.id.layout_region);
		layoutCategory = (LinearLayout)findViewById(R.id.layout_category);
		
		etTitle = (EditText)findViewById(R.id.et_title);
		etContent = (EditText)findViewById(R.id.et_content);
		
		spinnerCity1 = (Spinner)findViewById(R.id.spinner_city1);
		spinnerCity2 = (Spinner)findViewById(R.id.spinner_city2);
		spinnerCate1 = (Spinner)findViewById(R.id.spinner_cate1);
		spinnerCate2 = (Spinner)findViewById(R.id.spinner_cate2);
		
		btnOk = (Button)findViewById(R.id.btn_send_write);
		btnCancel = (Button)findViewById(R.id.btn_cancel_write);
		
		// 확인
		btnOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// validation check
				boolean isValid = checkValidation();
				if (!isValid) {
					return;
				}
				sendPost();
			}
		});
		
		// 취소
		btnCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setPositiveButton(R.string.label_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								setResult(RESULT_CANCELED);
								finish();
							}
						});
				builder.setNegativeButton(R.string.label_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
							}
						});
				builder.setMessage(mContext.getString(R.string.message_cancel_editting));
				builder.create().show();
			}
		});
		
		// 도시변경 이벤트
		spinnerCity1.setOnItemSelectedListener(this);
		
		// 시/구/군 데이터
		city2Default = new ArrayList<String>();
		city2Default.add("시/구/군");
		city2Adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1);
		spinnerCity2.setAdapter(city2Adapter);
		city2Adapter.addAll(city2Default);
		MenuData menuData = null;
		
		Bundle bundle = getIntent().getExtras();
		requestCode = bundle.getInt("requestCode");
		
		if (requestCode == Config.REQUEST_CODE_NEW) {
			// 글등록
			menuData = (MenuData) bundle.getSerializable("menu_data");
			if (menuData == null) {
				setResult(RESULT_CANCELED);
				finish();
				return;
			}
			menuName = menuData.getName();
			getActionBar().setTitle(mContext.getString(R.string.action_write)+" - "+menuName);
			
		} else if (requestCode == Config.REQUEST_CODE_EDIT) {
			// 글 수정
			String link = bundle.getString("link");
			String bbsId = Util.getValueFromUrl(link, "bo_table");
			postId = bundle.getString("postId");

			List<MenuData> menuList = Util.loadMenuData(mContext);
			menuData = Util.findMenuById(menuList, bbsId);
			
			// set pre-posted data
			String title = bundle.getString("title");
			etTitle.setText(title);
			
			String content = bundle.getString("content");
			etContent.setText(content);
			
			// 도시정보는 category 조회 후 적용
			city1 = bundle.getString("city");
			city2 = bundle.getString("category");
			if (city2 != null) {
				int spaceIndex = city2.indexOf(" ");
				city2 = city2.substring((spaceIndex+1));
			}
			if (city1 != null) {
				ArrayAdapter city1Adapter = (ArrayAdapter)spinnerCity1.getAdapter();
				spinnerCity1.setSelection(city1Adapter.getPosition(city1));
			}
			menuName = menuData.getName();
			getActionBar().setTitle(mContext.getString(R.string.action_edit)+" - "+menuName);
			
		} else {
			// unknown request code
			finish();
			return;
		}
		menuId = menuData.getId();
		setSpinnerVisibility();

		showLoadingIndicator();
		queryCategory(menuId);
	}
	
	/**
	 * 입력정보 Validation
	 * @return
	 */
	private boolean checkValidation() {
		if (spinnerCity1.getSelectedItemPosition() <= 0) {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_not_select_city1), Toast.LENGTH_LONG).show();
			return false;
		}
		if (spinnerCity2.getSelectedItemPosition() <= 0) {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_not_select_city2), Toast.LENGTH_LONG).show();
			return false;
		}
		if (spinnerCate1.getSelectedItemPosition() <= 0) {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_not_select_cate1), Toast.LENGTH_LONG).show();
			return false;
		}
		if ("house".equals(menuId)) {
			if (spinnerCate2.getSelectedItemPosition() <= 0) {
				Toast.makeText(mContext,
						mContext.getString(R.string.message_not_select_cate2), Toast.LENGTH_LONG).show();
				return false;
			}
		}
		if (etTitle.getText().length()<=0) {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_no_title), Toast.LENGTH_LONG).show();
			return false;
		}
		if (etContent.getText().length()<=0) {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_no_content), Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	
	private void sendPost() {
		showLoadingIndicator();
		
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        
    	String city1 = (String)spinnerCity1.getSelectedItem();
    	String city2 = (String)spinnerCity2.getSelectedItem();
    	String cate1 = (String)spinnerCate1.getSelectedItem();
    	pairs.add(new BasicNameValuePair("city1", city1)); 
    	pairs.add(new BasicNameValuePair("city2", city2));
    	pairs.add(new BasicNameValuePair("cate1", cate1));
    	if (("house").equals(menuId)) {
    		String cate2 = (String)spinnerCate2.getSelectedItem();
    		pairs.add(new BasicNameValuePair("cate2", cate2));
    	}
    	
    	String ssid = CookieManager.getInstance(mContext).getCookieSSID();
    	ssid = "141612811976";
    	pairs.add(new BasicNameValuePair("ssid", ssid));
        pairs.add(new BasicNameValuePair("a_type", "on"));
        pairs.add(new BasicNameValuePair("bo_table", menuId));
        pairs.add(new BasicNameValuePair("id", postId));
        pairs.add(new BasicNameValuePair("file2", "")); 
        pairs.add(new BasicNameValuePair("title", etTitle.getText().toString()));
        pairs.add(new BasicNameValuePair("content", etContent.getText().toString()));    
        HttpEntity entity = null;
		try {
			entity = new UrlEncodedFormEntity(pairs, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		headers.put("Referer", "http://www.114114.com/board.php?bo_mode=write");
		
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(AQuery.POST_ENTITY, entity);
        
        // to my google app engine python server
        WritePostService appspot = new WritePostService(mContext);
        appspot.setService(Links.TEST_POST_UPLOAD, this, null);
        appspot.setParams(params);
        appspot.setHeaders(headers);
        appspot.request();
        
        // to 114114.com server
        WritePostService service = new WritePostService(mContext);
        service.setService(Links.POST_UPLOAD, this, "onPostSent");
        service.setParams(params);
        service.setHeaders(headers);
        service.request();
        
	}
	
	/**
	 * 114114.com 서버에 업로드한 callback
	 * @param url
	 * @param html
	 * @param ajaxStatus
	 */
	public void onPostSent(String url, String html, AjaxStatus ajaxStatus) {
		hideLoadingIndicator();
		if (html == null) {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_network_unavailable), Toast.LENGTH_SHORT).show();
			return;
		}
		int index = -1;
		if ((index = html.indexOf("('"))>=0) {
			html = html.substring(index+2);
			int endIndex = html.indexOf("'");
			String alert = html.substring(0, endIndex);
			Toast.makeText(mContext, alert, Toast.LENGTH_LONG).show();
			return;
		}
		
		String[] tmps = html.split("[']");
		if (tmps.length > 2) {
			String respUrl = tmps[1];
			saveMyFavoritePost(respUrl);
		}
		
		if (ajaxStatus.getCode() == 200) {
			if (requestCode == Config.REQUEST_CODE_NEW) {
				Toast.makeText(mContext, mContext.getString(R.string.message_post_sent),
						Toast.LENGTH_LONG).show();
				
			} else if (requestCode == Config.REQUEST_CODE_EDIT) {
				Toast.makeText(mContext, mContext.getString(R.string.message_post_edited),
						Toast.LENGTH_LONG).show();
			}
		}
		
		setResult(RESULT_OK);
		finish();
	}
	
	/**
	 * 분류조회
	 */
	private void queryCategory(String bo_table) {
		long dd = new Random().nextLong() * 100000000;
		String url = Links.QUERY_CATE1+"?sel=&bo_table="+bo_table+"&cate1=&cate2=&dd="+dd;
		showLoadingIndicator();
		QueryCategoryService service = new QueryCategoryService(mContext);
		service.setService(url, this, "onQueryCateResult");
		service.request();
	}
	
	public void onQueryCateResult(String url, String html, AjaxStatus ajaxStatus) {
		hideLoadingIndicator();
		if (html == null) {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_network_unavailable), Toast.LENGTH_SHORT).show();
			return;
		}
		
		System.out.println(html);
		Element cateElement = Jsoup.parse(html);
		Elements options = cateElement.select("#cate1 option");
		ArrayList<String> cate1List = new ArrayList<String>();
		for (int i=0; i<options.size(); i++) {
			String value = options.get(i).attr("value");
			if (value.equals("")) {
				value = "분류선택";
			}
			cate1List.add(value);
		}
		ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(mContext,
		        android.R.layout.simple_list_item_1, cate1List);
		spinnerCate1.setAdapter(adapter1);
	}
	
	/**
	 * 시/구/군 조회
	 * @param city1
	 */
	public void queryCity2(String city1) {
		long dd = new Random().nextLong() * 100000000;
		String url = Links.QUERY_CITY2+"?title="+city1+"&dd="+dd;
		showLoadingIndicator();
		QueryCategoryService service = new QueryCategoryService(mContext);
		service.setService(url, this, "onQueryCity2Result");
		service.request();
	}
	
	public void onQueryCity2Result(String url, String html, AjaxStatus ajaxStatus) {
		hideLoadingIndicator();
		if (html == null) {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_network_unavailable), Toast.LENGTH_SHORT).show();
			return;
		}
		
		Element city2Element = Jsoup.parse(html);
		Elements options = city2Element.select("#city2 option");
		ArrayList<String> city2List = new ArrayList<String>();
		for (int i=0; i<options.size(); i++) {
			String value = options.get(i).attr("value");
			if (value.equals("")) {
				value = "시/구/군";
			}
			city2List.add(value);
		}
		city2Adapter.clear();
		city2Adapter.addAll(city2List);
		city2Adapter.notifyDataSetChanged();
		
		// 글 수정일 경우 이전 글 도시정보 등록
		if (requestCode == 1 && city2 != null) {
			ArrayAdapter city2Adapter = (ArrayAdapter)spinnerCity2.getAdapter();
			spinnerCity2.setSelection(city2Adapter.getPosition(city2));
		}
	}
	
	public void showLoadingIndicator() {
		if (mLoadingLayout != null) {
			mLoadingLayout.setVisibility(View.VISIBLE);
		}
	}
	
	public void hideLoadingIndicator() {
		if (mLoadingLayout != null) {
			mLoadingLayout.setVisibility(View.INVISIBLE);
		}
	}
	
	private void setSpinnerVisibility() {
		// spinner visible 처리
		if (("house").equals(menuId)) {
			spinnerCate2.setVisibility(View.VISIBLE);
		} else {
			spinnerCate2.setVisibility(View.INVISIBLE);
		}
	}
	
	private MenuData getTestMenuData() {
		MenuData menuData = new MenuData();
		menuData.setId("resume");
		menuData.setName("구직정보");
		menuData.setUrl("board.php?bo_mode=list&bo_table=resume");
		return menuData;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		if (parent == spinnerCity1) {
			if (position == 0) {
				city2Adapter.clear();
				city2Adapter.addAll(city2Default);
				city2Adapter.notifyDataSetChanged();
			} else {
				queryCity2((String)spinnerCity1.getSelectedItem());
			}
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}
	
	/**
	 * 글등록 성공후 redirect url로 query후 파싱한다.
	 * ../board.php?bo_mode=view&bo_table=resume&id=413&city1=%EA%B2%BD%EA%B8%B0
	 * @param respUrl
	 */
	private void saveMyFavoritePost(String respUrl) {
		showLoadingIndicator();
		String url = respUrl.replace("../", Links.DOMAIN_URL);
		QueryDetailPageService service = new QueryDetailPageService(mContext);
		service.setService(url, this, "onRedirectResult");
		service.request();
	}
	
	public void onRedirectResult(String url, String html, AjaxStatus ajaxStatus) {
		hideLoadingIndicator();
		if (html == null) {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_network_unavailable), Toast.LENGTH_SHORT).show();
			return;
		}
		Element body = Jsoup.parse(html).body();
        Elements contElm = body.select("div.layout");
        if (contElm.size() > 0) {
        	Element element = contElm.get(contElm.size()-1);
        	String title = element.select("div.article-title h1").text();
            String category = title.substring(title.lastIndexOf('-')+2);
            category = category.replace("(", "").replace(")", "");
            String city = category.split("\\s{1}")[0];
            String postId = Util.getValueFromUrl(url, "id");
            title = title.substring(0, title.lastIndexOf('-'));
            
            String content = null;
            Elements conts = element.select("div.article-detail div.content");
        	if (conts.size() > 0) {
        		Element cont = conts.get(0);
        		content = ParseUtil.extractPlainTextFromHtmlElement(cont);
        	}
            
            DataHashMap map = new DataHashMap();
			map.put("postId", postId);
			map.put("title", title);
			map.put("category", category);
			map.put("city", city);
			map.put("link", url);
			map.put("content", content);
			
            Elements metaElms = element.select("ul.article-meta li");
            if (metaElms.size()>2) {
            	String[] dateTmps = metaElms.get(0).text().split("[\\s]");
                String date = dateTmps[1];
                map.put("date", date);
            }
            
            PostStateManager.getInstance(mContext).addMyPost(postId, map);
            PostStateManager.getInstance(mContext).getFavoriteList().add(postId);
            PostStateManager.getInstance(mContext).saveMyPost();
        }
	}
}

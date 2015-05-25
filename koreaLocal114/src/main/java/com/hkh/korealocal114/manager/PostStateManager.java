package com.hkh.korealocal114.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import com.hkh.korealocal114.data.DataHashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * 기능1: 방문한 게시글 저장기능. 2차원 Set으로 관리
 * [메뉴Set<게시글Set<게시글ID String>>]
 * 
 * 기능2: 즐겨찾기 메뉴 관리기능
 * @author hkh
 *
 */
public class PostStateManager {

	private static String TAG = PostStateManager.class.getSimpleName();
	private static Context mContext;
	private static PostStateManager instance = null;
	private HashMap<String, ArrayList<String>> pageState = null;
	private List<String> favoriteList;
	private List<String> myPostList;
	private SharedPreferences spf = null;
	
	private PostStateManager() {
		pageState = new HashMap<String, ArrayList<String>>();
		favoriteList = new ArrayList<String>();
		myPostList = new ArrayList<String>();
		spf = mContext.getSharedPreferences("pref", Context.MODE_PRIVATE);
		restorePostState();
	}
	public static PostStateManager getInstance(Context context) {
		if (instance == null) {
			mContext = context;
			instance = new PostStateManager();
		}
		return instance;
	}
	
	/**
	 * 메뉴별 방문한 페이지 조회 [기능1]
	 * @param menuName
	 * @return
	 */
	public ArrayList<String> getVisitedList(String menuName) {
		ArrayList<String> list = pageState.get("key_"+menuName);
		if (list == null) {
			list = new ArrayList<String>();
			pageState.put("key_"+menuName, list);
		}
		return list;
	}
	
	/**
	 * SharedPreference 로 방문한 게시글 restore [기능1]
	 */
	private void restorePostState() {
		// restore key set first
		Set<String> keySet = spf.getStringSet("page_keyset", null);
		if (keySet != null) {
			for (Iterator<String> iterator=keySet.iterator();iterator.hasNext();) {
				String key = iterator.next();
				Set<String> set = spf.getStringSet(key, null);
				ArrayList<String> list = new ArrayList<String>();
				list.addAll(set);
				pageState.put(key, list);
			}
		}
	}
	
	/**
	 * SharedPreference 에 방문한 게시글 저장 [기능1] - 앱이 pause상태로 진입시 호출됨
	 */
	public void savePostState() {
		Set<String> keySet = pageState.keySet();
		// key set 저장
		spf.edit().putStringSet("page_keyset", keySet).commit();
		
		// 메뉴별 set저장
		for (Iterator<String> iterator=keySet.iterator();iterator.hasNext();) {
			String key = iterator.next();
			ArrayList<String> list = pageState.get(key);
			putSet2SharedPreference(key, list);
		}
	}
	
	private void putSet2SharedPreference(String key, ArrayList<String> list) {
		Set<String> set = new HashSet<String>();
		set.addAll(list);
		spf.edit().putStringSet(key, set).commit();
	}
	
	public List<String> restoreMyPost() {
		Set<String> menuSet = spf.getStringSet("mypost_keyset", null);
		if (menuSet == null) {
			return null;
		}
		myPostList.clear();
		for (String postId : menuSet) {
			try {
				myPostList.add(postId);
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}
		return myPostList;
	}
	
	/**
	 * 내가쓴글 저장 [기능2] - 앱이 pause상태로 진입시 호출됨
	 */
	public void saveMyPost() {
		Set<String> menuSet = new HashSet<String>();
		for (String postId : myPostList) {
			menuSet.add(postId);
		}
		spf.edit().putStringSet("mypost_keyset", menuSet).commit();
	}
	
	/**
	 * 내가쓴글을 임시저장목록에 추가
	 * 추가성공하면 true, 중복된 추가이면 false
	 * @param postId
	 * @return
	 */
	public boolean addMyPost(String postId, DataHashMap data) {
		if (!myPostList.contains(postId)) {
			myPostList.add(postId);
			
			JSONObject json = new JSONObject(data);
			spf.edit().putString("my_"+postId, json.toString()).commit();
			return true;
		}
		return false;
	}
	
	/**
	 * PostId 를 임시저장목록에서 숨기기 (최대10개 저장)
	 * @param postId
	 * @return
	 */
	public boolean removeMyPost(String postId) {
		if (myPostList.contains(postId)) {
			myPostList.remove(postId);
			spf.edit().remove("my_"+postId).commit();
			return true;
		}
		return false;
	}
	
	/**
	 * 내가쓴글 목록 조회
	 * @return
	 */
	public ArrayList<DataHashMap> getMyPostList() {
		if (myPostList == null)
			return null;
		
		Gson gson = new Gson();
		ArrayList<DataHashMap> list = new ArrayList<DataHashMap>();
		for (String postId : myPostList) {
			String jsonStr = spf.getString("my_"+postId, null);
			DataHashMap data = gson.fromJson(jsonStr, DataHashMap.class);
			list.add(data);
		}
		return list;
	}
	
	/**
	 * 저장된 즐겨찾기 Post 생성
	 */
	public List<String> restoreFavoritePost() {
		Set<String> menuSet = spf.getStringSet("favorite_keyset", null);
		if (menuSet == null) {
			return null;
		}
		favoriteList.clear();
		for (String postId : menuSet) {
			try {
				favoriteList.add(postId);
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}
		return favoriteList;
	}
	
	/**
	 * 즐겨찾기 Post저장 [기능2] - 앱이 pause상태로 진입시 호출됨
	 */
	public void saveFavoritePost() {
		Set<String> menuSet = new HashSet<String>();
		for (String postId : favoriteList) {
			menuSet.add(postId);
		}
		spf.edit().putStringSet("favorite_keyset", menuSet).commit();
	}
	
	public List<String> getFavoriteList() {
		return favoriteList;
	}
	
	/**
	 * PostId를 임시저장목록에 추가
	 * @param postId
	 * @return
	 */
	public List<String> addFavoritePost(String postId, DataHashMap data) {
		if (!favoriteList.contains(postId)) {
			favoriteList.add(postId);
			
			JSONObject json = new JSONObject(data);
			spf.edit().putString("post_"+postId, json.toString()).commit();
		}
		return favoriteList;
	}
	
	public ArrayList<DataHashMap> getFavoritePostList() {
		if (favoriteList == null)
			return null;
		
		Gson gson = new Gson();
		ArrayList<DataHashMap> list = new ArrayList<DataHashMap>();
		for (String postId : favoriteList) {
			String jsonStr = spf.getString("post_"+postId, null);
			DataHashMap data = gson.fromJson(jsonStr, DataHashMap.class);
			list.add(data);
		}
		return list;
	}
	
	/**
	 * PostId 를 임시저장목록에서 삭제
	 * @param postId
	 * @return
	 */
	public List<String> removeFavoritePost(String postId) {
		if (favoriteList.contains(postId)) {
			favoriteList.remove(postId);
			spf.edit().remove("post_"+postId).commit();
		}
		return favoriteList;
	}
}

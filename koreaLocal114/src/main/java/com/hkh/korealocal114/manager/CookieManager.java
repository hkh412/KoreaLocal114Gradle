package com.hkh.korealocal114.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import android.content.Context;
import android.content.SharedPreferences;

public class CookieManager {

	private List<Cookie> cookies = null;
	private static CookieManager instance = null;
	private SharedPreferences spf = null;
	Context mContext = null;
	
	private CookieManager(Context context) {
		mContext = context;
		spf = mContext.getSharedPreferences("pref", Context.MODE_PRIVATE);
		cookies = restoreCookies();
	}
	
	public static CookieManager getInstance(Context context) {
		if (instance == null) {
			instance = new CookieManager(context);
		}
		return instance;
	}
	
	public void setCookies(List<Cookie> cookies) {
		this.cookies = cookies;
		saveCookies();
	}
	
	public List<Cookie> getCookies() {
		return cookies;
	}
	
	public boolean hasCookies() {
		return cookies != null;
	}
	
	public String getCookieSSID() {
		if (!hasCookies())
			return "";
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals("114_ssid")) {
				return cookie.getValue();
			}
		}
		return "";
	}
	
	private List<Cookie> restoreCookies() {
		Set<String> cookieSet = spf.getStringSet("cookie_set", null);
		if (cookieSet == null) {
			return null;
		}
		if (cookies == null) {
			cookies = new ArrayList<Cookie>();
		}
		for (String str : cookieSet) {
			String[] strs = str.split("[:]");
			if (strs.length >= 2) {
				Cookie cookie = new BasicClientCookie(strs[0], strs[1]);
				cookies.add(cookie);
			}
		}
		return cookies;
	}
	
	private void saveCookies() {
		Set<String> cookieSet = new HashSet<String>();
		for (Cookie cookie : cookies) {
			cookieSet.add(cookie.getName()+":"+cookie.getValue());
		}
		spf.edit().putStringSet("cookie_set", cookieSet).commit();
	}
	
}

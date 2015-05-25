package com.hkh.korealocal114.service;

import java.util.List;
import java.util.Map;

import org.apache.http.cookie.Cookie;

import android.content.Context;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.hkh.korealocal114.manager.CookieManager;

public abstract class ServiceBase {

	Context mContext;
	AjaxCallback<String> cb;
	
	public ServiceBase(Context context) {
		mContext = context;
		cb = new AjaxCallback<String>();
		setCookies(CookieManager.getInstance(mContext).getCookies());
//		setTestCookies();
	}
	
	private void setTestCookies() {
		cb.cookie("114_ssid", "141612811976");
		cb.cookie("114_city", "%EC%A0%84%EA%B5%AD");
	}
	
	/**
	 * 서비스 설정
	 * @param url 
	 * @param handler - callback 메서드를 포함한 클래스 인스턴스
	 * @param handlerName - callback 메서드명
	 */
	public void setService(String url, Object handler, String handlerName) {
		cb.url(url).type(String.class).weakHandler(handler, handlerName);
	}
	
	/**
	 * http post body params
	 * @param params
	 */
	public void setParams(Map<String, Object> params) {
		cb.params(params);
	}
	
	/**
	 * http request headers
	 * @param headers
	 */
	public void setHeaders(Map<String, String> headers) {
		if (headers != null) {
			cb.headers(headers);
		}
	}
	
	/**
	 * http set request cookies
	 * @param cookies
	 */
	public void setCookies(List<Cookie> cookies) {
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				cb.cookie(cookie.getName(), cookie.getValue());
			}
		}
	}
	
	/**
	 * http request 실행
	 */
	public void request() {
		if (cb != null) {
			AQuery aq = new AQuery(mContext);
			aq.ajax(cb);
		}
	};
}

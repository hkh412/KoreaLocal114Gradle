package com.hkh.korealocal114.data;

import java.io.Serializable;

public class MenuData implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	int uid;
	String id;			// 메뉴 아이디
	String name;
	String url;
	String type;
	
	public String getId() {
		return this.id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getUid() {
		return uid;
	}
	public void setUid(int uid) {
		this.uid = uid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
}

package com.hkh.korealocal114.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import android.text.Html;

import com.hkh.korealocal114.config.HTMLTags;
import com.hkh.korealocal114.config.Links;
import com.hkh.korealocal114.data.DataHashMap;

public class ParseUtil {
	String TAG = ParseUtil.class.getSimpleName();

	/**
	 * 일반 게시판 파싱
	 * @param rows
	 * @return
	 */
	public static ArrayList<DataHashMap> parseBoardElements(List<Element> rows) {
		ArrayList<DataHashMap> list = new ArrayList<DataHashMap>();
		DataHashMap map = null;
		for (Iterator<Element> iterator=rows.iterator(); iterator.hasNext();) {
			Element row = iterator.next();
			map = new DataHashMap();
			
			String category = row.select("p.title em").text();
			String title = row.select("p.title a").text();
			String date = row.select("span.time").text();
			String link = row.select("p.title a").attr("href");
			String city = category.split("\\s{1}")[0].replace("(", "");
			String postId = Util.getValueFromUrl(link, "id");
			
			map.put("postId", postId);
			map.put("title", title);
			map.put("category", category);
			map.put("city", city);
			map.put("date", date);
			map.put("link", Links.DOMAIN_URL+link);
			list.add(map);
		}
		return list;
	}
	
	public static int mergeList(ArrayList<DataHashMap> data, ArrayList<DataHashMap> newList) {
		int duplicateCnt = 0;
		for (Iterator<DataHashMap> iterator = newList.iterator();iterator.hasNext();) {
			DataHashMap map = iterator.next();
			String postId = map.get("postId");
			boolean duplicate = checkContain(data, postId);
			if (duplicate) {
				duplicateCnt++;
			} else {
				data.add(map);
			}
		}
		return duplicateCnt;
	}
	
	public static boolean checkContain(ArrayList<DataHashMap> data, String postId) {
		boolean contain = false;
		for (DataHashMap map : data) {
			if (map.get("postId")!=null && map.get("postId").equals(postId)) {
				contain = true;
				break;
			}
		}
		return contain;
	}
	
	public static String extractPlainTextFromHtmlElement(Element element) {
		String plainText = null;
		plainText = Html.fromHtml(element.html()).toString();
		return plainText;
	}
}

package com.hkh.korealocal114.config;

public class Config {

	public static int REQUEST_CODE_NEW = 0;
	public static int REQUEST_CODE_EDIT = 1;
	
	/**
	 *  Log 메시지 출력 여부
	 */
	public static boolean LogEnable = false;
	
	/**
	 * 광고뷰 테스트모드
	 */
	public static boolean ADVIEW_TEST = false;
	
	/**
	 * 광고 로딩여부
	 */
	public static boolean AdEnable = true;
	public static boolean AdInterstitialEnable = false;
	
	/**
	 * 방문한 게시글 저장 리스트의 크기
	 */
	public static int LIST_LIMIT = 100;
	
	/**
	 * AD_BUDDIZ THRESHOLD - MainActivity가 10번 실행된 경우 Adbuddiz 전면광고가 노출된다.
	 */
	public static int AD_THRESHOLD = 5;
	
	public static final String USER_AGENT = 
			"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36"+
			" (KHTML, like Gecko) Chrome/38.0.2125.111 Safari/537.36";
}

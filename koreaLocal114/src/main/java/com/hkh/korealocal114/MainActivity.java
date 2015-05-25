package com.hkh.korealocal114;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.hkh.korealocal114.KoreaLocal114Application.TrackerName;
import com.hkh.korealocal114.adapter.ActionBarDropDownAdapter;
import com.hkh.korealocal114.adapter.DrawerListAdapter;
import com.hkh.korealocal114.adapter.ListDetailPagerAdapter;
import com.hkh.korealocal114.config.Config;
import com.hkh.korealocal114.data.MenuData;
import com.hkh.korealocal114.data.Region;
import com.hkh.korealocal114.fragments.BoardFragment;
import com.hkh.korealocal114.fragments.LocalCacheFragment;
import com.hkh.korealocal114.manager.PostStateManager;
import com.hkh.korealocal114.manager.SharedPreferenceManager;
import com.hkh.korealocal114.util.Util;
import com.purplebrain.adbuddiz.sdk.AdBuddiz;

/**
 * App: Portal 114114.com
 * @author hkh
 *
 */
public class MainActivity extends PagerActivity 
	implements OnNavigationListener, OnItemClickListener {

	private static final String TAG = MainActivity.class.getSimpleName();
	Context mContext;
	
	/**
	 * 좌측 메뉴 open / close 이벤트 핸들러
	 */
	ActionBarDrawerToggle mDrawerToggle;
	
	/**
	 * 좌측 메뉴 layout
	 */
	DrawerLayout mDrawerLayout;
	
	ListView mDrawerList;
	
	DrawerListAdapter mDrawerListAdapter;
	
	ActionBarDropDownAdapter mActionBarListAdapter;
	
	ActionBar mActionBar;
	
	/**
	 * 메뉴 데이터
	 */
	ArrayList<MenuData> menuList = new ArrayList<MenuData>();
	
	/**
	 * 지역정보 데이터
	 */
	ArrayList<Region> regionList = new ArrayList<Region>();
	
	/**
	 * 왼쪽 슬라이드 메뉴관련
	 */
	LinearLayout mLeftDrawer;
	
	/**
	 * 메뉴정보 데이터 menu.json
	 */
	MenuData selectedMenuData = null;
	
	/**
	 * 지역정보 데이터 region.json
	 */
	Region selectedRegion = null;
	
	/**
	 * 최초 로딩
	 */
	boolean initialMenuOpen = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_main);
		
		mLoadingLayout = (LinearLayout) findViewById(R.id.layout_indicator);
		
		// 메뉴 데이터 로딩
		menuList = Util.loadMenuData(mContext);
		
		// 지역정보 로딩
		regionList = Util.loadRegionData(mContext);
		
		PostStateManager.getInstance(mContext).restoreFavoritePost();
		PostStateManager.getInstance(mContext).restoreMyPost();
		
		selectedRegion = regionList.get(0);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
			@Override
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);
				invalidateOptionsMenu();
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
//				hideSoftKeyboard();
				invalidateOptionsMenu();
			}
		};
		mDrawerToggle.setDrawerIndicatorEnabled(true);
		mDrawerLayout.setDrawerListener(mDrawerToggle);
        
		mLeftDrawer = (LinearLayout) findViewById(R.id.left_drawer);
		
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setOnPageChangeListener(this);
		
		mDrawerList = (ListView) findViewById(R.id.drawer_list);
		mDrawerListAdapter = new DrawerListAdapter(mContext, R.layout.drawer_item, menuList);
		mDrawerList.setAdapter(mDrawerListAdapter);
		
		mDrawerList.setOnItemClickListener(this);
		
		setActionBar();
		setAdView();
		sendAnalytics();
		
		int lastUid = SharedPreferenceManager.getInstance(mContext).getInt("last-menu-uid");
		MenuData lastMenuData = null;
		if (lastUid < 0) {
			// 저장된 메뉴 없음, 구인정보
			lastMenuData = Util.getMatchedUrlDataByUid(menuList, 3);
		} else {
			lastMenuData = Util.getMatchedUrlDataByUid(menuList, lastUid);
		}
		loadFragment(lastMenuData);
		selectedMenuData = lastMenuData;
	}
	
	@Override
	public void setAdView() {
		if (!Config.AdEnable) {
			return;
		}
		
		mAdView = (AdView) findViewById(R.id.adView);
        mAdView.setAdListener(new ToastAdListener(this));

        String deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID); 
        Log.d(TAG, "DeviceId: "+deviceId);
        
        AdRequest.Builder builder = new AdRequest.Builder();
        if (Config.ADVIEW_TEST) {
        	builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
        	.addTestDevice(deviceId);
        }
        adRequest = builder.build();
        mAdView.loadAd(adRequest);
        
        // AD provider - adbuddiz.com
        SharedPreferenceManager spm = SharedPreferenceManager.getInstance(mContext);
        int viewCnt = spm.getInt("view_count");
        if (viewCnt >= Config.AD_THRESHOLD) {
        	spm.putInt("view_count", 0);
            AdBuddiz.setPublisherKey(mContext.getString(R.string.adbuddiz_pub_key));
            AdBuddiz.cacheAds((Activity)mContext);
            AdBuddiz.showAd(this);
        } else {
        	viewCnt++;
        	spm.putInt("view_count", viewCnt);
        }
	}
	
	private void setActionBar() {
		mActionBar = getActionBar();
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setHomeButtonEnabled(true);
//		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		mActionBarListAdapter = new ActionBarDropDownAdapter(mContext,
				android.R.layout.simple_spinner_dropdown_item, regionList);
		mActionBar.setListNavigationCallbacks(mActionBarListAdapter, this);
	}
	
	private void sendAnalytics() {
        Tracker t = ((KoreaLocal114Application) getApplication()).getTracker(
                TrackerName.APP_TRACKER);
        t.setScreenName(TAG);
        t.send(new HitBuilders.AppViewBuilder().build());
	}
	
	private void loadFragment(MenuData menuData) {
		// 액션바 타이틀 변경
		mActionBar.setTitle(menuData.getName());
		Fragment fragment = null;
		Bundle bundle = new Bundle();
		if (menuData.getUid() == 1) {
			// 즐겨찾기
			fragment = new LocalCacheFragment();
			bundle.putString("city", selectedRegion.getParam());
			bundle.putString("type", "favorite");
			
		} else if (menuData.getUid() == 2) {
			// 내가 쓴글
			fragment = new LocalCacheFragment();
			bundle.putString("city", selectedRegion.getParam());
			bundle.putString("type", "mypost");
			
		} else {
			fragment = new BoardFragment();
			
			String url = menuData.getUrl();
			bundle.putString("url", url+"&"+selectedRegion.getParam());
		}
		fragment.setArguments(bundle);
		
		mListDetailAdapter = new ListDetailPagerAdapter(getSupportFragmentManager(), fragment);
		mViewPager.setAdapter(mListDetailAdapter);
		mViewPager.setOffscreenPageLimit(mListDetailAdapter.getCount());
	}
	
	/**
	 * 처음 게시글목록 로딩완료후 side menu 열림
	 */
	public void openDrawerList() {
		if (!initialMenuOpen) {
			TimerTask timerTask = new TimerTask() {
				@Override
				public void run() {
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							mDrawerLayout.openDrawer(mLeftDrawer);
						}
					});
				}
			};
			Timer timer = new Timer();
			timer.schedule(timerTask, 300);
			initialMenuOpen = true;
		}
	}
	
	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/**
		 * The following 3 lines should be added (추가안하면 홈버튼 클릭했을 때 NavigationDrawer가 열리지 않음)
		 */
		if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
		
		int id = item.getItemId();
		if (id == R.id.action_write) {
			if (selectedMenuData.getId().equals("mypost") 
					|| selectedMenuData.getId().equals("favorite")) {
				// 해당 게시판에서만 글등록이 가능함
				Toast.makeText(mContext, mContext.getString(R.string.message_write_post_alert),
						Toast.LENGTH_LONG).show();
				return true;
			}
			Intent intent = new Intent(mContext, WriteActivity.class);
			Bundle bundle = new Bundle();
			bundle.putSerializable("menu_data", selectedMenuData);
			bundle.putInt("requestCode", Config.REQUEST_CODE_NEW);
			intent.putExtras(bundle);
			startActivityForResult(intent, Config.REQUEST_CODE_NEW);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Config.REQUEST_CODE_NEW) {
			if (resultCode == RESULT_OK) {
				loadFragment(selectedMenuData);
			}
		}
	}

	@Override
	public void onShowBackButtonToast() {
		Toast.makeText(mContext, mContext.getString(R.string.message_back_button_toast),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onNavigationItemSelected(int position, long itemId) {
		selectedRegion = mActionBarListAdapter.getItem(position);
		mDrawerLayout.closeDrawer(mLeftDrawer);
		loadFragment(selectedMenuData);
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
		mDrawerLayout.closeDrawer(mLeftDrawer);
		mBackBtnClickCount = 0;
		mViewPager.setAdapter(null);
		
		// 선택한 메뉴 인덱스 저장
		MenuData menuData = mDrawerListAdapter.getItem(position);
		
		// 선택한 메뉴 저장
		SharedPreferenceManager.getInstance(mContext).putInt("last-menu-uid", menuData.getUid());
		
		loadFragment(menuData);
		selectedMenuData = menuData;
	}
}

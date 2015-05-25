package com.hkh.korealocal114;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.hkh.korealocal114.adapter.ListDetailPagerAdapter;
import com.hkh.korealocal114.config.Config;
import com.hkh.korealocal114.fragments.BoardDetailFragment;
import com.hkh.korealocal114.fragments.BoardFragment;
import com.hkh.korealocal114.fragments.LocalCacheFragment;
import com.hkh.korealocal114.fragments.StateFragment;

public abstract class PagerActivity extends ActionBarActivity
	implements OnPageChangeListener {

	private static String TAG = PagerActivity.class.getSimpleName();
	Context mContext;
	ViewPager mViewPager;
	ListDetailPagerAdapter mListDetailAdapter;
	LinearLayout mLoadingLayout;
	
	AdRequest adRequest;
	AdView mAdView;
	
protected int mBackBtnClickCount = 0;
	
	/**
	 * n 번만큼 back button을 클릭하면 activity 종료함
	 */
	protected int backCountForFinish = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = getApplicationContext();
	}
	
	public void setDetailFragment(Fragment detailFragment) {
		mViewPager.setCurrentItem(1);
		mListDetailAdapter.setDetailFragment(detailFragment);
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
	
	/**
	 * 게시글 목록으로 이동
	 */
	public void redirectToList() {
		if (mViewPager != null) {
			if (mViewPager.getCurrentItem() == 1) {		// 게시글 상세
				mViewPager.setCurrentItem(0);
				mBackBtnClickCount = 0;
				return;
			}
		}
	}
	
	/**
	 * back 키 기본 동작 overriding
	 */
	@Override
	public void onBackPressed() {
		if (mViewPager != null) {
			if (mViewPager.getCurrentItem() == 1) {		// 게시글 상세
				mViewPager.setCurrentItem(0);
				mBackBtnClickCount = 0;
				return;
			}
			if (mViewPager.getCurrentItem() == 0) {		// 게시글 목록
				onShowBackButtonToast();
				if (mBackBtnClickCount >= backCountForFinish) {
					finish();
				}
				mBackBtnClickCount++;
				return;
			}
		}
		super.onBackPressed();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mAdView != null) {
			mAdView.destroy();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mAdView != null) {
			mAdView.pause();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdView != null) {
			mAdView.resume();
		}
	}
	
	@Override
	public void onPageScrollStateChanged(int state) {
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		if (position == 0) {
			showAdView();
		} else if (position == 1) {
//			hideAdView();
		}
		notifyPageActivated(position);
	}
	
	private void notifyPageActivated(int position) {
		Fragment fragment = mListDetailAdapter.getItem(position);
		if (fragment == null) {
			return;
		}
		if (fragment instanceof StateFragment) {
			((StateFragment)fragment).notifyPageActivated();
		}
	}
	
	public void notifyFavoriteChange() {
		if (mViewPager == null) {
			return;
		}
		if (mListDetailAdapter == null) {
			return;
		}
		if (mListDetailAdapter.getCount()<1) {
			return;
		}
		
		Fragment fragment = mListDetailAdapter.getItem(0);
		if (fragment == null) {
			return;
		}
		if (fragment instanceof BoardFragment) {
			((BoardFragment)fragment).notifyFavoriteChange();
		} else if (fragment instanceof LocalCacheFragment) {
			((LocalCacheFragment)fragment).notifyFavoriteChange();
		}
		
		Fragment detail= mListDetailAdapter.getItem(1);
		if (detail != null && detail instanceof BoardDetailFragment) {
			((BoardDetailFragment)detail).notifyFavoriteChange();
		}
	}
	
	public void setAdView() {
	}
	
	public void showAdView() {
		if (mAdView != null) {
			mAdView.setVisibility(View.VISIBLE);
		}
	}

	public void hideAdView() {
		if (mAdView != null) {
			mAdView.setVisibility(View.GONE);
		}
	}
	
	/**
	 * back 버튼 연속클릭시 보여줄 메시지
	 */
	abstract public void onShowBackButtonToast();
}

package com.hkh.korealocal114;

import java.util.HashMap;

import com.androidquery.callback.AjaxCallback;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.hkh.korealocal114.config.Config;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.splunk.mint.Mint;

import android.app.Application;
import android.content.Context;

public class KoreaLocal114Application extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		initImageLoader(getApplicationContext());
		AjaxCallback.setAgent(Config.USER_AGENT);
		Mint.initAndStartSession(this, "e7e551f1");
	}

	// The following line should be changed to include the correct property id.
    private static final String PROPERTY_ID = "UA-51659012-7";

    public static int GENERAL_TRACKER = 0;

    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();
    
    synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = analytics.newTracker(PROPERTY_ID);
            mTrackers.put(trackerId, t);

        }
        return mTrackers.get(trackerId);
    }
    
	/**
	 * 이미지 로더를 사용하기 위해 Config 설정
	 * @param context
	 */
	public static void initImageLoader(Context context) {
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
				.threadPriority(Thread.NORM_PRIORITY - 2)
				.threadPoolSize(3)
				.denyCacheImageMultipleSizesInMemory()
				.discCacheFileNameGenerator(new Md5FileNameGenerator())
				.tasksProcessingOrder(QueueProcessingType.LIFO)
				.memoryCache(new LruMemoryCache(5*1024*1024))
//				.writeDebugLogs() // TODO 배포시에는 삭제
				.build();
		ImageLoader.getInstance().init(config);
	}
}

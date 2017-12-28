package timeline.lizimumu.com.t.data;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timeline.lizimumu.com.t.database.DbExecutor;
import timeline.lizimumu.com.t.database.IgnoreItem;
import timeline.lizimumu.com.t.stat.StatEnum;
import timeline.lizimumu.com.t.stat.StatManager;
import timeline.lizimumu.com.t.util.AppUtil;
import timeline.lizimumu.com.t.util.PreferenceManager;
import timeline.lizimumu.com.t.util.SortEnum;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * DataManager
 * Created by zb on 18/12/2017.
 */

public class DataManager {

    private static DataManager mInstance;

    public static void init() {
        mInstance = new DataManager();
    }

    public static DataManager getInstance() {
        return mInstance;
    }

    public void requestPermission(Context context) {
        Intent intent = new Intent(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public boolean hasPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps != null) {
            int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        }
        return false;
    }

    public List<AppItem> getTargetAppTimeline(Context context, String target, int offset) {
        List<AppItem> items = new ArrayList<>();
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        PackageManager packageManager = context.getPackageManager();
        if (manager != null) {
            SortEnum sortRange = SortEnum.getSortEnum(offset);
            long[] range = AppUtil.getTimeRange(sortRange);
            List<UsageStats> stats = manager.queryUsageStats(sortRange.getValue(), range[0] - 1, range[1]);
            for (UsageStats u : stats) {
                if (!u.getPackageName().equals(target)) continue;
                logEvent(packageManager, u);
                if (u.getFirstTimeStamp() < range[0]) continue;
                AppItem item = new AppItem();
                item.mPackageName = u.getPackageName();
                item.mFirstUsedTime = u.getFirstTimeStamp();
                item.mLastUsedTime = u.getLastTimeStamp();
                item.mTotalForTime = u.getTotalTimeInForeground();
                items.add(item);
            }
        }
        return items;
    }

    private void logEvent(PackageManager packageManager, UsageStats u) {
        Log.d("--------",  AppUtil.parsePackageName(packageManager, u.getPackageName()) + " " +
                u.getPackageName() + " " +
                new SimpleDateFormat("yyyy.MM.dd · HH:mm:ss", Locale.getDefault()).format(new Date(u.getFirstTimeStamp())) + " " +
                new SimpleDateFormat("yyyy.MM.dd · HH:mm:ss", Locale.getDefault()).format(new Date(u.getLastTimeStamp())) + " " +
                u.getLastTimeUsed() + " " +
                u.getTotalTimeInForeground() + " " +
                u.describeContents());
    }

    public List<AppItem> getApps(Context context, int sort, int offset) {

        List<AppItem> items = new ArrayList<>();
        List<AppItem> newList = new ArrayList<>();
        Map<String, Long> mCacheTime = new HashMap<>();
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        PackageManager packageManager = context.getPackageManager();
        if (manager != null) {
            SortEnum sortRange = SortEnum.getSortEnum(offset);
            long[] range = AppUtil.getTimeRange(sortRange);
            List<UsageStats> stats = manager.queryUsageStats(sortRange.getValue(), range[0] - 1, range[1]);
            for (UsageStats u : stats) {
                if (u.getFirstTimeStamp() < range[0]) continue;
                boolean exists = mCacheTime.containsKey(u.getPackageName());
                AppItem item;
                if (!exists) {
                    mCacheTime.put(u.getPackageName(), u.getTotalTimeInForeground());
                    item = new AppItem();
                } else {
                    mCacheTime.put(u.getPackageName(), mCacheTime.get(u.getPackageName()) + u.getTotalTimeInForeground());
                    item = containItem(items, u.getPackageName());
                }
                if (null != item) {
                    item.mPackageName = u.getPackageName();
                    item.mLastUsedTime = u.getLastTimeStamp();
                    item.mFirstUsedTime = u.getFirstTimeStamp();
                    if (!exists) items.add(item);
                    logEvent(packageManager, u);
                }
            }
        }
        // 按照使用时长排序
        if (items.size() > 0) {
            boolean valid = false;
            Map<String, Long> mobileData = new HashMap<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                valid = true;
                NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                mobileData = getMobileData(context, telephonyManager, networkStatsManager, offset);
            }

            boolean hideSystem = PreferenceManager.getInstance().getBoolean(PreferenceManager.PREF_SETTINGS_HIDE_SYSTEM_APPS);
            boolean hideUninstall = PreferenceManager.getInstance().getBoolean(PreferenceManager.PREF_SETTINGS_HIDE_UNINSTALL_APPS);
            List<IgnoreItem> ignoreItems = DbExecutor.getInstance().getAllItems();
            for (AppItem item : items) {
                if (!AppUtil.openable(packageManager, item.mPackageName)) {
                    continue;
                }
                if (hideSystem && AppUtil.isSystemApp(packageManager, item.mPackageName)) {
                    continue;
                }
                if (hideUninstall && !AppUtil.isInstalled(packageManager, item.mPackageName)) {
                    continue;
                }
                if (inIgnoreList(ignoreItems, item.mPackageName)) {
                    continue;
                }
                if (valid) {
                    String key = "u" + AppUtil.getAppUid(packageManager, item.mPackageName);
                    if (mobileData.size() > 0 && mobileData.containsKey(key)) {
                        item.mMobile = mobileData.get(key);
                    }
                }
                item.mTotalForTime = mCacheTime.get(item.mPackageName);
                item.mName = AppUtil.parsePackageName(packageManager, item.mPackageName);
                newList.add(item);
            }

            if (sort == 0) {
                Collections.sort(newList, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem left, AppItem right) {
                        return (int) (right.mTotalForTime - left.mTotalForTime);
                    }
                });
                Bundle params = new Bundle();
                params.putString("package_name", items.get(0).mPackageName);
                StatManager.getInstance().logEvent(StatEnum.KEY_TOP1, params);
            } else if (sort == 1) {
                Collections.sort(newList, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem left, AppItem right) {
                        return (int) (right.mLastUsedTime - left.mLastUsedTime);
                    }
                });
            } else if (sort == 2) {
                Collections.sort(newList, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem left, AppItem right) {
                        return (int)(right.mMobile - left.mMobile);
                    }
                });
            }
        }
        return newList;
    }

    private Map<String, Long> getMobileData(Context context, TelephonyManager tm, NetworkStatsManager nsm, int offset) {
        Map<String, Long> result = new HashMap<>();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            long[] range = AppUtil.getTimeRange(SortEnum.getSortEnum(offset));
            NetworkStats networkStatsM;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    networkStatsM = nsm.querySummary(ConnectivityManager.TYPE_MOBILE, tm.getSubscriberId(), range[0], range[1]);
                    if (networkStatsM != null) {
                        while (networkStatsM.hasNextBucket()) {
                            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
                            networkStatsM.getNextBucket(bucket);
                            String key = "u" + bucket.getUid();
                            if (result.containsKey(key)) {
                                result.put(key, result.get(key) + bucket.getTxBytes() + bucket.getRxBytes());
                            } else {
                                result.put(key, bucket.getTxBytes() + bucket.getRxBytes());
                            }
                        }
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private AppItem containItem(List<AppItem> items, String packageName) {
        for (AppItem item : items) {
            if (item.mPackageName.equals(packageName)) return item;
        }
        return null;
    }

    private boolean inIgnoreList(List<IgnoreItem> items, String packageName) {
        for (IgnoreItem item : items) {
            if (item.mPackageName.equals(packageName)) return true;
        }
        return false;
    }
}

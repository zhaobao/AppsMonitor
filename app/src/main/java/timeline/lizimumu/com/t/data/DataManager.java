package timeline.lizimumu.com.t.data;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timeline.lizimumu.com.t.AppConst;
import timeline.lizimumu.com.t.database.DbExecutor;
import timeline.lizimumu.com.t.database.IgnoreItem;
import timeline.lizimumu.com.t.util.AppUtil;
import timeline.lizimumu.com.t.util.PreferenceManager;

/**
 * DataManager
 * Created by zb on 18/12/2017.
 */

public class DataManager {

    private static Map<String, Map<String, Object>> mCacheData = new HashMap<>();

    public void requestPermission(Context context) {
        context.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    public boolean hasPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps != null) {
            int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        }
        return false;
    }

    public List<AppItem> getTargetAppTimeline(Context context, String target) {
        List<AppItem> items = new ArrayList<>();
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager != null) {
            long endTime = System.currentTimeMillis();
            long startTime = AppUtil.startOfDay(endTime);
            UsageEvents events = manager.queryEvents(startTime, endTime);
            UsageEvents.Event event = new UsageEvents.Event();
            long duration = 0;
            long offset = 0;
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                String packageName = event.getPackageName();
                if (target.equals(packageName)) {
                    AppItem item = new AppItem();
                    item.mPackageName = event.getPackageName();
                    item.mEventType = event.getEventType();
                    item.mEventTime = event.getTimeStamp();
                    if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        offset = item.mEventTime;
                        item.mUsageTime = 0;
                    } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                        if (offset > 0) {
                            duration = item.mEventTime - offset;
                            offset = 0;
                        }
                        item.mUsageTime = duration;
                    }
                    items.add(item);
                }
            }
        }
        return items;
    }

    public List<AppItem> getApps(Context context, int sort) {

        long start_time = System.currentTimeMillis();

        List<AppItem> items = new ArrayList<>();
        List<IgnoreItem> ignoreItems = DbExecutor.getInstance().getAllItems();
        Map<String, Long> offsets = new HashMap<>();
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        boolean hideSystem = PreferenceManager.getInstance().getBoolean(PreferenceManager.PREF_SETTINGS_HIDE_SYSTEM_APPS);
        boolean hideUninstall = PreferenceManager.getInstance().getBoolean(PreferenceManager.PREF_SETTINGS_HIDE_UNINSTALL_APPS);
        PackageManager packageManager = context.getPackageManager();
        if (manager != null) {
            long endTime = System.currentTimeMillis();
            long startTime = AppUtil.startOfDay(endTime);
            UsageEvents events = manager.queryEvents(startTime, endTime);
            UsageEvents.Event event = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                boolean exists = true;
                events.getNextEvent(event);
                String packageName = event.getPackageName();
                AppItem item = getItem(items, packageName);
                if (item == null) {
                    exists = false;
                    item = new AppItem();
                    item.mPackageName = event.getPackageName();
                    boolean installed;
                    if (!mCacheData.containsKey(item.mPackageName)) {
                        item.mName = AppUtil.parsePackageName(packageManager, item.mPackageName);
                        item.mIsSystem = AppUtil.isSystemApp(packageManager, item.mPackageName);
                        installed = AppUtil.isInstalled(packageManager, item.mPackageName);
                        Map<String, Object> innerMap = new HashMap<>();
                        innerMap.put("name", item.mName);
                        innerMap.put("is_system", item.mIsSystem);
                        innerMap.put("installed", installed);
                        mCacheData.put(item.mPackageName, innerMap);
                    } else {
                        item.mName = (String) mCacheData.get(item.mPackageName).get("name");
                        item.mIsSystem = (boolean) mCacheData.get(item.mPackageName).get("is_system");
                        installed = (boolean) mCacheData.get(item.mPackageName).get("installed");
                    }
                    if (hideSystem && item.mIsSystem) {
                        continue;
                    }
                    if (hideUninstall && !installed) {
                        continue;
                    }
                }
                item.mEventTime = event.getTimeStamp();
                item.mEventType = event.getEventType();
                if (item.mEventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    offsets.put(item.mPackageName, item.mEventTime);
                } else if (item.mEventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    if (offsets.containsKey(item.mPackageName)) {
                        long offset = offsets.get(item.mPackageName);
                        if (offset > 0) {
                            item.mUsageTime = item.mUsageTime + (item.mEventTime - offset);
                            offsets.put(item.mPackageName, 0L);
                            if (item.mEventTime - offset > AppConst.USAGE_TIME_MIX) item.mCount++;
                        }
                    }
                }
                if (!exists && !inIgnoreList(ignoreItems, item.mPackageName)) {
                    items.add(item);
                }
            }
        }

        long end_time = System.currentTimeMillis();
        Log.d(">>>>>>>|||||", String.valueOf((end_time - start_time) / 1000));

        // 按照使用时长排序
        if (items.size() > 0) {
            for (AppItem item : items) {
                Log.d(">>>>>>||||||", item.toString());
            }

            if (sort == 0) {
                Collections.sort(items, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem left, AppItem right) {
                        return (int) (right.mEventTime - left.mEventTime);
                    }
                });
            } else if (sort == 1) {
                Collections.sort(items, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem left, AppItem right) {
                        return (int) (right.mUsageTime - left.mUsageTime);
                    }
                });
            } else {
                Collections.sort(items, new Comparator<AppItem>() {
                    @Override
                    public int compare(AppItem left, AppItem right) {
                        return right.mCount - left.mCount;
                    }
                });
            }
        }

        offsets.clear();
        return items;
    }

    private AppItem getItem(List<AppItem> items, String packageName) {
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

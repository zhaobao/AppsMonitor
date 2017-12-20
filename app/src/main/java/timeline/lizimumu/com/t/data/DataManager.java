package timeline.lizimumu.com.t.data;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.text.TextUtils;
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

            AppItem item = new AppItem();
            item.mPackageName = target;
            item.mName = AppUtil.parsePackageName(context.getPackageManager(), target);

            long start = 0;
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                String currentPackage = event.getPackageName();
                int eventType = event.getEventType();
                long eventTime = event.getTimeStamp();
                if (currentPackage.equals(target)) {
                    if (eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) { // 事件开始
                        if (start == 0) {
                            start = eventTime;
                            item.mEventTime = eventTime;
                            item.mEventType = eventType;
                            item.mUsageTime = 0;
                            items.add(item.copy());
                        }
                    }
                } else { // 事件结束
                    if (start > 0) {
                        item.mUsageTime = eventTime - start;
                        if (item.mUsageTime > AppConst.USAGE_TIME_MIX) { // 本次使用大于5秒
                            item.mCount++;
                        }
                        item.mEventTime = eventTime;
                        item.mEventType = UsageEvents.Event.MOVE_TO_BACKGROUND;
                        items.add(item.copy());
                        start = 0;
                    }
                }
            }
        }
        return items;
    }

    public List<AppItem> getApps(Context context, int sort) {

        List<AppItem> items = new ArrayList<>();
        List<AppItem> newList = new ArrayList<>();
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager != null) {
            // 缓存变量
            String prevPackage = "";
            Map<String, Long> startPoints = new HashMap<>();
            // 获取事件
            long endTime = System.currentTimeMillis();
            long startTime = AppUtil.startOfDay(endTime);
            UsageEvents events = manager.queryEvents(startTime, endTime);
            UsageEvents.Event event = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                // 解析时间
                events.getNextEvent(event);
                int eventType = event.getEventType();
                long eventTime = event.getTimeStamp();
                String eventPackage = event.getPackageName();
                // 开始点设置
                if (eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    // 获取bean
                    AppItem item = containItem(items, eventPackage);
                    if (item == null) {
                        item = new AppItem();
                        item.mPackageName = eventPackage;
                        items.add(item);
                    }
                    if (!startPoints.containsKey(eventPackage) || startPoints.get(eventPackage) == 0) {
                        startPoints.put(eventPackage, eventTime);
                    }
                }
                // 计算时间和次数 事件应该是连续的
                if (TextUtils.isEmpty(prevPackage)) prevPackage = eventPackage;
                if (!prevPackage.equals(eventPackage)) { // 包名有变化
                    if (startPoints.containsKey(prevPackage) && startPoints.get(prevPackage) > 0) {
                        // 计算时间
                        long start = startPoints.get(prevPackage);
                        AppItem prevItem = containItem(items, prevPackage);
                        if (prevItem != null) {
                            prevItem.mEventTime = eventTime;
                            prevItem.mUsageTime += eventTime - start;
                            if (prevItem.mUsageTime > AppConst.USAGE_TIME_MIX) {
                                prevItem.mCount++;
                            }
                        }
                        // 重置
                        startPoints.put(prevPackage, 0L);
                    }
                    prevPackage = eventPackage;
                    // 开始点设置
                    if (eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        // 获取bean
                        AppItem item2 = containItem(items, eventPackage);
                        if (item2 == null) {
                            item2 = new AppItem();
                            item2.mPackageName = eventPackage;
                            items.add(item2);
                        }
                        if (!startPoints.containsKey(eventPackage) || startPoints.get(eventPackage) == 0) {
                            startPoints.put(eventPackage, eventTime);
                        }
                    }
                }
            }
        }
        // 按照使用时长排序
        if (items.size() > 0) {
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

            boolean hideSystem = PreferenceManager.getInstance().getBoolean(PreferenceManager.PREF_SETTINGS_HIDE_SYSTEM_APPS);
            boolean hideUninstall = PreferenceManager.getInstance().getBoolean(PreferenceManager.PREF_SETTINGS_HIDE_UNINSTALL_APPS);
            List<IgnoreItem> ignoreItems = DbExecutor.getInstance().getAllItems();
            PackageManager packageManager = context.getPackageManager();
            for (AppItem item : items) {
                if (hideSystem && AppUtil.isSystemApp(packageManager, item.mPackageName)) {
                    continue;
                }
                if (hideUninstall && !AppUtil.isInstalled(packageManager, item.mPackageName)) {
                    continue;
                }
                if (inIgnoreList(ignoreItems, item.mPackageName)) {
                    continue;
                }
                item.mName = AppUtil.parsePackageName(packageManager, item.mPackageName);
                newList.add(item);
            }
        }
        return newList;
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

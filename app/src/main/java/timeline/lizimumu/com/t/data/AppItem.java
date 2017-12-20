package timeline.lizimumu.com.t.data;

import java.util.Locale;

/**
 * App Item
 * Created by zb on 18/12/2017.
 */

public class AppItem {
    public String mName;
    public String mPackageName;
    public long mEventTime;
    public long mUsageTime;
    boolean mIsSystem;
    public int mEventType;
    public int mCount;

    @Override
    public String toString() {
        return String.format(Locale.getDefault(),
                "name:%s package_name:%s time:%d total:%d type:%d system:%b count:%d",
                mName, mPackageName, mEventTime, mUsageTime, mEventType, mIsSystem, mCount);
    }

    public AppItem copy() {
        AppItem newItem = new AppItem();
        newItem.mName = this.mName;
        newItem.mPackageName = this.mPackageName;
        newItem.mEventTime = this.mEventTime;
        newItem.mUsageTime = this.mUsageTime;
        newItem.mEventType = this.mEventType;
        newItem.mIsSystem = this.mIsSystem;
        newItem.mCount = this.mCount;
        return newItem;
    }
}

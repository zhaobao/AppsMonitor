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
}

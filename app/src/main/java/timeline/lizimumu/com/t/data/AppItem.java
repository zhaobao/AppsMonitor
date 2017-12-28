package timeline.lizimumu.com.t.data;

import java.util.Locale;

/**
 * App Item
 * Created by zb on 18/12/2017.
 */

public class AppItem {
    public String mName;
    public String mPackageName;
    public long mFirstUsedTime;
    public long mLastUsedTime;
    public long mTotalForTime;
    private boolean mIsSystem;
    public long mMobile;

    @Override
    public String toString() {
        return String.format(Locale.getDefault(),
                "name:%s package_name:%s first:%d last:%d total:%d system:%b mobile:%d",
                mName, mPackageName, mFirstUsedTime, mLastUsedTime, mTotalForTime, mIsSystem, mMobile);
    }
}

package timeline.lizimumu.com.t.stat;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * statistic
 * Created by zb on 24/12/2017.
 */

public class StatManager {

    private static FirebaseAnalytics mFirebaseInstance;
    private static StatManager mInstance;

    private StatManager() {
    }

    public static void initInstance(Context context) {
        mFirebaseInstance = FirebaseAnalytics.getInstance(context);
        mInstance = new StatManager();
    }

    public static StatManager getInstance() {
        return mInstance;
    }

    public void top1Event(String packageName) {
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_NAME, packageName);
        mFirebaseInstance.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, params);
    }

    public void shareEvent(String shareText) {
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.CONTENT, shareText);
        mFirebaseInstance.logEvent(FirebaseAnalytics.Event.SHARE, params);
    }
}

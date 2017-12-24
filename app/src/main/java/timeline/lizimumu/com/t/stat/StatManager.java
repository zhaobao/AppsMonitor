package timeline.lizimumu.com.t.stat;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * statistic
 * Created by zb on 24/12/2017.
 */

public class StatManager {

    public final static String KEY_SHARE = "share";
    public final static String KEY_TOP1 = "top1";

    private static FirebaseAnalytics mFirebaseInstance;
    private static StatManager mInstance;

    private StatManager() {}

    public static void initInstance(Context context) {
        mFirebaseInstance = FirebaseAnalytics.getInstance(context);
        mInstance = new StatManager();
    }

    public static StatManager getInstance() {
        return mInstance;
    }

    public void logEvent(StatEnum key, Bundle bundle) {
        mFirebaseInstance.logEvent(key.name(), bundle);
    }
}

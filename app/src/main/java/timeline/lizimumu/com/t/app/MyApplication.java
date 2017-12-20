package timeline.lizimumu.com.t.app;

import android.app.Application;
import android.content.Intent;

import timeline.lizimumu.com.t.BuildConfig;
import timeline.lizimumu.com.t.data.AppItem;
import timeline.lizimumu.com.t.database.DbExecutor;
import timeline.lizimumu.com.t.service.AppService;
import timeline.lizimumu.com.t.util.PreferenceManager;

/**
 * My Application
 * Created by zb on 18/12/2017.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.init(this);
        getApplicationContext().startService(new Intent(getApplicationContext(), AppService.class));
        DbExecutor.init(getApplicationContext());
        AppItem item = new AppItem();
        item.mPackageName = BuildConfig.APPLICATION_ID;
        item.mEventTime = System.currentTimeMillis();
        DbExecutor.getInstance().insertItem(item);
    }
}

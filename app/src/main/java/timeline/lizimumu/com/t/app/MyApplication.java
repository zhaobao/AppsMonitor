package timeline.lizimumu.com.t.app;

import android.app.Application;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

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
        insertDefault();
    }

    private void insertDefault() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> mDefaults = new ArrayList<>();
                mDefaults.add("com.android.settings");
                mDefaults.add(BuildConfig.APPLICATION_ID);
                for (String packageName : mDefaults) {
                    AppItem item = new AppItem();
                    item.mPackageName = packageName;
                    item.mEventTime = System.currentTimeMillis();
                    DbExecutor.getInstance().insertItem(item);
                }
            }
        }).run();
    }
}

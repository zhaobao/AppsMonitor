package timeline.lizimumu.com.t.service;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import timeline.lizimumu.com.t.util.PreferenceManager;

/**
 * token
 * Created by zb on 03/01/2018.
 */

public class InstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onCreate() {
        super.onCreate();
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        PreferenceManager.getInstance().putString(PreferenceManager.FCM_ID, refreshedToken);
    }

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        PreferenceManager.getInstance().putString(PreferenceManager.FCM_ID, refreshedToken);
    }
}

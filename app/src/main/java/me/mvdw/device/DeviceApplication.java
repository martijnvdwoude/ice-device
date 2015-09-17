package me.mvdw.device;

import android.app.Application;
import com.estimote.sdk.EstimoteSDK;

/**
 * Created by Martijn van der Woude on 12-09-15.
 */
public class DeviceApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        EstimoteSDK.initialize(getApplicationContext(),
                getString(R.string.estimote_app_id),
                getString(R.string.estimote_app_token));

        EstimoteSDK.enableDebugLogging(true);
    }
}
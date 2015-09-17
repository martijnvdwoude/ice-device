package me.mvdw.device.activity;

import android.app.LoaderManager;
import android.content.Loader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.estimote.sdk.Beacon;

import me.mvdw.device.R;
import me.mvdw.device.connection.RegisterToProxyLoader;
import me.mvdw.device.model.ProxyResponse;

/**
 * Created by Martijn van der Woude on 12-09-15.
 */
public class IceDeviceMainActivity extends BaseBeaconRangingActivity {

    private Handler mHandler;
    private Beacon mClosestBeacon;
    private Beacon mRegisteredBeacon;

    private enum ScreenState {
        BLUETOOTH_UNAVAILABLE,
        SCANNING_FOR_BEACON,
        BEACON_LINKED
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.screenTitle_main));

        setSupportActionBar(toolbar);

        mHandler = new Handler();
    }

    @Override
    protected void onPause() {
        if(Build.VERSION.SDK_INT >= 21) {
            mHandler.removeCallbacks(animateBeaconRunnable);
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        if(mClosestBeacon == null) {
            if (Build.VERSION.SDK_INT >= 21) {
                mHandler.postDelayed(animateBeaconRunnable, 1000);
            } else {
                findViewById(R.id.estimote_beacon_scanning_indicator).setVisibility(View.VISIBLE);
            }
        }

        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_preferences:
                startActivity(IceDevicePreferenceActivity.getIntentToStartActivity(IceDeviceMainActivity.this));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void bluetoothUnavailable() {
        setScreenState(ScreenState.BLUETOOTH_UNAVAILABLE);
    }

    @Override
    protected void setClosestBeacon(Beacon closestBeacon) {
        if(shouldDoRegistration(closestBeacon)){
            registerDeviceToProxy(closestBeacon);
        }

        mClosestBeacon = closestBeacon;
    }

    private boolean shouldDoRegistration(Beacon closestBeacon){
        if(mClosestBeacon == null){
            return closestBeacon != null;
        } else {
            return closestBeacon == null || mClosestBeacon.getMajor() != closestBeacon.getMajor();
        }
    }

    private void registerDeviceToProxy(final Beacon beacon){
        getLoaderManager().restartLoader(0, null, new LoaderManager.LoaderCallbacks<ProxyResponse>() {
            @Override
            public Loader<ProxyResponse> onCreateLoader(int i, Bundle bundle) {
                return new RegisterToProxyLoader(IceDeviceMainActivity.this, beacon);
            }

            @Override
            public void onLoadFinished(Loader<ProxyResponse> loader, ProxyResponse proxyResponse) {
                getLoaderManager().destroyLoader(0);

                if (proxyResponse != null) {
                    if (beacon != null) {
                        if (mRegisteredBeacon != null) {
                            mRegisteredBeacon = beacon;
                            updateScreenWithNewBeacon(proxyResponse);
                        } else {
                            mRegisteredBeacon = beacon;
                            setScreenState(ScreenState.BEACON_LINKED);
                            updateScreenWithNewBeacon(proxyResponse);
                        }
                    } else {
                        if (mRegisteredBeacon != null) {
                            setScreenState(ScreenState.SCANNING_FOR_BEACON);
                        }

                        mRegisteredBeacon = null;
                    }
                } else {
                    // Something went wrong
                    mClosestBeacon = null;
                }
            }

            @Override
            public void onLoaderReset(Loader<ProxyResponse> loader) {

            }
        }).forceLoad();
    }

    private void setScreenState(ScreenState screenState){
        switch (screenState){
            case BLUETOOTH_UNAVAILABLE:
                findViewById(R.id.beacon_content_wrapper).setVisibility(View.GONE);
                findViewById(R.id.layout_no_bluetooth).setVisibility(View.VISIBLE);

                break;
            case SCANNING_FOR_BEACON:
                findViewById(R.id.layout_scanning_placeholder).setVisibility(View.VISIBLE);
                findViewById(R.id.layout_beacon_info).setVisibility(View.GONE);

                if(Build.VERSION.SDK_INT >= 21) {
                    mHandler.postDelayed(animateBeaconRunnable, 1000);
                } else {
                    findViewById(R.id.estimote_beacon_scanning_indicator).setVisibility(View.VISIBLE);
                }

                break;
            case BEACON_LINKED:
                findViewById(R.id.layout_scanning_placeholder).setVisibility(View.GONE);
                findViewById(R.id.layout_beacon_info).setVisibility(View.VISIBLE);

                if(Build.VERSION.SDK_INT >= 21) {
                    mHandler.removeCallbacks(animateBeaconRunnable);
                } else {
                    findViewById(R.id.estimote_beacon_scanning_indicator).setVisibility(View.GONE);
                }

                break;
        }
    }

    private void updateScreenWithNewBeacon(ProxyResponse proxyResponse){
        ((TextView) findViewById(R.id.beacon_info_title)).setText(mRegisteredBeacon.getMacAddress());
        ((TextView) findViewById(R.id.beacon_info_range)).setText("" + mRegisteredBeacon.getRssi());
        ((TextView) findViewById(R.id.beacon_info_power)).setText("" + mRegisteredBeacon.getMeasuredPower());
        ((TextView) findViewById(R.id.beacon_info_workstation_id)).setText(proxyResponse.getWorkstationId());
    }

    /**
     * Fancy scanning animation for beacon
     *
     */
    Runnable animateBeaconRunnable = new Runnable() {
        @Override
        public void run() {
            Drawable background = findViewById(R.id.estimote_beacon).getBackground();

            if(background instanceof RippleDrawable) {
                final RippleDrawable rippleDrawable = (RippleDrawable) background;
                rippleDrawable.setState(new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled});

                Handler handler = new Handler();

                handler.postDelayed(new Runnable() {
                    @Override public void run() {
                        rippleDrawable.setState(new int[]{});
                    }
                }, 250);
            }

            mHandler.postDelayed(animateBeaconRunnable, 1200);
        }
    };
}
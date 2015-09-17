package me.mvdw.device.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Created by Martijn van der Woude on 12-09-15.
 */
public class BaseBeaconRangingActivity extends AppCompatActivity {

    private BeaconManager beaconManager;

    private static final int REQUEST_ENABLE_BT = 100;

    private static Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        beaconManager = new BeaconManager(this);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> beacons) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Beacon closestBeacon = getClosestBeaconWithinRange(beacons);
                        setClosestBeacon(closestBeacon);
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!beaconManager.hasBluetooth()) {
            bluetoothUnavailable();
            return;
        }

        if (!beaconManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            connectToService();
        }
    }

    @Override
    protected void onStop() {
        try {
            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
        } catch (RemoteException e) {
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        beaconManager.disconnect();
        super.onDestroy();
    }

    /**
     * Connect to the beaconmanager and start the ranging service
     *
     */
    private void connectToService() {
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (RemoteException e) {
                }
            }
        });
    }

    /**
     * Returns the closest beacon that is in the proximity
     *
     * @param beacons
     * @return Beacon
     */
    private Beacon getClosestBeaconWithinRange(List<Beacon> beacons){
        ArrayList<Beacon> beaconsWithinRange = new ArrayList<>();

        for(Beacon beacon : beacons) {
            if(Utils.computeProximity(beacon) == Utils.Proximity.IMMEDIATE){
                beaconsWithinRange.add(0, beacon);
            }
        }

        Collections.sort(beaconsWithinRange, new BeaconRangeComparator());

        if(beaconsWithinRange.isEmpty()) {
            return null;
        } else {
            return beaconsWithinRange.get(0);
        }
    }

    public class BeaconRangeComparator implements Comparator<Beacon> {
        public int compare(Beacon left, Beacon right) {
            return left.getRssi() - right.getRssi();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                connectToService();
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Override this method to handle showing the state when Bluetooth LE is not
     * available on this device
     *
     */
    protected void bluetoothUnavailable(){}

    /**
     * Override this method to show information about the closest beacon within
     * range on the screen when this device has been linked to a beacon
     *
     */
    protected void setClosestBeacon(Beacon beacon){}
}

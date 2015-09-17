package me.mvdw.device.connection;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.repackaged.gson_v2_3_1.com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import me.mvdw.device.R;
import me.mvdw.device.model.ProxyResponse;

/**
 * Created by Martijn van der Woude on 16-09-15.
 */
public class RegisterToProxyLoader extends AsyncTaskLoader<ProxyResponse> {

    private Context mContext;
    private Beacon mBeacon;

    public RegisterToProxyLoader(Context context, Beacon beacon) {
        super(context);
        this.mContext = context;
        this.mBeacon = beacon;
    }

    @Override
    public ProxyResponse loadInBackground() {
        HttpURLConnection connection = null;

        try {
            URL url = new URL("http", getUrl(), getPort(), getQuery());

            connection = (HttpURLConnection) url.openConnection();
            Log.d("Opening connection to:", url.toString());

            connection.connect();

            int status = connection.getResponseCode();

            switch (status){
                case 200:
                    InputStream in = new BufferedInputStream(connection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder result = new StringBuilder();

                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    reader.close();

                    return new Gson().fromJson(result.toString(), ProxyResponse.class);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }

    private String getUrl(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        if(sharedPreferences.getBoolean("preference_proxy_custom_url_checkbox", false)) {
            return sharedPreferences.getString("preference_proxy_custom_url", mContext.getString(R.string.default_proxy_url));
        } else {
            return mContext.getString(R.string.default_proxy_url);
        }
    }

    private int getPort(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        try {
            return Integer.valueOf(sharedPreferences.getString("preference_proxy_port", ""));
        }catch(Exception e) {
            return 80;
        }
    }

    private String getPath(){
        return mContext.getString(R.string.register_to_proxy_path);
    }

    private String getQuery(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        StringBuilder proxyUrlQueryBuilder = new StringBuilder();
        proxyUrlQueryBuilder.append(getPath());

        // Client IP address
        try {
            List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : networkInterfaces) {
                List<InetAddress> inetAddresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress inetAddress : inetAddresses) {
                    if (!inetAddress.isLoopbackAddress()) {
                        String address = inetAddress.getHostAddress().toUpperCase();
                        if (inetAddress instanceof Inet4Address) {
                            proxyUrlQueryBuilder.append("?ip=").append(address);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // IceMobile phone id and estimote_beacon mac address
        proxyUrlQueryBuilder.append("&deviceId=").append(sharedPreferences.getString("preference_proxy_device_id", ""));

        if(mBeacon != null) {
            proxyUrlQueryBuilder.append("&workstation=").append(mBeacon.getMacAddress());
        }

        return proxyUrlQueryBuilder.toString();
    }
}
package me.asuka.monitor.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.SignalStrength;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import me.asuka.monitor.Application;
import me.asuka.monitor.R;
import me.asuka.monitor.databinding.ActivityMainBinding;
import me.asuka.monitor.util.NetUtils;
import me.asuka.monitor.util.PermissionUtils;
import me.asuka.monitor.util.PreferenceUtils;

@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private static final int BLUETOOTH_PERMISSION_REQUEST = 2;

    private static final String[] LOCATION_REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final String[] BLUETOOTH_REQUIRED_PERMISSIONS = {
            Manifest.permission.BLUETOOTH_SCAN
    };

    private MainActivity mActivity;

    private ActivityMainBinding binding;

    private LocationManager mLocationManager;

    private LocationProvider mProvider;

    private LocationProvider mNetworkProvider;

    private BluetoothManager mBluetoothManager;

    private BluetoothAdapter mBluetoothAdapter;

    private TelephonyManager mTelephonyManager;

    private boolean mUserDeniedPermission = false;

    private boolean mIsInit;

    private static final long BLE_REPORT_PERIOD = 10;

    private TextView bt1AddressEt;
    private TextView bt2AddressEt;
    private TextView bt3AddressEt;
    private TextView bt1RSSIEt;
    private TextView bt2RSSIEt;
    private TextView bt3RSSIEt;

    private long lastLocationReportTime;

    private static class BLE implements Comparable<BLE> {
        String address;
        int rssi;
        long foundAt;

        public BLE(String address, int rssi) {
            this.address = address;
            this.rssi = rssi;
            this.foundAt = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BLE ble = (BLE) o;
            return Objects.equals(address, ble.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address);
        }

        @Override
        public int compareTo(BLE other) {
            return Integer.compare(other.rssi, this.rssi);
        }
    }

    private final List<BLE> bleDevices = new ArrayList<>();

    private static class Freq implements Comparable<Freq> {
        double speed;
        int freq;

        public Freq(double speed, int freq) {
            this.speed = speed;
            this.freq = freq;
        }

        @Override
        public int compareTo(Freq o) {
            return Double.compare(this.speed, o.speed);
        }
    }

    private final List<Freq> reportFreq = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = this;
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bt1AddressEt = findViewById(R.id.bt1_address);
        bt2AddressEt = findViewById(R.id.bt2_address);
        bt3AddressEt = findViewById(R.id.bt3_address);
        bt1RSSIEt = findViewById(R.id.bt1_rssi);
        bt2RSSIEt = findViewById(R.id.bt2_rssi);
        bt3RSSIEt = findViewById(R.id.bt3_rssi);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.log_out:
                PreferenceUtils.remove("user_id");
                // Turn to LoginActivity
                startActivity(new Intent().setClass(
                        MainActivity.this,
                        LoginActivity.class));
                finish();
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mUserDeniedPermission) {
            requestPermissionAndInit(this);
        } else {
            // Explain permission to user (don't request permission here directly to avoid infinite
            // loop if user selects "Don't ask again") in system permission prompt
            showLocationPermissionDialog();
        }
    }

    private void requestPermissionAndInit(final Activity activity) {
        if (PermissionUtils.hasGrantedPermissions(activity, LOCATION_REQUIRED_PERMISSIONS)) {
            initLocation();
        } else {
            // Request permissions from the user
            ActivityCompat.requestPermissions(mActivity,
                    LOCATION_REQUIRED_PERMISSIONS, LOCATION_PERMISSION_REQUEST);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (PermissionUtils.hasGrantedPermissions(activity, BLUETOOTH_REQUIRED_PERMISSIONS)) {
                initBluetooth();
            } else {
                // Request permissions from the user
                ActivityCompat.requestPermissions(mActivity,
                        BLUETOOTH_REQUIRED_PERMISSIONS, BLUETOOTH_PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mUserDeniedPermission = false;
                initLocation();
            } else {
                mUserDeniedPermission = true;
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void initLocation() {
        // GPS init
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mProvider = mLocationManager.getProvider(LocationManager.GPS_PROVIDER);
        mNetworkProvider = mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER);
        if (mProvider == null) {
            Log.e(TAG, "Unable to get GPS_PROVIDER");
            Toast.makeText(this, "Unable to get GPS_PROVIDER",
                    Toast.LENGTH_SHORT).show();
        }
        if (mNetworkProvider == null) {
            Log.e(TAG, "Unable to get NETWORK_PROVIDER");
            Toast.makeText(this, "Unable to get NETWORK_PROVIDER",
                    Toast.LENGTH_SHORT).show();
        }

        mLocationManager.requestLocationUpdates(
                mProvider.getName(), 1000, 0, this);
//        if (mNetworkProvider != null) {
//            mLocationManager.requestLocationUpdates(
//                    mNetworkProvider.getName(), 1000, 0, this);
//        }
    }

    @SuppressLint("MissingPermission")
    private void initBluetooth() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        mBluetoothAdapter.getBluetoothLeScanner().startScan(mScanCallback);
        Application.getExecutor().scheduleAtFixedRate(
                this::reportBLETop3, 0, BLE_REPORT_PERIOD, TimeUnit.SECONDS);

        // Telephony init
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mTelephonyManager.registerTelephonyCallback(getMainExecutor(), new MyCallback());
        }

        Application.getExecutor().scheduleAtFixedRate(
                this::queryReportFreq, 0, 10, TimeUnit.MINUTES);
    }

    private void reportBLETop3() {
        synchronized (bleDevices) {
            String[] top3Address = new String[]{"-", "-", "-"};
            if (bleDevices.size() > 0) {
                top3Address[0] = bleDevices.get(0).address;
            }
            if (bleDevices.size() > 1) {
                top3Address[1] = bleDevices.get(1).address;
            }
            if (bleDevices.size() > 2) {
                top3Address[2] = bleDevices.get(2).address;
            }
            NetUtils.reportUserBLETop3(this, top3Address, response -> {});
        }
    }

    private void queryReportFreq() {
        NetUtils.queryGPSReportFreq(this, response -> {
            reportFreq.clear();
            for (int i = 0; i < response.length(); i++) {
                try {
                    JSONObject obj = response.getJSONObject(i);
                    reportFreq.add(new Freq(obj.getDouble("speed"), obj.getInt("freq")));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            Collections.sort(reportFreq);
            Log.i(TAG, "queryReportFreq: " + reportFreq);
        });
    }

    private final ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.i(TAG, String.format("device - %s, rssi - %d", result.getDevice().getAddress(), result.getRssi()));
            BLE ble = new BLE(result.getDevice().getAddress(), result.getRssi());
            synchronized (bleDevices) {
                Iterator<BLE> itr = bleDevices.iterator();
                while (itr.hasNext()) {
                    BLE device = itr.next();
                    if (System.currentTimeMillis() - device.foundAt > 3000
                            || device.address.equals(ble.address)) {
                        itr.remove();
                    }
                }
                bleDevices.add(ble);
                Collections.sort(bleDevices);
                if (bleDevices.size() > 0) {
                    bt1AddressEt.setText(bleDevices.get(0).address);
                    bt1RSSIEt.setText(bleDevices.get(0).rssi + " RSSI");
                } else {
                    bt1AddressEt.setText("-");
                    bt1RSSIEt.setText("-");
                }
                if (bleDevices.size() > 1) {
                    bt2AddressEt.setText(bleDevices.get(1).address);
                    bt2RSSIEt.setText(bleDevices.get(1).rssi + " RSSI");
                } else {
                    bt2AddressEt.setText("-");
                    bt2RSSIEt.setText("-");
                }
                if (bleDevices.size() > 2) {
                    bt3AddressEt.setText(bleDevices.get(2).address);
                    bt3RSSIEt.setText(bleDevices.get(2).rssi + " RSSI");
                } else {
                    bt3AddressEt.setText("-");
                    bt3RSSIEt.setText("-");
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                Log.i(TAG, "rssi: " + result.getRssi());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static class MyCallback extends TelephonyCallback implements
            TelephonyCallback.CellInfoListener,
            TelephonyCallback.SignalStrengthsListener {

        @Override
        public void onCellInfoChanged(@NonNull List<CellInfo> cellInfo) {
            Log.i(TAG, "onCellInfoChanged: " + cellInfo.size());
        }

        @Override
        public void onSignalStrengthsChanged(@NonNull SignalStrength signalStrength) {
            Log.i(TAG, "onSignalStrengthsChanged: " + signalStrength.getCellSignalStrengths().size());
        }
    }

    private void showLocationPermissionDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this)
//                .setTitle("Needs")
//                .setMessage("Permission")
//                .setCancelable(false)
//                .setPositiveButton("Accept",
//                        (dialog, which) -> {
//                            // Request permissions from the user
//                            ActivityCompat.requestPermissions(mActivity,
//                                    REQUIRED_PERMISSIONS, PERMISSION_REQUEST);
//                        }
//                )
//                .setNegativeButton("Reject",
//                        (dialog, which) -> {
//                            // Exit app
//                            finish();
//                        }
//                );
//        builder.create().show();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        ((TextView) findViewById(R.id.longitudeTv)).setText(String.valueOf(location.getLongitude()));
        ((TextView) findViewById(R.id.latitudeTv)).setText(String.valueOf(location.getLatitude()));
        ((TextView) findViewById(R.id.altitudeTv)).setText(String.valueOf(location.getAltitude()));
        ((TextView) findViewById(R.id.speedTv)).setText(String.valueOf(location.getSpeed()));
        float speed = location.getSpeed();
        if (reportFreq.size() == 0) {
            if (speed < 0.1f) {
                if (System.currentTimeMillis() - lastLocationReportTime > 100*1000) {
                    lastLocationReportTime = System.currentTimeMillis();
                    NetUtils.reportUserLocation(this, location, response -> {});
                }
            } else if (speed < 1.0f) {
                if (System.currentTimeMillis() - lastLocationReportTime > 10*1000) {
                    lastLocationReportTime = System.currentTimeMillis();
                    NetUtils.reportUserLocation(this, location, response -> {});
                }
            } else if (speed < 5.0f) {
                if (System.currentTimeMillis() - lastLocationReportTime > 3*1000) {
                    lastLocationReportTime = System.currentTimeMillis();
                    NetUtils.reportUserLocation(this, location, response -> {});
                }
            } else {
                if (System.currentTimeMillis() - lastLocationReportTime > 1000) {
                    lastLocationReportTime = System.currentTimeMillis();
                    NetUtils.reportUserLocation(this, location, response -> {});
                }
            }
        } else {
            for (Freq freq : reportFreq) {
                if (speed <= freq.speed &&
                        System.currentTimeMillis() - lastLocationReportTime > freq.freq) {
                    lastLocationReportTime = System.currentTimeMillis();
                    NetUtils.reportUserLocation(this, location, response -> {});
                }
            }
        }

    }

    @Override
    public void onLocationChanged(@NonNull List<Location> locations) {
        LocationListener.super.onLocationChanged(locations);
    }

    @Override
    public void onFlushComplete(int requestCode) {
        LocationListener.super.onFlushComplete(requestCode);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LocationListener.super.onStatusChanged(provider, status, extras);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }
}

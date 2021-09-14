package com.android.settings.bluetooth.irk;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.os.Handler;
import android.text.TextUtils;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class BluetoothIrkActivity extends Activity {

  private BluetoothAdapter mBluetoothAdapter = null;
  private boolean mIsScanning = false;

  private boolean mUseCallback = false;
  private TextView mTextView = null;

  private EditText mAddressText = null;
  private EditText mIrkText = null;
  private CheckBox mIsPublicCheckBox = null;
  private CheckBox mIsBondedCheckBox = null;

  // UI Toggles
  private boolean mIsPublic = true;
  private boolean mIsBonded = true;

  private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

      @Override
      public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
          Log.i("TEST", "status: " + status + " newState: " + newState);
      }
  };

  private final ScanCallback mScanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      Log.i("TEST ScanCallback",
          "result: " + result.getDevice().getAddress() + " : " + result.getDevice()
              .getName());
      final StringBuilder t = new StringBuilder();
      t.append(result.getDevice().getAddress());
      new Handler().post(new Runnable() {
        @Override
        public void run() {
          mTextView.setText(t.toString());
        }
      });
      result.getDevice().connectGatt(BluetoothIrkActivity.this, false, mGattCallback);
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
      super.onBatchScanResults(results);
      Log.i("TEST", "Batch result: " + results.size());
    }

    @Override
    public void onScanFailed(int errorCode) {
      super.onScanFailed(errorCode);
      Log.i("TEST", "failed: " + errorCode);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.irk_activity);

    Button btn = findViewById(R.id.btn);
    btn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
          mUseCallback = false;
        handleActionButton();
      }
    });
    Button btn2 = findViewById(R.id.btn2);
    btn2.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mUseCallback = true;
        handleActionButton();
      }
    });
    mTextView = (TextView) findViewById(R.id.tv_result);
    mAddressText = (EditText) findViewById(R.id.et_address);
    mIrkText = (EditText) findViewById(R.id.et_irk);
    mIsBondedCheckBox = (CheckBox) findViewById(R.id.cb_bonded);
    mIsPublicCheckBox = (CheckBox) findViewById(R.id.cb_public);
  }


  void handleActionButton() {
    final BluetoothManager bluetoothManager =
        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = bluetoothManager.getAdapter();

    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, 1);
    } else {
      enforcePermissionAndRun();
    }
  }

  void enforcePermissionAndRun() {
    if (checkSelfPermission(permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
      if (mIsScanning) {
        stopScanning();
      } else {
        startScanning();
      }
    } else if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
      Toast.makeText(this, "Cannot use without the permission!", Toast.LENGTH_LONG).show();
    } else {
      requestPermissions(new String[]{permission.ACCESS_FINE_LOCATION}, 1);
    }
  }

  public void onRequestPermissionsResults(int requestCode, String[] permissions,
      int[] grantResults) {
    switch (requestCode) {
      case 1:
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          if (mIsScanning) {
            stopScanning();
          } else {
            startScanning();
          }
        } else {
          Toast.makeText(this, "Cannot use without the permission!", Toast.LENGTH_LONG).show();
        }
      default:
        break;
    }
  }

  /* s must be an even-length string. */
  private static byte[] hexStringToByteArray(String s) {
      int len = s.length();
      byte[] data = new byte[len / 2];
      for (int i = 0; i < len; i += 2) {
          data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                               + Character.digit(s.charAt(i+1), 16));
      }
      return data;
  }

  byte[] irkBytesFromInput(final String irkString) {
      if (irkString.length() != 32) {
          Log.i("TEST", "Bad IRK Length!");
          return null;
      }
      return hexStringToByteArray(irkString);
  }

  boolean validateAddress(final String address) {
      Log.i("TEST", "Address: " + address);
      return !TextUtils.isEmpty(address) && address.split(":").length == 6;
  }

  void startScanning() {
    mIsScanning = true;
    mIsBonded = mIsBondedCheckBox.isChecked();
    mIsPublic = mIsPublicCheckBox.isChecked();

    BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

    // Test the good stuff
    List<ScanFilter> scanFilterList = new ArrayList<ScanFilter>();
    final String address = mAddressText.getText().toString();
    final String irkString = mIrkText.getText().toString();
    final int addressType = (mIsPublic) ? BluetoothDevice.ADDRESS_TYPE_PUBLIC : BluetoothDevice.ADDRESS_TYPE_RANDOM;
    final boolean isBonded = mIsBonded;

    if (!validateAddress(address)) {
        Toast.makeText(this, "Please enter a valid address to scan for device!", Toast.LENGTH_LONG).show();
        return;
    }

    if (isBonded) {
        Log.i("TEST", "Using bonded flow");
        scanFilterList.add(new ScanFilter.Builder().setDeviceAddress(address, addressType).build());
    } else {
        Log.i("TEST", "Using un-bonded flow");
        if (TextUtils.isEmpty(irkString)) {
            Toast.makeText(this, "Please enter an IRK in order to scan for un-bonded devices!", Toast.LENGTH_LONG).show();
            return;
        } else {
            byte[] irk = irkBytesFromInput(irkString);

            if (irk == null) {
                Toast.makeText(this, "Failed to convert IRK string to 16 octets", Toast.LENGTH_LONG).show();
                return;
            }

            StringBuilder irkStringAfter = new StringBuilder();
            for (byte b : irk) {
                irkStringAfter.append(String.format("%02X", b));
            }
            Log.i("TEST", "BTIRKA irk: " + irkStringAfter.toString());

            // IRK Scan
            scanFilterList.add(new ScanFilter.Builder().setDeviceAddress(address, addressType, irk).build());
        }
    }
    // Test service uuid scanning
    // scanFilterList.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("0000fff5-0000-1000-8000-00805f9b34fb")).build());

    if (mUseCallback) {
        Log.i("TEST", "Callback Scan");
          // Callback scan
        scanner.startScan(scanFilterList,
            new ScanSettings.Builder().setLegacy(false).setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH).build(),
            mScanCallback);
        findViewById(R.id.btn).setEnabled(false);
    } else {
        // Pending intent scan
        Log.i("TEST", "PI Scan");
        //scanner.startScan(scanFilterList, new ScanSettings.Builder().setLegacy(false).setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH).build(), createPendingIntent());
        scanner.startScan(scanFilterList, new ScanSettings.Builder().setLegacy(false).setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).setScanMode(ScanSettings.SCAN_MODE_AMBIENT_DISCOVERY).build(), createPendingIntent());
        //scanner.startScan(scanFilterList, new ScanSettings.Builder().setLegacy(false).setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build(), createPendingIntent());
        //scanner.startScan(scanFilterList, new ScanSettings.Builder().setLegacy(false).setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), createPendingIntent());
        //scanner.startScan(scanFilterList, new ScanSettings.Builder().setLegacy(false).setCallbackType(ScanSettings.CALLBACK_TYPE_MATCH_LOST).setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), createPendingIntent());
        findViewById(R.id.btn2).setEnabled(false);
    }

    if (isBonded) {
        Toast.makeText(this, "Scanning started for bonded device", Toast.LENGTH_SHORT).show();
    } else {
        Toast.makeText(this, "Scanning started unbonded device", Toast.LENGTH_SHORT).show();
    }
  }

  void stopScanning() {
    BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
    if (mUseCallback) {
        scanner.stopScan(mScanCallback);
        findViewById(R.id.btn).setEnabled(true);
    } else {
        findViewById(R.id.btn2).setEnabled(true);
        scanner.stopScan(createPendingIntent());
    }
    mIsScanning = false;
    Toast.makeText(this, "Scanning stopped", Toast.LENGTH_SHORT).show();
  }

  private PendingIntent createPendingIntent() {
    return PendingIntent
        .getBroadcast(getApplicationContext(), 0,
            new Intent(getApplicationContext(), ScanBroadcastReceiver.class)
                .setAction("com.example.myapplication.ACTION_FOUND")
                .setComponent(new ComponentName("com.android.settings", "com.android.settings.bluetooth.irk.ScanBroadcastReceiver")),
            PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_MUTABLE);
  }
}

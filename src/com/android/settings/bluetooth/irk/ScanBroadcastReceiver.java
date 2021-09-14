package com.android.settings.bluetooth.irk;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.ArrayList;

public class ScanBroadcastReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d("TEST", "Got some");
    ArrayList<ScanResult> results = intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);

    if (results != null) {
      for (final ScanResult result : results) {
        Log.e("TEST",
            "onScanResult : " + result.getDevice().getName() + " " + result.getDevice()
                .getAddress());
      }
    }

  }

}

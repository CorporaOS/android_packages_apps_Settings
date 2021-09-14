package com.android.settings.bluetooth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.view.View;
import android.widget.Button;
import android.os.Handler;

import com.android.settings.R;

import com.android.settings.bluetooth.irk.BluetoothIrkActivity;
import com.android.settings.bluetooth.oob.BluetoothOobActivity;

public class TestLauncherActivity extends Activity implements View.OnClickListener {


    private static final String TAG = "TestLauncherActivity";

    private Button mIrkButton = null;
    private Button mOobButton = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_launcher);

        mIrkButton = findViewById(R.id.btn_irk);
        mIrkButton.setOnClickListener(this);

        mOobButton = findViewById(R.id.btn_oob);
        mOobButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_irk) {
            testIrk();
        } else if (v.getId() == R.id.btn_oob) {
            testOob();
        } else {
            Log.w(TAG, "Unexpected view clicked");
        }
    }

    private void testIrk() {
        startActivity(new Intent(this, BluetoothIrkActivity.class));
    }

    private void testOob() {
        startActivity(new Intent(this, BluetoothOobActivity.class));
    }

}

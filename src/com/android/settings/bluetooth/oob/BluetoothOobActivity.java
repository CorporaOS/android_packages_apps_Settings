/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.bluetooth.oob;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.OobDataCallback;
import android.bluetooth.OobData;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.bluetooth.oob.qrcode.QrCodeGenerator;
import com.android.settings.R;

import org.json.JSONObject;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.WriterException;

import java.lang.String;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;

public class BluetoothOobActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "BtOobActivity";

    private BluetoothAdapter mBluetoothAdapter;
    private final Handler mHandler = new Handler();

    private class E implements Executor {
        private Handler mH;
        E(Handler h) { mH = h; }
        @Override
        public void execute(Runnable r) {
            mH.post(r);
        }
    }

    private class C implements OobDataCallback {
        public C() {

        }

        @Override
        public void onError(int error) {
            Log.i(TAG, "Error: " + error);
            mClassicBtn.setEnabled(true);
            mLeBtn.setEnabled(true);
            mPairClassicBtn.setEnabled(true);
            mPairLeBtn.setEnabled(true);
        }

        @Override
        public void onOobData(int transport, OobData data) {
            Log.i(TAG, "Transport: " + transport + "\n" + data);
            String transportString = "Unset";
            if (transport == 1) {
                transportString = "Classic";
            } else if (transport == 2) {
                transportString = "LE";
            }
            mTvTransport.setText("Transport: " + transportString);
            mClassicBtn.setEnabled(true);
            mLeBtn.setEnabled(true);
            mPairClassicBtn.setEnabled(true);
            mPairLeBtn.setEnabled(true);
            generateQrCode(toJson(transport, data));
        }
    }

    private OobData toOobData(String json) {
        OobData data = null;
        try {
            JSONObject o = new JSONObject(json);
            switch(o.getInt("transport")) {
                case 1:
                    data = new OobData.ClassicBuilder(
                        Base64.decode(o.getString("confirmation"), Base64.DEFAULT),
                        Base64.decode(o.getString("oob_len"), Base64.DEFAULT),
                        Base64.decode(o.getString("address"), Base64.DEFAULT)
                    )
                    .setRandomizerHash(Base64.decode(o.getString("randomizer"), Base64.DEFAULT))
                    .setDeviceName(Base64.decode(o.getString("name"), Base64.DEFAULT))
                    .setClassOfDevice(Base64.decode(o.getString("cod"), Base64.DEFAULT))
                    .build();
                    break;
                case 2:
                    data = new OobData.LeBuilder(
                        Base64.decode(o.getString("confirmation"), Base64.DEFAULT),
                        Base64.decode(o.getString("address"), Base64.DEFAULT),
                        o.getInt("role")
                    )
                    .setRandomizerHash(Base64.decode(o.getString("randomizer"), Base64.DEFAULT))
                    .setDeviceName(Base64.decode(o.getString("name"), Base64.DEFAULT))
                    .setLeTemporaryKey(Base64.decode(o.getString("tk"), Base64.DEFAULT))
                    .setLeFlags(o.getInt("flags"))
                    //.setLeAppearance(o.getString("appearance").getBytes())
                    .build();
                    break;
                default:
                    Log.e(TAG, "Invalid Transport: " + o.getInt("transport"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    private String toJson(int transport, OobData oobData) {
        byte[] address = oobData.getDeviceAddressWithType();
        String addressString =  String.format(Locale.US, "Type: %02X; Address: %02X:%02X:%02X:%02X:%02X:%02X", address[6], address[5], address[4],
                address[3], address[2], address[1], address[0]);
        Log.i(TAG, "Address: " + addressString);
        JSONObject o = new JSONObject();
        try {
            // Both
            o.put("transport", transport);
            o.put("address", Base64.encodeToString(address, Base64.DEFAULT));
            o.put("confirmation", Base64.encodeToString(oobData.getConfirmationHash(), Base64.DEFAULT));
            o.put("randomizer", Base64.encodeToString(oobData.getRandomizerHash(), Base64.DEFAULT));
            o.put("name", Base64.encodeToString(oobData.getDeviceName(), Base64.DEFAULT));

            // Classic
            o.put("oob_len", Base64.encodeToString(oobData.getClassicLength(), Base64.DEFAULT));
            o.put("cod", Base64.encodeToString(oobData.getClassOfDevice(), Base64.DEFAULT));

            // LE
            o.put("tk", Base64.encodeToString(oobData.getLeTemporaryKey(), Base64.DEFAULT));
            o.put("appearance", Base64.encodeToString(oobData.getLeAppearance(), Base64.DEFAULT));
            o.put("flags", oobData.getLeFlags());
            o.put("role", oobData.getLeDeviceRole());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return o.toString();
    }

    private void generateOobData(int transport) {
        BluetoothAdapter.getDefaultAdapter()
                .generateLocalOobData(transport, new E(mHandler), new C());
    }

    private void pairWithQrCode() {
        // Open QR Code scanner
        mClassicBtn.setEnabled(true);
        mLeBtn.setEnabled(true);
        mPairClassicBtn.setEnabled(true);
        mPairLeBtn.setEnabled(true);
        startActivityForResult(new Intent(this, BluetoothQrCodeActivity.class), 1000);
    }

    private Button mClassicBtn;
    private Button mLeBtn;
    private Button mPairClassicBtn;
    private Button mPairLeBtn;
    private TextView mTvTransport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(Activity.RESULT_CANCELED);
        setContentView(R.layout.activity_bluetooth_oob);
        mClassicBtn = findViewById(R.id.btn_classic);
        mClassicBtn.setOnClickListener(this);
        mLeBtn = findViewById(R.id.btn_le);
        mLeBtn.setOnClickListener(this);

        mPairClassicBtn = findViewById(R.id.btn_classic_pair);
        mPairClassicBtn.setOnClickListener(this);
        mPairLeBtn = findViewById(R.id.btn_le_pair);
        mPairLeBtn.setOnClickListener(this);

        mTvTransport = findViewById(R.id.tv_transport);
    }

    @Override
    public void onClick(View v) {
        mClassicBtn.setEnabled(false);
        mLeBtn.setEnabled(false);
        mPairClassicBtn.setEnabled(false);
        mPairLeBtn.setEnabled(false);
        if (v.getId() == R.id.btn_classic) {
            //startAdvertising(1);
            generateOobData(1);
        } else if (v.getId() == R.id.btn_le) {
            //startAdvertising(2);
            generateOobData(2);
        } else if (v.getId() == R.id.btn_classic_pair) {
            pairWithQrCode();
        } else if (v.getId() == R.id.btn_le_pair) {
            pairWithQrCode();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Get QR code result data
        // Parse it into OobData
        Log.i(TAG, "onActivityResult(" + requestCode + ", " + resultCode + ", " + data + ")");
        final String qrcode = data.getStringExtra("qrcode");
        Log.i(TAG, "QRCODE: " + qrcode);
        final OobData oobData = toOobData(qrcode);
        Log.i(TAG, "QRCODE: " + oobData);
        int transport = -1;
        try {
            transport = new JSONObject(qrcode).getInt("transport");
        } catch (Exception E) {
            Log.e(TAG, "Unable to determine transport!");
            Toast.makeText(this, "Unable to determine transport!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (qrcode != null) {
            // Address is little endian, always expected to be public until private implemented
            byte[] address = Arrays.copyOfRange(oobData.getDeviceAddressWithType(), 0, 6);
            String addressString =  String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X", address[5], address[4],
                    address[3], address[2], address[1], address[0]);
            Log.i(TAG, "Address: " + addressString);
            BluetoothAdapter.getDefaultAdapter().getRemoteDevice(addressString)
                    .createBondOutOfBand(
                            transport,
                            (transport == 1) ? oobData : null,
                            (transport == 2) ? oobData : null
                    );
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    private void generateQrCode(String content) {
        Log.i(TAG, "QR Content: " + content);
        try {
            ((ImageView) findViewById(R.id.img_result_qr))
                    .setImageBitmap(QrCodeGenerator.encodeQrCode(content, 256));
        } catch (WriterException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private boolean mIsAdvertising = false;
    BluetoothLeAdvertiser mAdvertiser;
    AdvertiseCallback mAdvertisingCallback;
    AdvertisingSet currentAdvertisingSet;

    private synchronized void startAdvertising(final int transport) {
        if (mIsAdvertising) return;
        mIsAdvertising = true;
        if( !BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported() ) {
            Toast.makeText( this, "Multiple advertisement not supported", Toast.LENGTH_SHORT ).show();
            mIsAdvertising = false;
            return;
        }

        mAdvertiser = BluetoothAdapter
                .getDefaultAdapter().getBluetoothLeAdvertiser();

        AdvertisingSetParameters parameters = (new AdvertisingSetParameters.Builder())
                .setLegacyMode(true) // True by default, but set here as a reminder.
                .setConnectable(true)
                .setScannable(true)
                .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                .build();
        AdvertiseData data = (new AdvertiseData.Builder()).setIncludeDeviceName(true).build();

        AdvertisingSetCallback callback = new AdvertisingSetCallback() {
            @Override
            public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
                Log.i(TAG, "onAdvertisingSetStarted(): txPower:" + txPower + " , status: "
                        + status);
                currentAdvertisingSet = advertisingSet;
                mIsAdvertising = true;
                currentAdvertisingSet.getOwnAddress();
            }

            @Override
            public void onOwnAddressRead(AdvertisingSet advertisingSet, int addressType, String address) {
                Log.i(TAG, "onOwnAddressRead(" + addressType + ", " + address + ")");
                //generateOobData(address, addressType, transport);

                // After onAdvertisingSetStarted callback is called, you can modify the
                // advertising data and scan response data:
                currentAdvertisingSet.setAdvertisingData(new AdvertiseData.Builder().
                        setIncludeDeviceName(true).setIncludeTxPowerLevel(true).build());
                // Wait for onAdvertisingDataSet callback...
                currentAdvertisingSet.setScanResponseData(new
                        AdvertiseData.Builder().addServiceUuid(new ParcelUuid(UUID.randomUUID())).build());
                // Wait for onScanResponseDataSet callback...
            }

            @Override
            public void onAdvertisingDataSet(AdvertisingSet advertisingSet, int status) {
                Log.i(TAG, "onAdvertisingDataSet() :status:" + status);
            }

            @Override
            public void onScanResponseDataSet(AdvertisingSet advertisingSet, int status) {
                Log.i(TAG, "onScanResponseDataSet(): status:" + status);
            }

            @Override
            public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
                Log.i(TAG, "onAdvertisingSetStopped():");
                mIsAdvertising = false;
            }
        };
        mAdvertiser.startAdvertisingSet(parameters, data, null, null, null, callback);


        // When done with the advertising:
//        mAdvertiser.stopAdvertisingSet(callback);
//        mAdvertiser.startAdvertising(settings, data, mAdvertisingCallback);
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (mIsAdvertising) {
//                    Toast.makeText(BluetoothOobActivity.this, "Advertising stopped",
//                    Toast.LENGTH_SHORT).show();
//                    mAdvertiser.stopAdvertising(mAdvertisingCallback);
//                    ((ImageView) findViewById(R.id.img_result_qr)).setImageBitmap(null);
//                    mIsAdvertising = false;
//                }
//            }
//        }, 10000);

    }

}

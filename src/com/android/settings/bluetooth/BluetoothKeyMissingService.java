/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.bluetooth;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;

import com.android.settings.R;

/**
 * BluetoothKeyMissingService shows a notification if there is a key missing
 * which can launch the appropriate dialog when tapped.
 */
public final class BluetoothKeyMissingService extends Service {

    @VisibleForTesting
    static final int NOTIFICATION_ID = android.R.drawable.stat_sys_data_bluetooth;
    @VisibleForTesting
    static final String ACTION_DISMISS_KEY_MISSING =
            "com.android.settings.bluetooth.ACTION_DISMISS_KEY_MISSING";
    @VisibleForTesting
    static final String ACTION_KEY_MISSING_DIALOG =
            "com.android.settings.bluetooth.ACTION_KEY_MISSING_DIALOG";

    private static final String BLUETOOTH_NOTIFICATION_CHANNEL =
            "bluetooth_notification_channel";

    private static final String TAG = "BluetoothKeyMissingService";

    private BluetoothDevice mDevice;

    @VisibleForTesting NotificationManager mNm;

    /**
     * Creates intent that would trigger the dialog displaying Key Missing information
     */
    public static Intent getKeyMissingDialogIntent(Context context, Intent intent,
                                                int initiator) {
        BluetoothDevice device =
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        Intent keyMissingIntent = new Intent();
        keyMissingIntent.setClass(context, BluetoothKeyMissingDialog.class);
        keyMissingIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        keyMissingIntent.putExtra(BluetoothDevice.EXTRA_PAIRING_INITIATOR,
                                initiator);
        keyMissingIntent.setAction(BluetoothDevice.ACTION_KEY_MISSING);
        keyMissingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return keyMissingIntent;
    }

    private boolean mRegistered = false;
    private final BroadcastReceiver mCancelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                                  BluetoothDevice.ERROR);
                Log.d(TAG, "onReceive() Bond state change : " + bondState
                              + ", device name : " + mDevice.getName());
                if ((bondState != BluetoothDevice.BOND_NONE)
                        && (bondState != BluetoothDevice.BOND_BONDED)) {
                    return;
                }
            } else {
                Log.d(TAG, "Don't know how to handle: " + action);
            }

            mNm.cancel(NOTIFICATION_ID);
            stopSelf();
        }
    };

    @Override
    public void onCreate() {
        mNm = getSystemService(NotificationManager.class);
        NotificationChannel notificationChannel = new NotificationChannel(
                BLUETOOTH_NOTIFICATION_CHANNEL, this.getString(R.string.bluetooth),
                NotificationManager.IMPORTANCE_HIGH);
        mNm.createNotificationChannel(notificationChannel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.e(TAG, "Can't start: null intent!");
            stopSelf();
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        Log.d(TAG, "onStartCommand() action : " + action);

        mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (mDevice != null
                && mDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
            Log.w(TAG, "Device " + mDevice.getName()
                          + " not bonded: " + mDevice.getBondState());
            mNm.cancel(NOTIFICATION_ID);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (TextUtils.equals(action, BluetoothDevice.ACTION_KEY_MISSING)) {
            createKeyMissingNotification(intent);
        } else if (TextUtils.equals(action, ACTION_DISMISS_KEY_MISSING)) {
            Log.d(TAG, "Notification cancel "
                          + " (" + mDevice.getName() + ")");
            mNm.cancel(NOTIFICATION_ID);
            stopSelf();
        } else if (TextUtils.equals(action, ACTION_KEY_MISSING_DIALOG)) {
            Intent keyMissingDialogIntent = getKeyMissingDialogIntent(
                     this, intent, BluetoothDevice.EXTRA_PAIRING_INITIATOR_BACKGROUND);

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_PAIRING_CANCEL);
            filter.addAction(ACTION_DISMISS_KEY_MISSING);
            registerReceiver(mCancelReceiver, filter);
            mRegistered = true;

            startActivity(keyMissingDialogIntent);
        }

        return START_STICKY;
    }

    private void createKeyMissingNotification(Intent intent) {
        Resources res = getResources();
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, BLUETOOTH_NOTIFICATION_CHANNEL)
                    .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                    .setTicker(res.getString(R.string.bluetooth_notif_ticker))
                    .setLocalOnly(true);

        Intent keyMissingDialogIntent = new Intent(ACTION_KEY_MISSING_DIALOG);
        keyMissingDialogIntent.setClass(this, BluetoothKeyMissingService.class);
        keyMissingDialogIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);

        PendingIntent keyMissingIntent = PendingIntent.getService(
                this, 0, keyMissingDialogIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        Intent dismissIntent = new Intent(ACTION_DISMISS_KEY_MISSING);
        dismissIntent.setClass(this, BluetoothKeyMissingService.class);
        dismissIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);

        PendingIntent dismissPendingIntent = PendingIntent.getService(
                this, 0, dismissIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
        if (TextUtils.isEmpty(name)) {
            BluetoothDevice device =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            name = device != null ? device.getAlias()
                                  : res.getString(android.R.string.unknownName);
        }

        Log.d(TAG, "Show key missing notification for " + " (" + name + ")");

        NotificationCompat.Action keyMissingAction =
                new NotificationCompat.Action
                    .Builder(
                        0,
                        res.getString(R.string.bluetooth_unpair_dialog_forget_confirm_button),
                        keyMissingIntent)
                    .build();

        NotificationCompat.Action dismissAction =
                new NotificationCompat.Action
                    .Builder(
                        0,
                        res.getString(R.string.bluetooth_notif_key_missing_dismiss),
                        dismissPendingIntent)
                    .build();

        builder.setContentTitle(res.getString(R.string.bluetooth_notif_key_missing_title))
                .setContentText(res.getString(R.string.bluetooth_notif_key_missing_message, name))
                .setContentIntent(keyMissingIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setOngoing(true)
                .setColor(getColor(
                    com.android.internal.R.color.system_notification_accent_color))
                .addAction(keyMissingAction)
                .addAction(dismissAction);

        mNm.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDestroy() {
        if (mRegistered) {
            unregisterReceiver(mCancelReceiver);
            mRegistered = false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // No binding.
        return null;
    }
}

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
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.ViewModelProviders;

import com.android.settings.R;
import com.android.settings.bluetooth.oob.qrcode.QrCamera;
import com.android.settings.bluetooth.oob.qrcode.QrDecorateView;

import java.util.List;

public class BluetoothQrCodeScannerFragment extends BluetoothQrCodeBaseFragment implements
        SurfaceTextureListener,
        QrCamera.ScannerCallback {
    private static final String TAG = "BluetoothQrCodeScanner";

    /** Message sent to hide error message */
    private static final int MESSAGE_HIDE_ERROR_MESSAGE = 1;

    /** Message sent to show error message */
    private static final int MESSAGE_SHOW_ERROR_MESSAGE = 2;

    /** Message sent to manipulate ZXing Bluetooth QR code */
    private static final int MESSAGE_SCAN_ZXING_BLUETOOTH_FORMAT_SUCCESS = 4;

    private static final long SHOW_ERROR_MESSAGE_INTERVAL = 10000;
    private static final long SHOW_SUCCESS_SQUARE_INTERVAL = 1000;

    // Key for Bundle usage
    private static final String KEY_IS_CONFIGURATOR_MODE = "key_is_configurator_mode";
    private static final String KEY_LATEST_ERROR_CODE = "key_latest_error_code";

    private static final int ARG_RESTART_CAMERA = 1;

    private QrCamera mCamera;
    private TextureView mTextureView;
    private QrDecorateView mDecorateView;
    private TextView mErrorMessage;

    /** true if the fragment working for configurator, false enrollee*/
    private boolean mIsConfiguratorMode;

    /** The SSID of the Wi-Fi network which the user specify to enroll */
    private String mSsid;

    private int mLatestStatusCode = -1;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_HIDE_ERROR_MESSAGE:
                    mErrorMessage.setVisibility(View.INVISIBLE);
                    break;

                case MESSAGE_SHOW_ERROR_MESSAGE:
                    final String errorMessage = (String) msg.obj;

                    mErrorMessage.setVisibility(View.VISIBLE);
                    mErrorMessage.setText(errorMessage);
                    mErrorMessage.sendAccessibilityEvent(
                            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

                    // Cancel any pending messages to hide error view and requeue the message so
                    // user has time to see error
                    removeMessages(MESSAGE_HIDE_ERROR_MESSAGE);
                    sendEmptyMessageDelayed(MESSAGE_HIDE_ERROR_MESSAGE,
                            SHOW_ERROR_MESSAGE_INTERVAL);

                    if (msg.arg1 == ARG_RESTART_CAMERA) {
                        setProgressBarShown(false);
                        mDecorateView.setFocused(false);
                        restartCamera();
                    }
                    break;

                case MESSAGE_SCAN_ZXING_BLUETOOTH_FORMAT_SUCCESS:
                    notifyUserForQrCodeRecognition();
                    break;

                default:
            }
        }
    };

    @UiThread
    private void notifyUserForQrCodeRecognition() {
        if (mCamera != null) {
            mCamera.stop();
        }

        mDecorateView.setFocused(true);
        mErrorMessage.setVisibility(View.INVISIBLE);

        // TODO(optedoblivion): Kang vibration
        //WifiDppUtils.triggerVibrationForQrCodeRecognition(getContext());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsConfiguratorMode = savedInstanceState.getBoolean(KEY_IS_CONFIGURATOR_MODE);
            mLatestStatusCode = savedInstanceState.getInt(KEY_LATEST_ERROR_CODE);
        }
    }

    @Override
    public void onPause() {
        if (mCamera != null) {
            mCamera.stop();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        restartCamera();
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    /**
     * Configurator container activity of the fragment should create instance with this constructor.
     */
    public BluetoothQrCodeScannerFragment() {
        super();

        mIsConfiguratorMode = true;
    }

    BluetoothQrCodeScannerFragment(String ssid) {
        super();

        mIsConfiguratorMode = false;
        mSsid = ssid;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setTitle for TalkBack
        if (mIsConfiguratorMode) {
            getActivity().setTitle(R.string.wifi_dpp_add_device_to_network);
        } else {
            getActivity().setTitle(R.string.wifi_dpp_scan_qr_code);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bluetooth_qrcode_scanner_fragment, container,
                /* attachToRoot */ false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTextureView = view.findViewById(R.id.preview_view);
        mTextureView.setSurfaceTextureListener(this);

        mDecorateView = view.findViewById(R.id.decorate_view);

        // TODO(optedoblivion): figure out progress bar use case
        setProgressBarShown(true);

        if (mIsConfiguratorMode) {
            // TODO(optedoblivion): Update title
            setHeaderTitle(R.string.wifi_dpp_add_device_to_network);

            mSummary.setText(getString(R.string.wifi_dpp_center_qr_code,
                    "TODO: Bluetooth Name"));
        } else {
            // TODO(optedoblivion): Update title
            setHeaderTitle(R.string.wifi_dpp_scan_qr_code);
        }

        mErrorMessage = view.findViewById(R.id.error_message);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.removeItem(Menu.FIRST);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        initCamera(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Do nothing
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        destroyCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Do nothing
    }

    @Override
    public Size getViewSize() {
        return new Size(mTextureView.getWidth(), mTextureView.getHeight());
    }

    @Override
    public Rect getFramePosition(Size previewSize, int cameraOrientation) {
        return new Rect(0, 0, previewSize.getHeight(), previewSize.getHeight());
    }

    @Override
    public void setTransform(Matrix transform) {
        mTextureView.setTransform(transform);
    }

    @Override
    public boolean isValid(String qrCode) {
        // TODO(optedoblivion): finish me
        Log.i(TAG, "isValid(): QR CODE: " + qrCode);
//        try {
//            mWifiQrCode = new WifiQrCode(qrCode);
//        } catch (IllegalArgumentException e) {
//            showErrorMessage(R.string.wifi_dpp_qr_code_is_not_valid_format);
//            return false;
//        }

//        // It's impossible to provision other device with ZXing Wi-Fi Network config format
//        final String scheme = mWifiQrCode.getScheme();
//        if (mIsConfiguratorMode && WifiQrCode.SCHEME_ZXING_WIFI_NETWORK_CONFIG.equals(scheme)) {
//            showErrorMessage(R.string.wifi_dpp_qr_code_is_not_valid_format);
//            return false;
//        }

        return true;
    }

    /**
     * This method is only called when QrCamera.ScannerCallback.isValid returns true;
     */
    @Override
    public void handleSuccessfulResult(String qrCode) {
        // TODO(optedoblivion): finish me
        Log.i(TAG, "QR CODE: " + qrCode);
        Intent i = new Intent();
        i.putExtra("qrcode", qrCode);
        getActivity().setResult(Activity.RESULT_OK, i);
        getActivity().finish();
    }

    private void handleZxingBluetoothFormat() {
        Message message = mHandler.obtainMessage(MESSAGE_SCAN_ZXING_BLUETOOTH_FORMAT_SUCCESS);
        // TODO(optedoblivion): Implement BluetoothQrCode()
//        message.obj = new BluetoothQrCode();
        mHandler.sendMessageDelayed(message, SHOW_SUCCESS_SQUARE_INTERVAL);
    }

    @Override
    public void handleCameraFailure() {
        destroyCamera();
    }

    private void initCamera(SurfaceTexture surface) {
        // Check if the camera has already created.
        if (mCamera == null) {
            mCamera = new QrCamera(getContext(), this);
            mCamera.start(surface);
        }
    }

    private void destroyCamera() {
        if (mCamera != null) {
            mCamera.stop();
            mCamera = null;
        }
    }

    private void showErrorMessage(@StringRes int messageResId) {
        final Message message = mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MESSAGE,
                getString(messageResId));
        message.sendToTarget();
    }

    private void showErrorMessageAndRestartCamera(@StringRes int messageResId) {
        final Message message = mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MESSAGE,
                getString(messageResId));
        message.arg1 = ARG_RESTART_CAMERA;
        message.sendToTarget();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_IS_CONFIGURATOR_MODE, mIsConfiguratorMode);
        outState.putInt(KEY_LATEST_ERROR_CODE, mLatestStatusCode);

        super.onSaveInstanceState(outState);
    }

    /**
     * To resume camera decoding task after handshake fail or Wi-Fi connection fail.
     */
    private void restartCamera() {
        if (mCamera == null) {
            Log.d(TAG, "mCamera is not available for restarting camera");
            return;
        }

        if (mCamera.isDecodeTaskAlive()) {
            mCamera.stop();
        }

        final SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        if (surfaceTexture == null) {
            throw new IllegalStateException("SurfaceTexture is not ready for restarting camera");
        }

        mCamera.start(surfaceTexture);
    }

    @VisibleForTesting
    protected boolean isDecodeTaskAlive() {
        return mCamera != null && mCamera.isDecodeTaskAlive();
    }

    @Override
    protected boolean isFooterAvailable() {
        return false;
    }
}

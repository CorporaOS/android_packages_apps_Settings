/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.threadnetwork

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.thread.ThreadNetworkController.STATE_DISABLED
import android.net.thread.ThreadNetworkController.STATE_DISABLING
import android.net.thread.ThreadNetworkController.STATE_ENABLED
import android.net.thread.ThreadNetworkController.StateCallback
import android.net.thread.ThreadNetworkException
import android.os.OutcomeReceiver
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.net.thread.platform.flags.Flags
import com.android.settings.R
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING
import com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE
import com.android.settings.threadnetwork.ThreadNetworkPreferenceController.BaseThreadNetworkController
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import java.util.concurrent.Executor

/** Unit tests for [ThreadNetworkPreferenceController].  */
@RunWith(AndroidJUnit4::class)
class ThreadNetworkPreferenceControllerTest {
    @get:Rule
    val mSetFlagsRule = SetFlagsRule()
    private lateinit var mContext: Context
    private lateinit var mExecutor: Executor
    private lateinit var mController: ThreadNetworkPreferenceController
    private lateinit var mFakeThreadNetworkController: FakeThreadNetworkController
    private lateinit var mPreference: SwitchPreference
    private val mBroadcastReceiverArgumentCaptor = ArgumentCaptor.forClass(
        BroadcastReceiver::class.java
    )

    @Before
    fun setUp() {
        mSetFlagsRule.enableFlags(Flags.FLAG_THREAD_ENABLED_PLATFORM)
        mContext = Mockito.spy(ApplicationProvider.getApplicationContext<Context>())
        mExecutor = ContextCompat.getMainExecutor(mContext)
        mFakeThreadNetworkController = FakeThreadNetworkController(mExecutor)
        mController = newControllerWithThreadFeatureSupported(true)
        val preferenceManager = PreferenceManager(mContext)
        val preferenceScreen = preferenceManager.createPreferenceScreen(mContext)
        mPreference = SwitchPreference(mContext)
        mPreference.key = "thread_network_settings"
        preferenceScreen.addPreference(mPreference)
        mController.displayPreference(preferenceScreen)
    }

    private fun newControllerWithThreadFeatureSupported(
        present: Boolean
    ): ThreadNetworkPreferenceController {
        return ThreadNetworkPreferenceController(
            mContext,
            "thread_network_settings" /* key */,
            mExecutor,
            if (present) mFakeThreadNetworkController else null
        )
    }

    @Test
    fun availabilityStatus_flagDisabled_returnsConditionallyUnavailable() {
        mSetFlagsRule.disableFlags(Flags.FLAG_THREAD_ENABLED_PLATFORM)
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun availabilityStatus_airPlaneModeOn_returnsDisabledDependentSetting() {
        Settings.Global.putInt(mContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
        mController.onStart()

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING)
    }

    @Test
    fun availabilityStatus_airPlaneModeOff_returnsAvailable() {
        Settings.Global.putInt(mContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        mController.onStart()

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE)
    }

    @Test
    fun availabilityStatus_threadFeatureNotSupported_returnsUnsupported() {
        mController = newControllerWithThreadFeatureSupported(false)
        mController.onStart()

        assertThat(mFakeThreadNetworkController.registeredStateCallback).isNull()
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun isChecked_threadSetEnabled_returnsTrue() {
        mFakeThreadNetworkController.setEnabled(true, mExecutor) { }
        mController.onStart()

        assertThat(mController.isChecked).isTrue()
    }

    @Test
    fun isChecked_threadSetDisabled_returnsFalse() {
        mFakeThreadNetworkController.setEnabled(false, mExecutor) { }
        mController.onStart()

        assertThat(mController.isChecked).isFalse()
    }

    @Test
    fun setChecked_setChecked_threadIsEnabled() {
        mController.onStart()

        mController.setChecked(true)

        assertThat(mFakeThreadNetworkController.isEnabled).isTrue()
    }

    @Test
    fun setChecked_setUnchecked_threadIsDisabled() {
        mController.onStart()

        mController.setChecked(false)

        assertThat(mFakeThreadNetworkController.isEnabled).isFalse()
    }

    @Test
    fun updatePreference_airPlaneModeOff_preferenceEnabled() {
        Settings.Global.putInt(mContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        mController.onStart()

        assertThat(mPreference.isEnabled).isTrue()
        assertThat(mPreference.summary).isEqualTo(
            mContext.resources.getString(R.string.thread_network_settings_summary)
        )
    }

    @Test
    fun updatePreference_airPlaneModeOn_preferenceDisabled() {
        Settings.Global.putInt(mContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
        mController.onStart()

        assertThat(mPreference.isEnabled).isFalse()
        assertThat(mPreference.summary).isEqualTo(
            mContext.resources.getString(R.string.thread_network_settings_summary_airplane_mode)
        )
    }

    @Test
    fun updatePreference_airPlaneModeTurnedOn_preferenceDisabled() {
        Settings.Global.putInt(mContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        startControllerAndCaptureCallbacks()

        Settings.Global.putInt(mContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
        mBroadcastReceiverArgumentCaptor.value.onReceive(mContext, Intent())

        assertThat(mPreference.isEnabled).isFalse()
        assertThat(mPreference.summary).isEqualTo(
            mContext.resources.getString(R.string.thread_network_settings_summary_airplane_mode)
        )
    }

    private fun startControllerAndCaptureCallbacks() {
        mController.onStart()
        Mockito.verify(mContext)!!.registerReceiver(
            mBroadcastReceiverArgumentCaptor.capture(), any()
        )
    }

    private class FakeThreadNetworkController(private val mExecutor: Executor) :
        BaseThreadNetworkController {
        var isEnabled = true
            private set
        var registeredStateCallback: StateCallback? = null
            private set

        override fun setEnabled(
            enabled: Boolean,
            executor: Executor,
            receiver: OutcomeReceiver<Void?, ThreadNetworkException>
        ) {
            isEnabled = enabled
            if (registeredStateCallback != null) {
                if (!isEnabled) {
                    mExecutor.execute {
                        registeredStateCallback!!.onThreadEnableStateChanged(
                            STATE_DISABLING
                        )
                    }
                    mExecutor.execute {
                        registeredStateCallback!!.onThreadEnableStateChanged(
                            STATE_DISABLED
                        )
                    }
                } else {
                    mExecutor.execute {
                        registeredStateCallback!!.onThreadEnableStateChanged(
                            STATE_ENABLED
                        )
                    }
                }
            }
            executor.execute { receiver.onResult(null) }
        }

        override fun registerStateCallback(
            executor: Executor,
            callback: StateCallback
        ) {
            require(callback !== registeredStateCallback) { "callback is already registered" }
            registeredStateCallback = callback
            val enabledState =
                if (isEnabled) STATE_ENABLED else STATE_DISABLED
            mExecutor.execute { registeredStateCallback!!.onThreadEnableStateChanged(enabledState) }
        }

        override fun unregisterStateCallback(callback: StateCallback) {
            requireNotNull(registeredStateCallback) { "callback is already unregistered" }
            registeredStateCallback = null
        }
    }
}

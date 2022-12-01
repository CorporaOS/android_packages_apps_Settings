package com.android.settings;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;

public class DummyActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dummy_activity);
    }
}

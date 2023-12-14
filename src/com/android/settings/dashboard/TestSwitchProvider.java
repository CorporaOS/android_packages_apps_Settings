package com.android.settings.dashboard;

import static com.android.settingslib.drawer.EntriesProvider.EXTRA_ENTRY_DATA;
import static com.android.settingslib.drawer.EntriesProvider.EXTRA_SWITCH_CHECKED_STATE;
import static com.android.settingslib.drawer.EntriesProvider.METHOD_GET_ENTRY_DATA;
import static com.android.settingslib.drawer.EntriesProvider.METHOD_IS_CHECKED;
import static com.android.settingslib.drawer.EntriesProvider.METHOD_ON_CHECKED_CHANGED;
import static com.android.settingslib.drawer.TileUtils.META_DATA_KEY_ORDER;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_KEYHINT;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SUMMARY;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SWITCH_URI;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_TITLE;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import com.android.settings.R;

public class TestSwitchProvider extends ContentProvider {

    private static final String MY_TEST_SWITCH = "my_test_switch";

    private boolean mIsChecked = false;

    @Override
    public Bundle call(String method, String uri, Bundle extras) {
        final Bundle bundle = new Bundle();
        switch (method) {
            case METHOD_GET_ENTRY_DATA:
                ArrayList<Bundle> entryData = new ArrayList<Bundle>();
                Bundle metaData = new Bundle();
                metaData.putString("com.android.settings.category",
                        "com.android.settings.category.ia.connect");
                metaData.putString(META_DATA_PREFERENCE_KEYHINT, MY_TEST_SWITCH);
                metaData.putString(META_DATA_PREFERENCE_TITLE, "My test switch");
                metaData.putString(META_DATA_PREFERENCE_SUMMARY, "Testing Switch injection");
                metaData.putString(META_DATA_PREFERENCE_SWITCH_URI,
                        "content://com.android.settings.dashboard.TestSwitchProvider");
                metaData.putInt(META_DATA_KEY_ORDER, -100);
                entryData.add(metaData);
                bundle.putParcelableArrayList(EXTRA_ENTRY_DATA, entryData);
                break;
            case METHOD_IS_CHECKED:
                bundle.putBoolean(EXTRA_SWITCH_CHECKED_STATE, mIsChecked);
                break;
            case METHOD_ON_CHECKED_CHANGED:
                mIsChecked = extras.getBoolean(EXTRA_SWITCH_CHECKED_STATE);
                break;
            default:
                throw new IllegalArgumentException("Unknown Uri format: " + uri);
        }
        return bundle;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}

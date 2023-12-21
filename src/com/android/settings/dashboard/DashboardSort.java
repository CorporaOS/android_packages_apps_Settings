/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.settings.dashboard;

import static com.android.settingslib.drawer.TileUtils.META_DATA_POSITION_AFTER;
import static com.android.settingslib.drawer.TileUtils.META_DATA_POSITION_BEFORE;
import static com.android.settingslib.drawer.TileUtils.POSITION_FIRST;
import static com.android.settingslib.drawer.TileUtils.POSITION_LAST;
import static com.android.settings.dashboard.DashboardFragment.TILE_METADATA_EXTRA;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import java.util.ArrayList;

public class DashboardSort {
    private static final String LOG_TAG = "DashboardSort";
    private static final boolean DEBUG = false;

    private static final int SEPARATION = 10;
    private static final int BEFORE = -1;
    private static final int AFTER = 1;
    private static final int NOT_FOUND = Integer.MAX_VALUE;

    static void sortPreferences(@NonNull PreferenceGroup group) {
        if (DEBUG) {
            debugListPrefs(group, "Sort begin");
        }

        // Create a list of all preferences that need to be sorted in this group. If this group
        // contains a sub-group then recursively reorder that sub-group.
        final ArrayList<Preference> preferencesToBeSorted = new ArrayList<Preference>();
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            final Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                // PreferenceGroup, check if the group itself needs to be reordered within its
                // parent group
                if (needsSorting(pref)) {
                    preferencesToBeSorted.add(pref);
                }
                // Recurse over the group's preferences
                sortPreferences((PreferenceGroup) pref);
            } else {
                // Preference, check if the preference needs to be reordered within its parent group
                if (needsSorting(pref)) {
                    preferencesToBeSorted.add(pref);
                }
            }
        }

        if (preferencesToBeSorted.isEmpty()) {
            // Move along, nothing to sort here
            if (DEBUG) {
                Log.d(LOG_TAG, "Nothing to sort");
            }
            return;
        }

        // Equalize the separation between the preferences' orders
        equalizeOrderSeparations(group);

        // Reorder the preferences
        ArrayList<Preference> unorderedPrefs = reorderPreferences(group, preferencesToBeSorted);

        // Sanity check
        if (!unorderedPrefs.isEmpty()) {
            // One or more preferences failed to be reordered. Move these to the bottom of the group
            Log.w(LOG_TAG, "Failed to reorder all preferences");
            for (Preference pref : unorderedPrefs) {
                int bottomOrder = getTargetOrder(group, POSITION_LAST);
                reorder(group, pref, bottomOrder, POSITION_LAST, AFTER);
                Log.w(LOG_TAG, " Reordering " + pref.getKey() + " to the bottom, after " +
                        group.getPreference(group.getPreferenceCount() - 1).getKey());
            }
        }

        if (DEBUG) {
            debugListPrefs(group, "Sort done");
        }
    }

    /**
     * Reorder preferences according to the preferences' metadata ({@link #META_DATA_POSITION_AFTER}
     * and {@link #META_DATA_POSITION_BEFORE}).
     *
     * @param group The PreferenceGroup to reorder into
     * @param prefs The preferences to be reordered
     * @return Any preferences which failed to be reordered
     */
    private static @NonNull ArrayList<Preference> reorderPreferences(@NonNull PreferenceGroup group,
            @NonNull ArrayList<Preference> prefs) {
        final ArrayList<Preference> unordered = new ArrayList(prefs);
        for (Preference pref : prefs) {
            final String afterKey = getTargetKeyAfter(pref);
            final String beforeKey = getTargetKeyBefore(pref);

            if (!TextUtils.isEmpty(afterKey) && !TextUtils.isEmpty(beforeKey)) {
                Log.w(LOG_TAG, pref.getKey() + " has both a 'before' and an 'after' position " +
                        "which isn't supported. Using the 'after' position");
            }

            int targetOrder;
            if (!TextUtils.isEmpty(afterKey)) {
                targetOrder = getTargetOrder(group, afterKey);
                if (targetOrder != NOT_FOUND) {
                    reorder(group, pref, targetOrder, afterKey, AFTER);
                    unordered.remove(pref);
                }
            } else if (!TextUtils.isEmpty(beforeKey)) {
                targetOrder = getTargetOrder(group, beforeKey);
                if (targetOrder != NOT_FOUND) {
                    reorder(group, pref, targetOrder, beforeKey, BEFORE);
                    unordered.remove(pref);
                }
            } else if (TextUtils.isEmpty(afterKey) && TextUtils.isEmpty(beforeKey)) {
                Log.w(LOG_TAG, pref.getKey() + " has no before/after position");
            }
        }

        return unordered;
    }

    /**
     * Helper to reorder a Preference before or after a specified position (order).
     *
     * @param group The PreferenceGroup of the Preference
     * @param pref The Preference to reorder
     * @param targetOrder The position (order) of the preference before or after which to
     *                    position the Preference
     * @param targetOrder The key of the preference before or after which to position the Preference
     * @param shift {@link #AFTER} (1) to reorder after, {@link #BEFORE} (-1) to reorder before
     */
    private static void reorder(@NonNull PreferenceGroup group, @NonNull Preference pref,
            int targetOrder, String targetKey, int shift) {
        int newOrder = targetOrder + shift;
        if (DEBUG) {
            Log.d(LOG_TAG, " Reorder '" + pref.getKey() +
                    (shift == 1 ? "' after '" : "' before '") + targetKey + "' (order " +
                    targetOrder + ") -> " + newOrder);
        }

        // The pref must be removed and then added for the reorder to register
        group.removePreference(pref);
        pref.setOrder(newOrder);
        group.addPreference(pref);
        equalizeOrderSeparations(group);
    }

    /**
     * Reorder so that all orders are equally separated, without changing the relative order of the
     * preferences.
     */
    private static void equalizeOrderSeparations(@NonNull PreferenceGroup group) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            final Preference pref = group.getPreference(i);
            if (pref != null) {
                pref.setOrder(i * SEPARATION);
                if (pref instanceof PreferenceGroup) {
                    equalizeOrderSeparations((PreferenceGroup) pref);
                }
            }
        }
    }

    /**
     * Helper to check if sorting is necessary
     *
     * @param pref preference
     * @return if sorting is necessary
     */
    private static boolean needsSorting(@NonNull Preference pref) {
        return getTargetKeyBefore(pref) != null || getTargetKeyAfter(pref) != null;
    }

    /**
     * Helper to find the target key referenced by META_DATA_POSITION_BEFORE
     *
     * @param pref preference
     * @return target key or null
     */
    private static @Nullable String getTargetKeyBefore(@NonNull Preference pref) {
        final Bundle extras = pref.peekExtras();
        if (extras == null) {
            return null;
        }
        final Bundle metaData = extras.getBundle(TILE_METADATA_EXTRA);
        return metaData != null ? metaData.getString(META_DATA_POSITION_BEFORE) : null;
    }

    /**
     * Helper to find the target key referenced by META_DATA_POSITION_AFTER
     *
     * @param pref preference
     * @return target key or null
     */
    private static @Nullable String getTargetKeyAfter(@NonNull Preference pref) {
        final Bundle extras = pref.peekExtras();
        if (extras == null) {
            return null;
        }
        final Bundle metaData = extras.getBundle(TILE_METADATA_EXTRA);
        return metaData != null ? metaData.getString(META_DATA_POSITION_AFTER) : null;
    }

    /**
     * Helper to find the position (order) of a preference with a specified key.
     *
     * @param group the PreferenceGroup of the preference
     * @param targetKey the key of the Preference to find, or {@link #POSITION_FIRST} or
     *                  {@link #POSITION_LAST} to find the first or last preference in the group
     */
    private static int getTargetOrder(@NonNull PreferenceGroup group, @NonNull String targetKey) {
        Preference targetPref;
        if (TextUtils.isEmpty(targetKey) || POSITION_LAST.equals(targetKey)) {
            // The last preference in the group
            targetPref = group.getPreference(group.getPreferenceCount() - 1);
        } else if (POSITION_FIRST.equals(targetKey)) {
            // The first preference in the group
            targetPref = group.getPreference(0);
        } else {
            targetPref = group.findPreference(targetKey);
        }
        return targetPref != null ? targetPref.getOrder() : NOT_FOUND;
    }

    /**
     * Helper to list all Preferences in a specified group, for debugging
     */
    private static void debugListPrefs(@NonNull PreferenceGroup group, @NonNull String tag) {
        Log.d(LOG_TAG, tag);
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                debugListPrefs((PreferenceGroup) pref, " PreferenceGroup - '" + pref.getKey() +
                        "', title '" + pref.getTitle() + "', order: " + pref.getOrder());
            } else {
                Log.d(LOG_TAG, "  Preference - " + pref.getKey() + ", title '" + pref.getTitle() +
                        "', order: " + pref.getOrder());
            }
        }
    }
}

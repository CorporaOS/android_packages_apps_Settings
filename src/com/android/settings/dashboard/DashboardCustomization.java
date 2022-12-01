/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.settingslib.drawer.TileUtils.META_DATA_CUSTOMIZATION_MOVE_TARGET;
import static com.android.settingslib.drawer.TileUtils.META_DATA_CUSTOMIZATION_REMOVE_TARGET;
import static com.android.settingslib.drawer.TileUtils.META_DATA_CUSTOMIZATION_TYPE;
import static com.android.settingslib.drawer.TileUtils.META_DATA_FIRST_POSITION;
import static com.android.settingslib.drawer.TileUtils.META_DATA_LAST_POSITION;
import static com.android.settingslib.drawer.TileUtils.META_DATA_POSITION_AFTER;
import static com.android.settingslib.drawer.TileUtils.META_DATA_POSITION_BEFORE;
import static com.android.settingslib.drawer.TileUtils.MOVE;
import static com.android.settingslib.drawer.TileUtils.REMOVE;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;

public class DashboardCustomization {
    private static final String LOG_TAG = "DashboardCustomization";
    private static final boolean DEBUG = false;

    private static final int SEPARATION = 10;
    private static final int BEFORE = -1;
    private static final int AFTER = 1;

    static void customizePreferences(@NonNull PreferenceScreen screen) {
        final ArrayList<Preference> toBeSorted = new ArrayList<Preference>();
        final ArrayList<Preference> toBeRemoved = new ArrayList<Preference>();
        final ArrayList<Preference> noLongerNeeded = new ArrayList<Preference>();

        if (DEBUG) {
            debugListPrefs(screen, "Customization start");
        }

        // Determine which tiles need sorting and which tiles are customization tiles (move/remove
        // customization)
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            final Preference pref = screen.getPreference(i);

            // Collect all Preferences that need sorting/moving
            if (needsSorting(pref)) {
                if (needsMoving(pref)) {
                    // A Move customization should move its target Preference instead of itself.
                    // Add the Move target preference to the list of preferences to be sorted,
                    // adding the Move customization metadata to the target Preference.
                    final Preference moveTargetPref = findTarget(screen, getMoveTarget(pref));
                    if (moveTargetPref != null) {
                        if (DEBUG) {
                            Log.d(LOG_TAG, pref.getKey() + " wants to move " +
                                    moveTargetPref.getKey());
                        }
                        copyMetaData(pref, moveTargetPref);
                        toBeSorted.add(moveTargetPref);
                        // Remove the Move customization, it's just a placeholder and we don't need
                        // it anymore. Can't remove it inside this loop though.
                        noLongerNeeded.add(pref);
                    }
                } else {
                    // A "regular" Preference (i.e Activity tile)
                    toBeSorted.add(pref);
                }
            } else if (needsRemoving(pref)) {
                // Collect all Preferences that are customized for removal
                toBeRemoved.add(pref);
            }
        }

        if (toBeSorted.size() == 0 && toBeRemoved.size() == 0 && noLongerNeeded.size() == 0) {
            // No customization required
            if (DEBUG) {
                debugListPrefs(screen, "No customizations");
            }

            return;
        }

        // Remove tiles to be sorted/moved, they'll be re-added later in {@link insertPreference}
        for (Preference pref : toBeSorted) {
            PreferenceGroup parent = getParentOfKey(screen, pref.getKey());
            parent.removePreference(pref);
        }
        // Remove customization tiles specifying removal of a preference, they are just placeholders
        for (Preference pref : toBeRemoved) {
            screen.removePreference(pref);
        }
        // Remove customization tiles that have been handled
        for (Preference pref : noLongerNeeded) {
            screen.removePreference(pref);
        }

        // Equalize the separation between the preferences orders
        equalizeOrderSeparations(screen);

        // Iterate over the items to be inserted and reorder. Iteration may be needed in case items
        // to be inserted have dependencies to each other
        if (toBeSorted.size() > 0) {
            int maxIterationCount = toBeSorted.size() - 1; // The worst-case scenario
            ArrayList<Preference> unsorted = insertPreferences(screen, toBeSorted); // Initial insertion

            // Iterate if neccessary
            int iterationCount = 0;
            while (unsorted.size() > 0 && iterationCount++ < maxIterationCount) {
                unsorted = insertPreferences(screen, unsorted);
            }

            if (DEBUG) {
                debugListPrefs(screen, "After sorting");
            }

            // Sanity check
            if (unsorted.size() > 0) {
                Log.w(LOG_TAG, "Failed to customize all preferences");
                // One or more Preferences failed to be sorted (customized to be sorted before/after
                // Preferences which failed dependency/entitlement and are thus not added). Add
                // these Preferences to the bottom.
                for (Preference pref : unsorted) {
                    Preference bottomPref = screen.getPreference(screen.getPreferenceCount() - 1);
                    insert(screen, pref, screen.getPreference(0), AFTER);
                    Log.w(LOG_TAG, " Inserting " + pref.getKey() + " at the bottom, after " +
                            bottomPref.getKey());
                }
            }
        }

        // Remove preferences customized for removal. Do this last since other customizations may be
        // dependent on preferences designated for removal
        if (toBeRemoved.size() > 0) {
            for (Preference pref : toBeRemoved) {
                final String removalTargetKey = getRemoveTarget(pref);
                final PreferenceGroup parent = getParentOfKey(screen, removalTargetKey);
                if (parent != null) {
                    if (DEBUG) {
                        Log.d(LOG_TAG, pref.getKey() + " wants to remove " + removalTargetKey +
                                " from parent " + parent.getKey());
                    }
                    final Preference remove = findTarget(parent, removalTargetKey);
                    if (remove != null) {
                        if (DEBUG) {
                            Log.d(LOG_TAG, "Removing target for key: " + removalTargetKey + " = " +
                                remove.getKey());
                        }
                        parent.removePreference(remove);
                    } else {
                        // Sanity check
                        Log.w(LOG_TAG, "Couldn't find removal target for key: " + removalTargetKey);
                    }
                } else {
                    Log.w(LOG_TAG, "Couldn't find parent of removal target for key: " +
                            removalTargetKey);
                }
            }
        }

        if (DEBUG) {
            debugListPrefs(screen, "Customization done");
        }
    }

    /**
     * Reorder so that all orders are equally separated, without changing the relative order of the
     * preferences. Recursively reorder any PreferenceGroups.
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
     * Insert preferences according to the preferences' metadata ({@link META_DATA_INJECT_AFTER} and
     * {@link META_DATA_INJECT_BEFORE}).
     *
     * @param group The PreferenceGroup to insert into
     * @param prefs The preferences to be inserted
     * @return The preferences which failed to be inserted (requiring iteration)
     */
    private static @NonNull ArrayList<Preference> insertPreferences(@NonNull PreferenceGroup group,
            @NonNull ArrayList<Preference> prefs) {
        final ArrayList<Preference> uninserted = new ArrayList(prefs);
        for (Preference pref: prefs) {
            final String afterKey = getTargetKeyBeforeOrAfter(pref, META_DATA_POSITION_AFTER);
            final String beforeKey = getTargetKeyBeforeOrAfter(pref, META_DATA_POSITION_BEFORE);
            Preference destinationPref;
            if (!TextUtils.isEmpty(afterKey)) {
                destinationPref = findAfter(group, afterKey);
                if (destinationPref != null) {
                    insert(group, pref, destinationPref, AFTER);
                    uninserted.remove(pref);
                }
            } else if (!TextUtils.isEmpty(beforeKey)) {
                destinationPref = findBefore(group, beforeKey);
                if (destinationPref != null) {
                    insert(group, pref, destinationPref, BEFORE);
                    uninserted.remove(pref);
                }
            } else if (TextUtils.isEmpty(afterKey) && TextUtils.isEmpty(beforeKey)) {
                Log.w(LOG_TAG, pref.getKey() + " has no before/after position");
            }

            // Injecting a pref with both before and after params is not supported
        }

        return uninserted;
    }

    /**
     * Helper to insert a Preference before or after a specified Preference.
     *
     * @param group The PreferenceGroup of the destination Preference
     * @param pref The Preference to insert
     * @param destinationPref The preference before or after which to insert the Preference
     * @param shift {@link #AFTER} (1) to insert after, {@link #BEFORE} (-1) to insert before
     */
    private static void insert(@NonNull PreferenceGroup group, @NonNull Preference pref,
            Preference destinationPref, int shift) {
        PreferenceGroup destinationParent;
        int destinationOrder;
        if (destinationPref instanceof PreferenceGroup && shift == AFTER) {
            // If the destinationPref is a PreferenceGroup and we are placing it AFTER, then the
            // pref shall be placed as the first pref within the destinationPref
            destinationParent = (PreferenceGroup) destinationPref;
            if (destinationParent.getPreferenceCount() > 0) {
                destinationOrder = destinationParent.getPreference(0).getOrder() - 1;
            } else {
                destinationOrder = -1;
            }
        } else {
            // Place the pref within the destinationPref's parent, before or after the
            // destinationPref
            destinationParent = getParentOfKey(group, destinationPref.getKey());
            destinationOrder = destinationPref.getOrder();
        }
        if (DEBUG) {
            Log.d(LOG_TAG, " Insert " + pref.getKey() + (shift == 1 ? " after " : " before ") +
                    destinationPref.getKey() + " at pos " + (destinationOrder + shift));
        }
        pref.setOrder(destinationOrder + shift);
        destinationParent.addPreference(pref);
        equalizeOrderSeparations(destinationParent);
    }

    /**
     * Helper to check if sorting is necessary
     *
     * @param pref preference
     * @return if sorting is necessary
     */
    private static boolean needsSorting(@NonNull Preference pref) {
        // Sorting is deemed necessary if a before or after position has been provided. Ignore
        // Remove customizations which have mistakenly added a position
        return (getTargetKeyBeforeOrAfter(pref, META_DATA_POSITION_AFTER) != null ||
                getTargetKeyBeforeOrAfter(pref, META_DATA_POSITION_BEFORE) != null) &&
                !needsRemoving(pref);
    }

    /**
     * Helper to find the target key referenced by settings.position.after or
     * settings.position.before
     *
     * @param pref preference
     * @param relativePos META_DATA_POSITION_AFTER to find the key after pref,
     *                    META_DATA_POSITION_BEFORE to find the key before pref
     * @return target key or null
     */
    private static @Nullable String getTargetKeyBeforeOrAfter(@NonNull Preference pref,
            String relativePos) {
        final Bundle extras = pref.peekExtras();
        if (extras == null) {
            return null;
        }
        final Bundle metaData = extras.getBundle(DashboardFragment.TILE_METADATA_EXTRA);
        return metaData != null ? metaData.getString(relativePos) : null;
    }

    /**
     * Helper to find the preference referenced by settings.position.after
     *
     * @param group parent preference
     * @param targetKey preference to find
     * @return preference or null
     */
    private static @Nullable Preference findAfter(@NonNull PreferenceGroup group,
            @NonNull String targetKey) {
        if (META_DATA_LAST_POSITION.equals(targetKey)) {
            return group.getPreference(group.getPreferenceCount() - 1);
        }
        return (!TextUtils.isEmpty(targetKey)) ? group.findPreference(targetKey) : null;
    }

    /**
     * Helper to find the preference referenced by settings.position.before
     *
     * @param group parent preference
     * @param targetKey preference to find
     * @return preference or null
     */
    private static @Nullable Preference findBefore(@NonNull PreferenceGroup group,
            String targetKey) {
        if (META_DATA_FIRST_POSITION.equals(targetKey)) {
            return group.getPreference(0);
        }
        return (!TextUtils.isEmpty(targetKey)) ? group.findPreference(targetKey) : null;
    }

    /**
     * Helper to check if removal is necessary, i.e if this is a Remove customization
     *
     * @param pref preference
     * @return if removal is necessary
     */
    private static boolean needsRemoving(@NonNull Preference pref) {
        return getRemoveTarget(pref) != null;
    }

    /**
     * Helper to find the target key referenced by settings.customization.type.remove.targetKey
     *
     * @param pref preference
     * @return target key or null
     */
    private static @Nullable String getRemoveTarget(@NonNull Preference pref) {
        final Bundle extras = pref.peekExtras();
        if (extras == null) {
            return null;
        }
        final Bundle metaData = extras.getBundle(DashboardFragment.TILE_METADATA_EXTRA);
        if (metaData == null) {
            return null;
        }
        if (!REMOVE.equals(metaData.getString(META_DATA_CUSTOMIZATION_TYPE))) {
            return null;
        }
        return metaData.getString(META_DATA_CUSTOMIZATION_REMOVE_TARGET);
    }

    /**
     * Helper to find the preference referenced by settings.customization.type.X.targetKey
     *
     * @param group parent preference
     * @param targetKey preference to find
     * @return preference or null
     */
    private static @Nullable Preference findTarget(@NonNull PreferenceGroup group,
            @NonNull String targetKey) {
        return (!TextUtils.isEmpty(targetKey)) ? group.findPreference(targetKey) : null;
    }

    /**
     * Helper to check if moving is necessary, i.e if this is a Move customization
     *
     * @param pref preference
     * @return if moving is necessary
     */
    private static boolean needsMoving(@NonNull Preference pref) {
        return (getMoveTarget(pref) != null);
    }

    /**
     * Helper to find the target key referenced by settings.customization.type.move.targetKey
     *
     * @param pref preference
     * @return target key or null
     */
    private static @Nullable String getMoveTarget(@NonNull Preference pref) {
        final Bundle extras = pref.peekExtras();
        if (extras == null) {
            return null;
        }
        final Bundle metaData = extras.getBundle(DashboardFragment.TILE_METADATA_EXTRA);
        if (metaData == null) {
            return null;
        }
        if (!MOVE.equals(metaData.getString(META_DATA_CUSTOMIZATION_TYPE))) {
            return null;
        }
        return metaData.getString(META_DATA_CUSTOMIZATION_MOVE_TARGET);
    }

    /**
     * Recursively finds the first occurrence of a Preference with key targetKey within
     * the tree that starts with root, and returns the parent PreferenceGroup of
     * that Preference.
     *
     * @param root The PreferenceGroup within which to look for targetKey
     * @param targetKey The key of the Preference to look for
     * @return the PreferenceGroup that is the parent of the Preference with key targetKey
     */
    private static @Nullable PreferenceGroup getParentOfKey(PreferenceGroup root,
            String targetKey) {
        if (root == null || targetKey == null) return null;
        for (int i = 0, count = root.getPreferenceCount(); i < count ; i++) {
            Preference pref = root.getPreference(i);
            if (targetKey.equals(pref.getKey())) {
                return root;
            } else if (pref instanceof PreferenceGroup) {
                PreferenceGroup parent = getParentOfKey((PreferenceGroup)pref, targetKey);
                // If we found the parent, return it, otherwise let the search continue
                if (parent != null) {
                    return parent;
                }
            }
        }
        // targetKey not found in this node
        return null;
    }

    /**
     * Helper to copy the metadata from one Preference to another.
     *
     * @param sourcePref The Preference from which to copy metadata
     * @param destPref Thge Preference to which to copy metadata
     */
    private static void copyMetaData(@NonNull Preference sourcePref, @NonNull Preference destPref) {
        final Bundle sourceExtras = sourcePref.peekExtras();
        if (sourceExtras != null) {
            final Bundle sourceMetaData = sourceExtras.getBundle(
                    DashboardFragment.TILE_METADATA_EXTRA);
            if (sourceMetaData != null) {
                Bundle destExtras = destPref.getExtras();
                destExtras.putBundle(DashboardFragment.TILE_METADATA_EXTRA, sourceMetaData);
            }
        }
    }

    /**
     * Helper to list all Preferences in the specified group, for debugging
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

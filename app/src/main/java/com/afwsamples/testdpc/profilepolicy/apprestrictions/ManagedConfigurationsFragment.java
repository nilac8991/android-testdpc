/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.afwsamples.testdpc.profilepolicy.apprestrictions;

import static com.afwsamples.testdpc.common.keyvaluepair.KeyValuePairDialogFragment.RESULT_VALUE;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.content.RestrictionsManager;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.afwsamples.testdpc.R;
import com.afwsamples.testdpc.common.EditDeleteArrayAdapter;
import com.afwsamples.testdpc.common.ManageAppFragment;
import com.afwsamples.testdpc.common.RestrictionManagerCompat;
import com.afwsamples.testdpc.common.Util;
import com.afwsamples.testdpc.common.keyvaluepair.KeyValuePairDialogFragment;
import com.afwsamples.testdpc.models.RestrictionBatchEntry;
import com.afwsamples.testdpc.util.ManagedConfigurationsBatchUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * This fragment shows all installed apps and allows viewing and editing application restrictions
 * for those apps. It also allows loading the default app restrictions for each of those apps.
 */
public class ManagedConfigurationsFragment extends ManageAppFragment
        implements EditDeleteArrayAdapter.OnEditButtonClickListener<RestrictionEntry> {

    private final static int ACTION_EXPORT_BATCH_FILE = 3341;
    private final static int ACTION_IMPORT_BATCH_FILE = 3342;

    private List<RestrictionEntry> mRestrictionEntries = new ArrayList<>();
    private List<RestrictionEntry> mLastRestrictionEntries;
    private DevicePolicyManager mDevicePolicyManager;
    private RestrictionsManager mRestrictionsManager;
    private EditDeleteArrayAdapter<RestrictionEntry> mAppRestrictionsArrayAdapter;
    private RestrictionEntry mEditingRestrictionEntry;
    private ComponentName mAdminComponent;

    private static final int RESULT_CODE_EDIT_DIALOG = 1;

    private static final int[] SUPPORTED_TYPES = {
            KeyValuePairDialogFragment.DialogType.BOOL_TYPE,
            KeyValuePairDialogFragment.DialogType.INT_TYPE,
            KeyValuePairDialogFragment.DialogType.STRING_TYPE,
            KeyValuePairDialogFragment.DialogType.STRING_ARRAY_TYPE,
            KeyValuePairDialogFragment.DialogType.BUNDLE_TYPE,
            KeyValuePairDialogFragment.DialogType.BUNDLE_ARRAY_TYPE
    };
    private static final int[] SUPPORTED_TYPES_PRE_M = {
            KeyValuePairDialogFragment.DialogType.BOOL_TYPE,
            KeyValuePairDialogFragment.DialogType.INT_TYPE,
            KeyValuePairDialogFragment.DialogType.STRING_TYPE,
            KeyValuePairDialogFragment.DialogType.STRING_ARRAY_TYPE,
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mDevicePolicyManager =
                (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        mRestrictionsManager =
                (RestrictionsManager) getActivity().getSystemService(Context.RESTRICTIONS_SERVICE);
        if (Util.hasDelegation(getActivity(), DevicePolicyManager.DELEGATION_APP_RESTRICTIONS)) {
            mAdminComponent = null;
        } else {
            mAdminComponent = DeviceAdminReceiver.getComponentName(getActivity());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setTitle(R.string.managed_configurations);
    }

    protected void loadAppRestrictionsList(RestrictionEntry[] restrictionEntries) {
        if (restrictionEntries != null) {
            mAppRestrictionsArrayAdapter.clear();
            mAppRestrictionsArrayAdapter.addAll(Arrays.asList(restrictionEntries));
        }
    }

    protected RestrictionEntry[] convertBundleToRestrictions(Bundle restrictionBundle) {
        List<RestrictionEntry> restrictionEntries = new ArrayList<>();
        Set<String> keys = restrictionBundle.keySet();
        for (String key : keys) {
            Object value = restrictionBundle.get(key);
            if (value instanceof Boolean) {
                restrictionEntries.add(new RestrictionEntry(key, (boolean) value));
            } else if (value instanceof Integer) {
                restrictionEntries.add(new RestrictionEntry(key, (int) value));
            } else if (value instanceof String) {
                RestrictionEntry entry = new RestrictionEntry(RestrictionEntry.TYPE_STRING, key);
                entry.setSelectedString((String) value);
                restrictionEntries.add(entry);
            } else if (value instanceof String[]) {
                restrictionEntries.add(new RestrictionEntry(key, (String[]) value));
            } else if (value instanceof Bundle) {
                addBundleEntryToRestrictions(restrictionEntries, key, (Bundle) value);
            } else if (value instanceof Parcelable[]) {
                addBundleArrayToRestrictions(restrictionEntries, key, (Parcelable[]) value);
            }
        }
        return restrictionEntries.toArray(new RestrictionEntry[0]);
    }

    @TargetApi(VERSION_CODES.M)
    private void addBundleEntryToRestrictions(
            List<RestrictionEntry> restrictionEntries, String key, Bundle value) {
        restrictionEntries.add(
                RestrictionEntry.createBundleEntry(key, convertBundleToRestrictions(value)));
    }

    @TargetApi(VERSION_CODES.M)
    private void addBundleArrayToRestrictions(
            List<RestrictionEntry> restrictionEntries, String key, Parcelable[] value) {
        int length = value.length;
        RestrictionEntry[] entriesArray = new RestrictionEntry[length];
        for (int i = 0; i < entriesArray.length; ++i) {
            entriesArray[i] =
                    RestrictionEntry.createBundleEntry(key, convertBundleToRestrictions((Bundle) value[i]));
        }
        restrictionEntries.add(RestrictionEntry.createBundleArrayEntry(key, entriesArray));
    }

    protected void showToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditButtonClick(RestrictionEntry restrictionEntry) {
        showEditDialog(restrictionEntry);
    }

    private void showEditDialog(final RestrictionEntry restrictionEntry) {
        mEditingRestrictionEntry = restrictionEntry;
        int type = KeyValuePairDialogFragment.DialogType.BOOL_TYPE;
        Object value = null;
        String key = "";
        if (restrictionEntry != null) {
            key = restrictionEntry.getKey();
            type = getTypeIndexFromRestrictionType(restrictionEntry.getType());
            switch (restrictionEntry.getType()) {
                case RestrictionEntry.TYPE_BOOLEAN:
                    value = restrictionEntry.getSelectedState();
                    break;
                case RestrictionEntry.TYPE_INTEGER:
                    value = restrictionEntry.getIntValue();
                    break;
                case RestrictionEntry.TYPE_STRING:
                    value = restrictionEntry.getSelectedString();
                    break;
                case RestrictionEntry.TYPE_MULTI_SELECT:
                    value = restrictionEntry.getAllSelectedStrings();
                    break;
                case RestrictionEntry.TYPE_BUNDLE:
                    value =
                            RestrictionManagerCompat.convertRestrictionsToBundle(
                                    Arrays.asList(getRestrictionEntries(restrictionEntry)));
                    break;
                case RestrictionEntry.TYPE_BUNDLE_ARRAY:
                    RestrictionEntry[] restrictionEntries = getRestrictionEntries(restrictionEntry);
                    Bundle[] bundles = new Bundle[restrictionEntries.length];
                    for (int i = 0; i < restrictionEntries.length; i++) {
                        bundles[i] =
                                RestrictionManagerCompat.convertRestrictionsToBundle(
                                        Arrays.asList(getRestrictionEntries(restrictionEntries[i])));
                    }
                    value = bundles;
                    break;
            }
        }
        int[] supportType = (Util.SDK_INT < VERSION_CODES.M) ? SUPPORTED_TYPES_PRE_M : SUPPORTED_TYPES;
        KeyValuePairDialogFragment dialogFragment =
                KeyValuePairDialogFragment.newInstance(
                        type, true, key, restrictionEntry.getDescription() != null ? restrictionEntry.getDescription() : "", value, supportType, getCurrentAppName());
        dialogFragment.setTargetFragment(this, RESULT_CODE_EDIT_DIALOG);
        dialogFragment.show(getFragmentManager(), "dialog");
    }

    private int getTypeIndexFromRestrictionType(int restrictionType) {
        switch (restrictionType) {
            case RestrictionEntry.TYPE_BOOLEAN:
                return KeyValuePairDialogFragment.DialogType.BOOL_TYPE;
            case RestrictionEntry.TYPE_INTEGER:
                return KeyValuePairDialogFragment.DialogType.INT_TYPE;
            case RestrictionEntry.TYPE_STRING:
                return KeyValuePairDialogFragment.DialogType.STRING_TYPE;
            case RestrictionEntry.TYPE_MULTI_SELECT:
                return KeyValuePairDialogFragment.DialogType.STRING_ARRAY_TYPE;
            case RestrictionEntry.TYPE_BUNDLE:
                return KeyValuePairDialogFragment.DialogType.BUNDLE_TYPE;
            case RestrictionEntry.TYPE_BUNDLE_ARRAY:
                return KeyValuePairDialogFragment.DialogType.BUNDLE_ARRAY_TYPE;
            default:
                throw new AssertionError("Unknown restriction type");
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        final RestrictionEntry updatedRestrictionEntry = mEditingRestrictionEntry;

        if (requestCode == RESULT_CODE_EDIT_DIALOG) {
            updateRestrictionEntryFromResultIntent(updatedRestrictionEntry, result);
            mAppRestrictionsArrayAdapter.remove(mEditingRestrictionEntry);

            mEditingRestrictionEntry = null;
            mAppRestrictionsArrayAdapter.add(updatedRestrictionEntry);
        } else if (requestCode == ACTION_EXPORT_BATCH_FILE) {
            final Uri selectedFile = result.getData();

            Toast.makeText(getContext(),
                    "Exporting Managed Configurations for " + getCurrentAppName(),
                    Toast.LENGTH_LONG).show();

            ManagedConfigurationsBatchUtil.INSTANCE.exportToJsonFile(
                    selectedFile,
                    getContext(),
                    mRestrictionEntries,
                    new ManagedConfigurationsBatchUtil.ProcessStateCallBack() {
                        @Override
                        public void onFinished(@NonNull List<RestrictionEntry> list) {
                            //Nothing to see here
                        }

                        @Override
                        public void onFinished() {
                            Toast.makeText(getContext(),
                                    "Export successfully completed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        } else if (requestCode == ACTION_IMPORT_BATCH_FILE) {
            //Clean first the old entries
            mAppRestrictionsArrayAdapter.clear();
            mLastRestrictionEntries.clear();
            mRestrictionEntries.clear();

            final Uri selectedFile = result.getData();

            Toast.makeText(getContext(),
                    "Importing Managed Configurations for " + getCurrentAppName(),
                    Toast.LENGTH_LONG).show();

            ManagedConfigurationsBatchUtil.INSTANCE.importFromJsonFile(
                    selectedFile,
                    getContext(),
                    new ManagedConfigurationsBatchUtil.ProcessStateCallBack() {
                        @Override
                        public void onFinished() {
                            //Nothing to see here
                        }

                        @Override
                        public void onFinished(@NonNull List<RestrictionEntry> list) {
                            RestrictionManagerCompat.convertTypeChoiceAndNullToString(list);
                            loadAppRestrictionsList(list.toArray(new RestrictionEntry[0]));

                            Toast.makeText(getContext(),
                                    "Import successfully completed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }
    }

    // TYPE_BUNDLE and TYPE_BUNDLE_ARRAY are only supported from M onward. It is blocked in the
    // UI side.
    @TargetApi(VERSION_CODES.M)
    private void updateRestrictionEntryFromResultIntent(
            RestrictionEntry restrictionEntry, Intent intent) {
        switch (restrictionEntry.getType()) {
            case RestrictionEntry.TYPE_BOOLEAN:
                restrictionEntry.setSelectedState(intent.getBooleanExtra(RESULT_VALUE, false));
                break;
            case RestrictionEntry.TYPE_INTEGER:
                restrictionEntry.setIntValue(intent.getIntExtra(RESULT_VALUE, 0));
                break;
            case RestrictionEntry.TYPE_STRING:
                restrictionEntry.setSelectedString(intent.getStringExtra(RESULT_VALUE));
                break;
            case RestrictionEntry.TYPE_MULTI_SELECT:
                restrictionEntry.setAllSelectedStrings(intent.getStringArrayExtra(RESULT_VALUE));
                break;
            case RestrictionEntry.TYPE_BUNDLE: {
                Bundle bundle = intent.getBundleExtra(RESULT_VALUE);
                restrictionEntry.setRestrictions(convertBundleToRestrictions(bundle));
                break;
            }
            case RestrictionEntry.TYPE_BUNDLE_ARRAY: {
                Parcelable[] bundleArray = intent.getParcelableArrayExtra(RESULT_VALUE);
                RestrictionEntry[] restrictionEntryArray = new RestrictionEntry[bundleArray.length];
                for (int i = 0; i < bundleArray.length; i++) {
                    restrictionEntryArray[i] =
                            RestrictionEntry.createBundleEntry(
                                    String.valueOf(i), convertBundleToRestrictions((Bundle) bundleArray[i]));
                }
                restrictionEntry.setRestrictions(restrictionEntryArray);
                break;
            }
        }
    }

    private int getRestrictionTypeFromDialogType(int typeIndex) {
        switch (typeIndex) {
            case KeyValuePairDialogFragment.DialogType.BOOL_TYPE:
                return RestrictionEntry.TYPE_BOOLEAN;
            case KeyValuePairDialogFragment.DialogType.INT_TYPE:
                return RestrictionEntry.TYPE_INTEGER;
            case KeyValuePairDialogFragment.DialogType.STRING_TYPE:
                return RestrictionEntry.TYPE_STRING;
            case KeyValuePairDialogFragment.DialogType.STRING_ARRAY_TYPE:
                return RestrictionEntry.TYPE_MULTI_SELECT;
            case KeyValuePairDialogFragment.DialogType.BUNDLE_TYPE:
                return RestrictionEntry.TYPE_BUNDLE;
            case KeyValuePairDialogFragment.DialogType.BUNDLE_ARRAY_TYPE:
                return RestrictionEntry.TYPE_BUNDLE_ARRAY;
            default:
                throw new AssertionError("Unknown type index");
        }
    }

    @Override
    protected void addNewRow() {
        showEditDialog(null);
    }

    @Override
    public View onCreateView(
            LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(layoutInflater, container, savedInstanceState);

        //Not needed anymore since we're performing the action automatically from the Spinner
        View loadDefaultButton = view.findViewById(R.id.load_default_button);
        loadDefaultButton.setVisibility(View.GONE);

        View openBatchFileButton = view.findViewById(R.id.open_batch_file_button);
        openBatchFileButton.setVisibility(View.VISIBLE);

        View saveBatchFileButton = view.findViewById(R.id.save_batch_file_button);
        saveBatchFileButton.setVisibility(View.VISIBLE);

        return view;
    }

    @Override
    protected BaseAdapter createListAdapter() {
        mAppRestrictionsArrayAdapter =
                new RestrictionEntryEditDeleteArrayAdapter(getActivity(), mRestrictionEntries, this, null);
        return mAppRestrictionsArrayAdapter;
    }

    @Override
    protected void onSpinnerItemSelected(ApplicationInfo appInfo) {
        String pkgName = appInfo.packageName;
        if (!TextUtils.isEmpty(pkgName)) {
            Bundle bundle = mDevicePolicyManager.getApplicationRestrictions(mAdminComponent, pkgName);
            loadAppRestrictionsList(convertBundleToRestrictions(bundle));

            //This means that we haven't touched this app, we need to load the default application's restrictions list
            if (mRestrictionEntries.size() == 0) {
                loadManifestAppRestrictions(pkgName);
            } else {
                mLastRestrictionEntries = new ArrayList<>(mRestrictionEntries);
            }
        }
    }

    /**
     * Return the name of the application whose restrictions are currently displayed.
     */
    private String getCurrentAppName() {
        ApplicationInfo applicationInfo = (ApplicationInfo) mManagedAppsSpinner.getSelectedItem();
        return (String) getActivity().getPackageManager().getApplicationLabel(applicationInfo);
    }

    private String getCurrentPackageName() {
        ApplicationInfo applicationInfo = (ApplicationInfo) mManagedAppsSpinner.getSelectedItem();
        return applicationInfo.processName;
    }

    private void loadManifestAppRestrictions(String pkgName) {
        if (!TextUtils.isEmpty(pkgName)) {
            List<RestrictionEntry> manifestRestrictions = null;
            try {
                manifestRestrictions = mRestrictionsManager.getManifestRestrictions(pkgName);
                RestrictionManagerCompat.convertTypeChoiceAndNullToString(manifestRestrictions);
            } catch (NullPointerException e) {
                // This means no default restrictions.
            }
            if (manifestRestrictions != null) {
                loadAppRestrictionsList(manifestRestrictions.toArray(new RestrictionEntry[0]));
            }
        }
    }

    @Override
    protected void saveConfig() {
        String pkgName = ((ApplicationInfo) mManagedAppsSpinner.getSelectedItem()).packageName;
        mDevicePolicyManager.setApplicationRestrictions(
                mAdminComponent,
                pkgName,
                RestrictionManagerCompat.convertRestrictionsToBundle(mRestrictionEntries));
        mLastRestrictionEntries = new ArrayList<>(mRestrictionEntries);
        showToast(getString(R.string.set_app_restrictions_success, pkgName));
    }

    @Override
    protected void resetConfig() {
        mAppRestrictionsArrayAdapter.clear();
        mAppRestrictionsArrayAdapter.addAll(mLastRestrictionEntries);
    }

    @Override
    protected void loadDefault() {
        loadManifestAppRestrictions(
                ((ApplicationInfo) mManagedAppsSpinner.getSelectedItem()).packageName);
    }

    @Override
    protected void importBatchConfigFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");

        startActivityForResult(intent, ACTION_IMPORT_BATCH_FILE);
    }

    @Override
    protected void exportBatchConfigFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, getCurrentPackageName() + "_managed_configs.json");
        startActivityForResult(intent, ACTION_EXPORT_BATCH_FILE);
    }

    @TargetApi(VERSION_CODES.M)
    private RestrictionEntry[] getRestrictionEntries(RestrictionEntry restrictionEntry) {
        return restrictionEntry.getRestrictions();
    }

    public class RestrictionEntryEditDeleteArrayAdapter
            extends EditDeleteArrayAdapter<RestrictionEntry> {

        public RestrictionEntryEditDeleteArrayAdapter(
                Context context,
                List<RestrictionEntry> entries,
                OnEditButtonClickListener<RestrictionEntry> onEditButtonClickListener,
                OnDeleteButtonClickListener<RestrictionEntry> onDeleteButtonClickListener) {
            super(context, entries, onEditButtonClickListener, onDeleteButtonClickListener);
        }

        @Override
        protected String getDisplayName(RestrictionEntry entry) {
            return entry.getKey();
        }
    }
}

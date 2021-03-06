/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.permissioncontroller.permission.ui.auto;

import static com.android.permissioncontroller.Constants.EXTRA_SESSION_ID;
import static com.android.permissioncontroller.Constants.INVALID_SESSION_ID;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;

import com.android.permissioncontroller.R;
import com.android.permissioncontroller.permission.model.legacy.PermissionGroup;
import com.android.permissioncontroller.permission.ui.UnusedAppsFragment;
import com.android.permissioncontroller.permission.ui.model.ManageStandardPermissionsViewModel;
import com.android.permissioncontroller.permission.utils.Utils;

import java.util.List;

/** Shows the standard permissions that can be granted/denied. */
public class AutoManageStandardPermissionsFragment extends AutoManagePermissionsFragment {

    private static final String EXTRA_PREFS_KEY = "extra_prefs_key";
    private static final String AUTO_REVOKE_KEY = "auto_revoke_key";
    private ManageStandardPermissionsViewModel mViewModel;

    /** Returns a new instance of {@link AutoManageStandardPermissionsFragment}. */
    public static AutoManageStandardPermissionsFragment newInstance() {
        return new AutoManageStandardPermissionsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Application application = getActivity().getApplication();
        mViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(application))
                .get(ManageStandardPermissionsViewModel.class);

        mViewModel.getUiDataLiveData().observe(this, permissionGroups -> {
            if (permissionGroups != null) {
                updatePermissionsUi();
            } else {
                getActivity().finish();
            }
        });

        mViewModel.getNumAutoRevoked().observe(this, show -> updatePermissionsUi());
    }

    @Override
    protected int getScreenHeaderRes() {
        return R.string.app_permission_manager;
    }

    @Override
    protected void updatePermissionsUi() {
        updatePermissionsUi(/* addSystemPermissions= */ true);

        // Check if we need an additional permissions preference
        List<PermissionGroup> groups = getPermissions().getGroups();
        int numExtraPermissions = 0;
        for (PermissionGroup group : groups) {
            if (!group.getDeclaringPackage().equals(AutoManagePermissionsFragment.OS_PKG)) {
                numExtraPermissions++;
            }
        }

        Preference additionalPermissionsPreference = getPreferenceScreen().findPreference(
                EXTRA_PREFS_KEY);
        if (numExtraPermissions == 0) {
            if (additionalPermissionsPreference != null) {
                getPreferenceScreen().removePreference(additionalPermissionsPreference);
            }
        } else {
            if (additionalPermissionsPreference == null) {
                additionalPermissionsPreference = new Preference(
                        getPreferenceManager().getContext());
                additionalPermissionsPreference.setKey(EXTRA_PREFS_KEY);
                additionalPermissionsPreference.setIcon(Utils.applyTint(getActivity(),
                        R.drawable.ic_more_items,
                        android.R.attr.colorControlNormal));
                additionalPermissionsPreference.setTitle(R.string.additional_permissions);
                additionalPermissionsPreference.setOnPreferenceClickListener(preference -> {
                    AutoManageCustomPermissionsFragment frag =
                            new AutoManageCustomPermissionsFragment();
                    frag.setTargetFragment(AutoManageStandardPermissionsFragment.this,
                            /* requestCode= */ 0);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(android.R.id.content, frag);
                    ft.addToBackStack(null);
                    ft.commit();
                    return true;
                });

                getPreferenceScreen().addPreference(additionalPermissionsPreference);
            }

            additionalPermissionsPreference.setSummary(getResources().getQuantityString(
                    R.plurals.additional_permissions_more, numExtraPermissions,
                    numExtraPermissions));
        }

        Integer numAutoRevoked = mViewModel.getNumAutoRevoked().getValue();

        Preference autoRevokePreference = getPreferenceScreen().findPreference(AUTO_REVOKE_KEY);
        if (numAutoRevoked != null && numAutoRevoked != 0) {
            if (autoRevokePreference == null) {
                autoRevokePreference = new Preference(getPreferenceManager().getContext());
                autoRevokePreference.setOrder(-1);
                autoRevokePreference.setKey(AUTO_REVOKE_KEY);
                autoRevokePreference.setSingleLineTitle(false);
                autoRevokePreference.setIcon(R.drawable.ic_info_outline);
                autoRevokePreference.setTitle(
                        R.string.auto_revoke_permission_notification_title);
                autoRevokePreference.setSummary(
                        R.string.auto_revoke_setting_subtitle);
                autoRevokePreference.setOnPreferenceClickListener(preference -> {
                    AutoUnusedAppsFragment frag = AutoUnusedAppsFragment.newInstance();
                    frag.setArguments(UnusedAppsFragment.createArgs(
                            getArguments().getLong(EXTRA_SESSION_ID, INVALID_SESSION_ID)));
                    frag.setTargetFragment(AutoManageStandardPermissionsFragment.this,
                            /* requestCode= */ 0);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(android.R.id.content, frag);
                    ft.addToBackStack(null);
                    ft.commit();
                    return true;
                });

                getPreferenceScreen().addPreference(autoRevokePreference);
            }
        } else if (numAutoRevoked != null && autoRevokePreference != null) {
            getPreferenceScreen().removePreference(autoRevokePreference);
        }
    }
}

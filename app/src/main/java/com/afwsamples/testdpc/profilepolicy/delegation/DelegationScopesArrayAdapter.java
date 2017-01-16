package com.afwsamples.testdpc.profilepolicy.delegation;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

import com.afwsamples.testdpc.R;

import java.util.List;

/**
 * ArrayAdapter to assist on rendering the delegation scopes granted to an app.
 */

class DelegationScopesArrayAdapter
        extends ArrayAdapter<DelegationFragment.DelegationScope> {

    public DelegationScopesArrayAdapter(Context context, int res,
                                        List<DelegationFragment.DelegationScope> objects) {
        super(context, res, objects);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CheckBox viewHolder;
        if (convertView == null || !(convertView.getTag() instanceof CheckBox)) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.delegation_scope_row,
                    parent, false);
            viewHolder = (CheckBox) convertView.findViewById(R.id.checkbox_delegation_scope);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (CheckBox) convertView.getTag();
        }

        DelegationFragment.DelegationScope delegationScope = getItem(position);
        viewHolder.setChecked(delegationScope.granted);
        switch (delegationScope.scope) {
            case DelegationFragment.DELEGATION_CERT_INSTALL:
                viewHolder.setText(R.string.delegation_scope_cert_install);
                break;
            case DelegationFragment.DELEGATION_APP_RESTRICTIONS:
                viewHolder.setText(R.string.delegation_scope_app_restrictions);
                break;
            case DelegationFragment.DELEGATION_BLOCK_UNINSTALL:
                viewHolder.setText(R.string.delegation_scope_block_uninstall);
                break;
            case DelegationFragment.DELEGATION_PERMISSION_GRANT:
                viewHolder.setText(R.string.delegation_scope_permission_grant);
                break;
            case DelegationFragment.DELEGATION_PACKAGE_ACCESS:
                viewHolder.setText(R.string.delegation_scope_package_access);
                break;
            case DelegationFragment.DELEGATION_ENABLE_SYSTEM_APP:
                viewHolder.setText(R.string.delegation_scope_enable_system_app);
                break;
            case DelegationFragment.DELEGATION_KEEP_UNINSTALLED_PACKAGES:
                viewHolder.setText(R.string.delegation_scope_keep_uninstalled_packages);
                break;
        }

        return convertView;
    }
}

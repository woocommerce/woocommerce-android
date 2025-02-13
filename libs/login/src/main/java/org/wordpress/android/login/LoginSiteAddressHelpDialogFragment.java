package org.wordpress.android.login;

import static android.content.DialogInterface.BUTTON_NEUTRAL;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class LoginSiteAddressHelpDialogFragment extends DialogFragment {
    public static final String TAG = "login_site_address_help_dialog_fragment_tag";

    private LoginListener mLoginListener;

    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    @Inject LoginAnalyticsListener mAnalyticsListener;

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
        if (context instanceof LoginListener) {
            mLoginListener = (LoginListener) context;
        } else {
            throw new RuntimeException(context + " must implement LoginListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder alert = new MaterialAlertDialogBuilder(getActivity());
        if (mLoginListener.getLoginMode() != LoginMode.WOO_LOGIN_MODE) {
            // Only set the title if not the woo app, since the woo app specifies an override
            // layout that includes the title.
            alert.setTitle(R.string.login_site_address_help_title);
        }

        //noinspection InflateParams
        alert.setView(getActivity().getLayoutInflater().inflate(R.layout.login_alert_site_address_help, null));
        alert.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
            mAnalyticsListener.trackDismissDialog();
            dialog.dismiss();
        });
        alert.setNeutralButton(R.string.login_site_address_more_help,
                (dialog, which) -> mLoginListener.helpFindingSiteAddress(
                        mAccountStore.getAccount().getUserName(),
                        mSiteStore));

        if (savedInstanceState == null) {
            mAnalyticsListener.trackUrlHelpScreenViewed();
        }
        AlertDialog dialog = alert.create();
        dialog.setOnShowListener(shownDialog -> setNeutralButton(dialog.getButton(BUTTON_NEUTRAL)));

        return dialog;
    }

    private void setNeutralButton(@NonNull Button button) {
        Context context = getContext();
        if (context != null) {
            int textColor = context.getColor(R.color.login_dialog_neutral_text_button_color);
            button.setTextColor(textColor);
            button.setAllCaps(false);
        }
    }
}

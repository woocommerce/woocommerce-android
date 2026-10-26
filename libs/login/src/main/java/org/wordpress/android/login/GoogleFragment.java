package org.wordpress.android.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.login.google.GoogleCredentialClient;
import org.wordpress.android.login.google.GoogleCredentialResult;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public abstract class GoogleFragment extends Fragment {
    private static final String STATE_FINISHED = "STATE_FINISHED";
    private static final String STATE_DISPLAY_NAME = "STATE_DISPLAY_NAME";
    private static final String STATE_GOOGLE_EMAIL = "STATE_GOOGLE_EMAIL";
    private static final String STATE_GOOGLE_TOKEN_ID = "STATE_GOOGLE_TOKEN_ID";
    private static final String STATE_GOOGLE_PHOTO_URL = "STATE_GOOGLE_PHOTO_URL";
    /**
     * This flag is used to store the information the finishFlow was called when the fragment wasn't attached to an
     * activity (for example an EventBus event was received during ongoing configuration change).
     */
    private boolean mFinished;
    private boolean mFlowStarted;

    @Nullable private CancellationSignal mCredentialRequestCancellationSignal;
    protected GoogleListener mGoogleListener;
    protected LoginListener mLoginListener;
    protected String mDisplayName;
    protected String mGoogleEmail;
    protected String mIdToken;
    protected String mPhotoUrl;

    protected ProgressDialog mProgressDialog;

    public static final String SERVICE_TYPE_GOOGLE = "google";

    @Inject protected Dispatcher mDispatcher;
    @Inject protected SiteStore mSiteStore;

    @Inject protected LoginAnalyticsListener mAnalyticsListener;

    public interface GoogleListener {
        void onGoogleEmailSelected(String email);
        void onGoogleLoginFinished();
        void onGoogleSignupError(String msg);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDispatcher.register(this);
        if (savedInstanceState != null) {
            mFinished = savedInstanceState.getBoolean(STATE_FINISHED, false);
            mDisplayName = savedInstanceState.getString(STATE_DISPLAY_NAME);
            mGoogleEmail = savedInstanceState.getString(STATE_GOOGLE_EMAIL);
            mIdToken = savedInstanceState.getString(STATE_GOOGLE_TOKEN_ID);
            mPhotoUrl = savedInstanceState.getString(STATE_GOOGLE_PHOTO_URL);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_FINISHED, mFinished);
        outState.putString(STATE_DISPLAY_NAME, mDisplayName);
        outState.putString(STATE_GOOGLE_EMAIL, mGoogleEmail);
        outState.putString(STATE_GOOGLE_TOKEN_ID, mIdToken);
        outState.putString(STATE_GOOGLE_PHOTO_URL, mPhotoUrl);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mProgressDialog = ProgressDialog.show(getActivity(), null, getProgressDialogText(), true, false, null);
        mLoginListener = (LoginListener) context;

        try {
            mGoogleListener = (GoogleListener) context;
        } catch (ClassCastException exception) {
            throw new ClassCastException(context.toString() + " must implement GoogleListener");
        }
        if (mFinished) {
            finishFlow();
        }
    }

    @Override
    public void onDetach() {
        dismissProgressDialog();
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        if (mCredentialRequestCancellationSignal != null) {
            mCredentialRequestCancellationSignal.cancel();
        }
        AppLog.d(T.MAIN, "GOOGLE SIGNUP/LOGIN: destroying fragment");
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        // The fragment is retained across configuration changes, so the flow is started only once per instance.
        if (!mFlowStarted && !mFinished) {
            mFlowStarted = true;
            startFlow();
        }
    }

    protected void requestGoogleCredential() {
        GoogleCredentialClient client = new GoogleCredentialClient(requireContext(),
                getString(R.string.default_web_client_id));
        mCredentialRequestCancellationSignal = new CancellationSignal();
        client.requestIdToken(requireActivity(), mCredentialRequestCancellationSignal, result -> {
            mCredentialRequestCancellationSignal = null;
            // Google's account picker should be shown again on the next attempt.
            client.clearCredentialState();
            if (isAdded()) {
                onGoogleCredentialResult(result);
            } else {
                AppLog.d(T.MAIN, "GOOGLE SIGNUP/LOGIN: credential result ignored, fragment not attached");
            }
        });
    }

    protected void setGoogleAccount(@NonNull GoogleCredentialResult.Success account) {
        mDisplayName = account.getDisplayName();
        mGoogleEmail = account.getEmail();
        mIdToken = account.getIdToken();
        mPhotoUrl = removeScaleFromGooglePhotoUrl(account.getPhotoUrl());
    }

    protected abstract String getProgressDialogText();

    protected abstract void startFlow();

    protected abstract void onGoogleCredentialResult(@NonNull GoogleCredentialResult result);

    protected void finishFlow() {
        /* This flag might get lost when the finishFlow is called after the fragment's
         onSaveInstanceState was called - however it's a very rare case, since the fragment is retained across
         config changes.  */
        mFinished = true;
        if (getActivity() != null) {
            AppLog.d(T.MAIN, "GOOGLE SIGNUP/LOGIN: finishing signup/login");
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        }
    }

    protected void showError(String message) {
        finishFlow();
        mGoogleListener.onGoogleSignupError(message);
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    // Remove scale from photo URL path string. Current URL matches /s96-c, which returns a 96 x 96
    // pixel image. Removing /s96-c from the string returns a 512 x 512 pixel image. Using regular
    // expressions may help if the photo URL scale value in the returned path changes.
    protected String removeScaleFromGooglePhotoUrl(String photoUrl) {
        Pattern pattern = Pattern.compile("(/s[0-9]+-c)");
        Matcher matcher = pattern.matcher(photoUrl);
        return matcher.find() ? photoUrl.replace(matcher.group(1), "") : photoUrl;
    }
}

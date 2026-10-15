package org.wordpress.android.login;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnSocialChanged;
import org.wordpress.android.fluxc.store.AccountStore.PushSocialPayload;
import org.wordpress.android.login.google.GoogleCredentialResult;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import static org.wordpress.android.fluxc.store.AccountStore.AccountSocialErrorType.UNKNOWN_USER;
import static org.wordpress.android.fluxc.store.AccountStore.AccountSocialErrorType.USER_EXISTS;

import dagger.android.support.AndroidSupportInjection;

public class LoginGoogleFragment extends GoogleFragment {
    private static final String ARG_SIGNUP_FROM_LOGIN_ENABLED = "ARG_SIGNUP_FROM_LOGIN_ENABLED";
    private static final String FAILURE_SOURCE_RESULT_CANCELED = "result_canceled";
    private static final String FAILURE_SOURCE_CREDENTIAL_ERROR = "credential_error";
    private boolean mIsSignupFromLoginEnabled;

    public static final String TAG = "login_google_fragment_tag";

    public static LoginGoogleFragment newInstance(boolean isSignupFromLoginEnabled) {
        LoginGoogleFragment fragment = new LoginGoogleFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SIGNUP_FROM_LOGIN_ENABLED, isSignupFromLoginEnabled);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
        Bundle args = getArguments();
        if (args != null) {
            mIsSignupFromLoginEnabled = args.getBoolean(ARG_SIGNUP_FROM_LOGIN_ENABLED, false);
        }
    }

    @Override
    protected String getProgressDialogText() {
        return getString(R.string.logging_in);
    }

    @Override
    protected void startFlow() {
        AppLog.d(T.MAIN, "GOOGLE LOGIN: startFlow");
        mAnalyticsListener.trackSocialButtonStart();
        requestGoogleCredential();
    }

    @Override
    protected void onGoogleCredentialResult(@NonNull GoogleCredentialResult result) {
        if (result instanceof GoogleCredentialResult.Success) {
            AppLog.d(T.MAIN, "GOOGLE LOGIN: Google has returned a sign in result - dispatching SocialLoginAction");
            setGoogleAccount((GoogleCredentialResult.Success) result);
            mGoogleListener.onGoogleEmailSelected(mGoogleEmail);
            PushSocialPayload payload = new PushSocialPayload(mIdToken, SERVICE_TYPE_GOOGLE);
            mDispatcher.dispatch(AccountActionBuilder.newPushSocialLoginAction(payload));
        } else if (result instanceof GoogleCredentialResult.Cancelled) {
            GoogleCredentialResult.Cancelled cancelled = (GoogleCredentialResult.Cancelled) result;
            AppLog.d(T.MAIN, "GOOGLE LOGIN: Google has returned a sign in result - canceled");
            trackSocialButtonFailure(FAILURE_SOURCE_RESULT_CANCELED, cancelled.getErrorType(),
                    cancelled.getErrorMessage());
            finishFlow();
        } else if (result instanceof GoogleCredentialResult.Failure) {
            GoogleCredentialResult.Failure failure = (GoogleCredentialResult.Failure) result;
            AppLog.d(T.MAIN, "GOOGLE LOGIN: Google has returned a sign in result - error");
            trackSocialButtonFailure(FAILURE_SOURCE_CREDENTIAL_ERROR, failure.getErrorType(),
                    failure.getErrorMessage());
            showError(getString(R.string.login_error_generic));
        }
    }

    private void trackSocialButtonFailure(@NonNull String source, @NonNull String errorType, String errorMessage) {
        AppLog.e(T.NUX, "Google Login Failed: " + source + " - " + errorType + " - " + errorMessage);
        mAnalyticsListener.trackSocialButtonFailure(source, null, errorType, errorMessage);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            AppLog.d(T.MAIN, "GOOGLE LOGIN: onAuthenticationChanged - error");
            AppLog.e(T.API, "LoginGoogleFragment.onAuthenticationChanged: " + event.error.type
                            + " - " + event.error.message);
            mAnalyticsListener.trackLoginFailed(event.getClass().getSimpleName(),
                    event.error.type.toString(), event.error.message);

            mAnalyticsListener.trackSocialFailure(event.getClass().getSimpleName(),
                    event.error.type.toString(), event.error.message);

            showError(getString(R.string.login_error_generic));
        } else {
            AppLog.d(T.MAIN, "GOOGLE LOGIN: onAuthenticationChanged - success");
            AppLog.i(T.NUX, "LoginGoogleFragment.onAuthenticationChanged: " + event.toString());
            mGoogleListener.onGoogleLoginFinished();
            finishFlow();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocialChanged(OnSocialChanged event) {
        // Response returns error for non-existing account and existing account not connected.
        if (event.isError()) {
            AppLog.e(T.API, "LoginGoogleFragment.onSocialChanged: " + event.error.type + " - " + event.error.message);

            // We don't want to track these errors: USER_EXISTS and UNKNOWN_USER (if signup from login is enabled)
            if (event.error.type != USER_EXISTS && (!mIsSignupFromLoginEnabled || event.error.type != UNKNOWN_USER)) {
                mAnalyticsListener.trackLoginFailed(event.getClass().getSimpleName(),
                        event.error.type.toString(), event.error.message);

                mAnalyticsListener.trackSocialFailure(event.getClass().getSimpleName(),
                        event.error.type.toString(), event.error.message);
            }

            switch (event.error.type) {
                // WordPress account exists with input email address, but not connected.
                case USER_EXISTS:
                    AppLog.d(T.MAIN, "GOOGLE LOGIN: onSocialChanged - wordpress acount exists but not connected");
                    mAnalyticsListener.trackSocialAccountsNeedConnecting();
                    mLoginListener.loginViaSocialAccount(mGoogleEmail, mIdToken, SERVICE_TYPE_GOOGLE, true);
                    break;
                // WordPress account does not exist with input email address.
                case UNKNOWN_USER:
                    AppLog.d(T.MAIN, "GOOGLE LOGIN: onSocialChanged - wordpress account doesn't exist");
                    if (mIsSignupFromLoginEnabled) {
                        mLoginListener.gotUnregisteredSocialAccount(mGoogleEmail, mDisplayName, mIdToken, mPhotoUrl,
                                SERVICE_TYPE_GOOGLE);
                    } else {
                        mAnalyticsListener.trackSocialErrorUnknownUser();
                        showError(getString(R.string.login_error_email_not_found_v2));
                    }
                    break;
                // Too many attempts on sending SMS verification code. The user has to wait before they try again
                case SMS_CODE_THROTTLED:
                    AppLog.d(T.MAIN, "GOOGLE LOGIN: onSocialChanged - error - sms code throttled");
                    showError(getString(R.string.login_error_sms_throttled));
                    break;
                // Unknown error.
                case GENERIC_ERROR:
                // Do nothing for now (included to show all error types) and just fall through to 'default'
                default:
                    AppLog.d(T.MAIN, "GOOGLE LOGIN: onSocialChanged - unknown error");
                    showError(getString(R.string.login_error_generic));
                    break;
            }
        // Response does not return error when two-factor authentication is required.
        } else if (event.requiresTwoStepAuth || Login2FaFragment.TWO_FACTOR_TYPE_SMS.equals(event.notificationSent)) {
            AppLog.d(T.MAIN, "GOOGLE LOGIN: onSocialChanged - needs 2fa");
            mLoginListener.needs2faSocial(mGoogleEmail, event.userId, event.nonceAuthenticator, event.nonceBackup,
                    event.nonceSms, event.nonceWebauthn, event.twoStepTypes);
        } else {
            AppLog.d(T.MAIN, "GOOGLE LOGIN: onSocialChanged - success");
            mGoogleListener.onGoogleLoginFinished();
        }
        finishFlow();
    }
}

package org.wordpress.android.fluxc.model;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.RawConstraints;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId;
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Table
@RawConstraints({"UNIQUE (SITE_ID, URL)"})
public class SiteModel extends Payload<BaseNetworkError> implements Identifiable, Serializable {
    private static final long serialVersionUID = -7641813766771796252L;

    @Retention(SOURCE)
    @IntDef({ORIGIN_UNKNOWN, ORIGIN_WPCOM_REST, ORIGIN_XMLRPC, ORIGIN_WPAPI})
    public @interface SiteOrigin {
    }

    public static final int ORIGIN_UNKNOWN = 0;
    public static final int ORIGIN_WPCOM_REST = 1;
    public static final int ORIGIN_XMLRPC = 2;
    public static final int ORIGIN_WPAPI = 3;

    @Retention(SOURCE)
    @IntDef({HTTPS_CONFIGURATION_UNKNOWN, HTTPS_CONFIGURATION_SECURE,
            HTTPS_CONFIGURATION_REQUIRES_HTTPS})
    public @interface HttpsConfigurationState {
    }

    public static final int HTTPS_CONFIGURATION_UNKNOWN = 0;
    public static final int HTTPS_CONFIGURATION_SECURE = 1;
    public static final int HTTPS_CONFIGURATION_REQUIRES_HTTPS = 2;

    public static final long VIP_PLAN_ID = 31337;

    @PrimaryKey
    @Column
    private int mId;
    // Only given a value for wpcom and Jetpack sites - self-hosted sites use mSelfHostedSiteId
    @Column
    private long mSiteId;
    @Column
    private String mUrl;
    @Column
    private String mAdminUrl;
    @Column
    private String mLoginUrl;
    @Column
    private String mName;
    @Column
    private boolean mIsWPCom;
    @Column
    private boolean mIsWPComAtomic;
    @Column
    private int mPublishedStatus = -1;
    @Column
    private String mTimezone; // Expressed as an offset relative to GMT (e.g. '-8')
    @Column
    private int mOrigin = ORIGIN_UNKNOWN; // Does this site come from a WPCOM REST or XMLRPC fetch_sites call?

    // Self hosted specifics
    // The siteId for self hosted sites. Jetpack sites will also have a mSiteId, which is their id on wpcom
    @Column
    private long mSelfHostedSiteId;
    @Column
    private String mUsername;
    @Column
    private String mPassword;
    @Column
    private String mWpApiRestUrl;

    // Self hosted user's profile data
    @Column
    private String mEmail;
    @Column
    private String mDisplayName;

    // mIsJetpackInstalled is true if Jetpack is installed and activated on the self hosted site, but Jetpack can
    // be disconnected.
    @Column
    private boolean mIsJetpackInstalled;
    // mIsJetpackConnected is true if Jetpack is installed, activated and connected to a WordPress.com account.
    @Column
    private boolean mIsJetpackConnected;
    // mIsJetpackCPConnected is true for self hosted sites that use Jetpack Connection Package,
    // but don't have full jetpack plugin
    @Column(name = "IS_JETPACK_CP_CONNECTED")
    private boolean mIsJetpackCPConnected;
    @Column
    private String mJetpackVersion;
    @Column
    private String mJetpackUserEmail;
    @Column
    private boolean mIsWpComStore;
    @Column
    private boolean mHasWooCommerce;

    // WPCom specifics
    @Column
    private boolean mIsPrivate;
    @Column
    private long mPlanId;
    @Column
    private String mPlanShortName;
    @Column
    private String mPlanProductSlug;

    // WPCom capabilities
    @Column
    private boolean mHasCapabilityManageOptions;
    @Column
    private String mActiveJetpackConnectionPlugins;
    @Column
    private String mJetpackModules;

    @Column
    private String mApplicationPasswordsAuthorizeUrl;
    @Column
    private boolean mCanBlaze;
    // Comma-separated list of active features in the site's plan
    @Column
    private String mPlanActiveFeatures;
    @Column
    private int mHttpsConfigurationState = HTTPS_CONFIGURATION_UNKNOWN;

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    public LocalId localId() {
        return new LocalOrRemoteId.LocalId(mId);
    }

    public RemoteId remoteId() {
        if (mSiteId != 0L) {
            return new RemoteId(mSiteId);
        } else {
            return new RemoteId(mSelfHostedSiteId);
        }
    }

    public SiteModel() {
    }

    public long getSiteId() {
        return mSiteId;
    }

    public void setSiteId(long siteId) {
        mSiteId = siteId;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(@NonNull String url) {
        try {
            // Normalize the URL, because it can be used as an identifier.
            mUrl = (new URI(url)).normalize().toString();
        } catch (URISyntaxException e) {
            // Don't set the URL
            AppLog.e(T.API, "Trying to set an invalid url: " + url);
        }
    }

    public String getLoginUrl() {
        return mLoginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        mLoginUrl = loginUrl;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public boolean isWPCom() {
        return mIsWPCom;
    }

    public void setIsWPCom(boolean wpCom) {
        mIsWPCom = wpCom;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        mUsername = username;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public String getWpApiRestUrl() {
        return mWpApiRestUrl;
    }

    public void setWpApiRestUrl(String wpApiRestEndpoint) {
        mWpApiRestUrl = wpApiRestEndpoint;
    }

    public long getSelfHostedSiteId() {
        return mSelfHostedSiteId;
    }

    public void setSelfHostedSiteId(long selfHostedSiteId) {
        mSelfHostedSiteId = selfHostedSiteId;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public boolean isPrivate() {
        return mIsPrivate;
    }

    public void setIsPrivate(boolean isPrivate) {
        mIsPrivate = isPrivate;
    }

    public String getAdminUrl() {
        return mAdminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        mAdminUrl = adminUrl;
    }

    public boolean getHasCapabilityManageOptions() {
        return mHasCapabilityManageOptions;
    }

    public void setHasCapabilityManageOptions(boolean capabilityManageOptions) {
        mHasCapabilityManageOptions = capabilityManageOptions;
    }

    public String getTimezone() {
        return mTimezone;
    }

    public void setTimezone(String timezone) {
        mTimezone = timezone;
    }

    public String getPlanShortName() {
        return mPlanShortName;
    }

    public void setPlanShortName(String planShortName) {
        mPlanShortName = planShortName;
    }

    public String getPlanProductSlug() {
        return mPlanProductSlug;
    }

    public void setPlanProductSlug(String planProductSlug) {
        mPlanProductSlug = planProductSlug;
    }

    public long getPlanId() {
        return mPlanId;
    }

    public void setPlanId(long planId) {
        mPlanId = planId;
    }

    public boolean isJetpackInstalled() {
        return mIsJetpackInstalled;
    }

    public void setIsJetpackInstalled(boolean jetpackInstalled) {
        mIsJetpackInstalled = jetpackInstalled;
    }

    public boolean isJetpackConnected() {
        return mIsJetpackConnected;
    }

    public void setIsJetpackConnected(boolean jetpackConnected) {
        mIsJetpackConnected = jetpackConnected;
    }

    public boolean isJetpackCPConnected() {
        return mIsJetpackCPConnected;
    }

    public void setIsJetpackCPConnected(boolean isJetpackCPConnected) {
        this.mIsJetpackCPConnected = isJetpackCPConnected;
    }

    public String getJetpackVersion() {
        return mJetpackVersion;
    }

    public void setJetpackVersion(String jetpackVersion) {
        mJetpackVersion = jetpackVersion;
    }

    public String getJetpackUserEmail() {
        return mJetpackUserEmail;
    }

    public void setJetpackUserEmail(String jetpackUserEmail) {
        mJetpackUserEmail = jetpackUserEmail;
    }

    public boolean isWpComStore() {
        return mIsWpComStore;
    }

    public void setIsWpComStore(boolean isWpComStore) {
        mIsWpComStore = isWpComStore;
    }

    public boolean getHasWooCommerce() {
        return mHasWooCommerce;
    }

    public void setHasWooCommerce(boolean hasWooCommerce) {
        mHasWooCommerce = hasWooCommerce;
    }

    @SiteOrigin
    public int getOrigin() {
        return mOrigin;
    }

    public void setOrigin(@SiteOrigin int origin) {
        mOrigin = origin;
    }

    public boolean isUsingWpComRestApi() {
        return isWPCom() || getOrigin() == ORIGIN_WPCOM_REST;
    }

    public boolean isWPComAtomic() {
        return mIsWPComAtomic;
    }

    public void setIsWPComAtomic(boolean isWPComAtomic) {
        mIsWPComAtomic = isWPComAtomic;
    }

    public String getActiveJetpackConnectionPlugins() {
        return mActiveJetpackConnectionPlugins;
    }

    public void setActiveJetpackConnectionPlugins(String activeJetpackConnectionPlugins) {
        mActiveJetpackConnectionPlugins = activeJetpackConnectionPlugins;
    }

    @Nullable
    public String getJetpackModules() {
        return mJetpackModules;
    }

    public void setJetpackModules(@Nullable String jetpackModules) {
        mJetpackModules = jetpackModules;
    }

    public boolean isAdmin() {
        return mHasCapabilityManageOptions;
    }

    public String getApplicationPasswordsAuthorizeUrl() {
        return mApplicationPasswordsAuthorizeUrl;
    }

    public void setApplicationPasswordsAuthorizeUrl(String applicationPasswordsAuthorizeUrl) {
        mApplicationPasswordsAuthorizeUrl = applicationPasswordsAuthorizeUrl;
    }

    public boolean isApplicationPasswordsSupported() {
        return mApplicationPasswordsAuthorizeUrl != null &&
                !mApplicationPasswordsAuthorizeUrl.isEmpty();
    }

    public int getPublishedStatus() {
        return mPublishedStatus;
    }

    public void setPublishedStatus(int publishedStatus) {
        this.mPublishedStatus = publishedStatus;
    }

    public boolean getCanBlaze() {
        return mCanBlaze;
    }

    public void setCanBlaze(boolean canBlaze) {
        this.mCanBlaze = canBlaze;
    }

    public String getPlanActiveFeatures() {
        return mPlanActiveFeatures;
    }

    public void setPlanActiveFeatures(final String planActiveFeatures) {
        this.mPlanActiveFeatures = planActiveFeatures;
    }

    @HttpsConfigurationState
    public int getHttpsConfigurationState() {
        return mHttpsConfigurationState;
    }

    public void setHttpsConfigurationState(@HttpsConfigurationState int httpsConfigurationState) {
        mHttpsConfigurationState = httpsConfigurationState;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SiteModel)) return false;
        SiteModel siteModel = (SiteModel) o;
        return mId == siteModel.mId &&
                mSiteId == siteModel.mSiteId &&
                mIsWPCom == siteModel.mIsWPCom &&
                mIsWPComAtomic == siteModel.mIsWPComAtomic &&
                mPublishedStatus == siteModel.mPublishedStatus &&
                mOrigin == siteModel.mOrigin &&
                mSelfHostedSiteId == siteModel.mSelfHostedSiteId &&
                mIsJetpackInstalled == siteModel.mIsJetpackInstalled &&
                mIsJetpackConnected == siteModel.mIsJetpackConnected &&
                mIsJetpackCPConnected == siteModel.mIsJetpackCPConnected &&
                mIsWpComStore == siteModel.mIsWpComStore &&
                mHasWooCommerce == siteModel.mHasWooCommerce &&
                mIsPrivate == siteModel.mIsPrivate &&
                mPlanId == siteModel.mPlanId &&
                mHasCapabilityManageOptions == siteModel.mHasCapabilityManageOptions &&
                mHttpsConfigurationState == siteModel.mHttpsConfigurationState &&
                Objects.equals(mUrl, siteModel.mUrl) &&
                Objects.equals(mAdminUrl, siteModel.mAdminUrl) &&
                Objects.equals(mLoginUrl, siteModel.mLoginUrl) &&
                Objects.equals(mName, siteModel.mName) &&
                Objects.equals(mTimezone, siteModel.mTimezone) &&
                Objects.equals(mUsername, siteModel.mUsername) &&
                Objects.equals(mPassword, siteModel.mPassword) &&
                Objects.equals(mWpApiRestUrl, siteModel.mWpApiRestUrl) &&
                Objects.equals(mEmail, siteModel.mEmail) &&
                Objects.equals(mDisplayName, siteModel.mDisplayName) &&
                Objects.equals(mJetpackVersion, siteModel.mJetpackVersion) &&
                Objects.equals(mJetpackUserEmail, siteModel.mJetpackUserEmail) &&
                Objects.equals(mPlanShortName, siteModel.mPlanShortName) &&
                Objects.equals(mPlanProductSlug, siteModel.mPlanProductSlug) &&
                Objects.equals(mActiveJetpackConnectionPlugins, siteModel.mActiveJetpackConnectionPlugins) &&
                Objects.equals(mJetpackModules, siteModel.mJetpackModules) &&
                Objects.equals(mApplicationPasswordsAuthorizeUrl, siteModel.mApplicationPasswordsAuthorizeUrl) &&
                Objects.equals(mCanBlaze, siteModel.mCanBlaze) &&
                Objects.equals(mPlanActiveFeatures, siteModel.mPlanActiveFeatures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId,
                mSiteId,
                mUrl,
                mAdminUrl,
                mLoginUrl,
                mName,
                mIsWPCom,
                mIsWPComAtomic,
                mPublishedStatus,
                mTimezone,
                mOrigin,
                mSelfHostedSiteId,
                mUsername,
                mPassword,
                mWpApiRestUrl,
                mEmail,
                mDisplayName,
                mIsJetpackInstalled,
                mIsJetpackConnected,
                mIsJetpackCPConnected,
                mJetpackVersion,
                mJetpackUserEmail,
                mIsWpComStore,
                mHasWooCommerce,
                mIsPrivate,
                mPlanId,
                mPlanShortName,
                mPlanProductSlug,
                mHasCapabilityManageOptions,
                mActiveJetpackConnectionPlugins,
                mJetpackModules,
                mApplicationPasswordsAuthorizeUrl,
                mCanBlaze,
                mPlanActiveFeatures,
                mHttpsConfigurationState);
    }
}

package org.wordpress.android.fluxc.model;

import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.util.StringUtils;

import java.util.Objects;

public class AccountModel extends Payload<BaseNetworkError> {
    private int mId;

    // Account attributes
    private String mUserName;
    private long mUserId;
    private String mDisplayName;
    private String mProfileUrl; // profile_URL
    private String mAvatarUrl; // avatar_URL
    private long mPrimarySiteId;
    private boolean mEmailVerified;
    private int mSiteCount;
    private int mVisibleSiteCount;
    private String mEmail;
    private boolean mHasUnseenNotes;

    // Account Settings attributes
    private String mFirstName;
    private String mLastName;
    private String mAboutMe;
    private String mDate;
    private String mNewEmail;
    private boolean mPendingEmailChange;
    private boolean mTwoStepEnabled;
    private String mWebAddress; // WPCom rest API: user_URL
    private boolean mTracksOptOut;
    // WPCom rest API: woomobile_crash_reporting_opt_out. Null means the user never made a persisted choice.
    private Boolean mCrashReportingOptOut;
    private boolean mUsernameCanBeChanged;

    public AccountModel() {
        init();
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof AccountModel)) return false;

        AccountModel otherAccount = (AccountModel) other;

        return getId() == otherAccount.getId()
               && StringUtils.equals(getUserName(), otherAccount.getUserName())
               && getUserId() == otherAccount.getUserId()
               && StringUtils.equals(getDisplayName(), otherAccount.getDisplayName())
               && StringUtils.equals(getProfileUrl(), otherAccount.getProfileUrl())
               && StringUtils.equals(getAvatarUrl(), otherAccount.getAvatarUrl())
               && getPrimarySiteId() == otherAccount.getPrimarySiteId()
               && getSiteCount() == otherAccount.getSiteCount()
               && getEmailVerified() == otherAccount.getEmailVerified()
               && getVisibleSiteCount() == otherAccount.getVisibleSiteCount()
               && StringUtils.equals(getFirstName(), otherAccount.getFirstName())
               && StringUtils.equals(getLastName(), otherAccount.getLastName())
               && StringUtils.equals(getAboutMe(), otherAccount.getAboutMe())
               && StringUtils.equals(getDate(), otherAccount.getDate())
               && StringUtils.equals(getNewEmail(), otherAccount.getNewEmail())
               && getPendingEmailChange() == otherAccount.getPendingEmailChange()
               && getTwoStepEnabled() == otherAccount.getTwoStepEnabled()
               && StringUtils.equals(getWebAddress(), otherAccount.getWebAddress())
               && getHasUnseenNotes() == otherAccount.getHasUnseenNotes()
               && getTracksOptOut() == otherAccount.getTracksOptOut()
               && Objects.equals(getCrashReportingOptOut(), otherAccount.getCrashReportingOptOut())
               && getUsernameCanBeChanged() == otherAccount.getUsernameCanBeChanged();
    }

    public void init() {
        mUserName = "";
        mUserId = 0;
        mDisplayName = "";
        mProfileUrl = "";
        mAvatarUrl = "";
        mPrimarySiteId = 0;
        mSiteCount = 0;
        mEmailVerified = true;
        mVisibleSiteCount = 0;
        mEmail = "";
        mFirstName = "";
        mLastName = "";
        mAboutMe = "";
        mDate = "";
        mNewEmail = "";
        mPendingEmailChange = false;
        mTwoStepEnabled = false;
        mWebAddress = "";
        mTracksOptOut = false;
        mCrashReportingOptOut = null;
        mUsernameCanBeChanged = false;
    }

    /**
     * Copies Account attributes from another {@link AccountModel} to this instance.
     */
    public void copyAccountAttributes(AccountModel other) {
        if (other == null) return;
        setUserName(other.getUserName());
        setUserId(other.getUserId());
        setDisplayName(other.getDisplayName());
        setProfileUrl(other.getProfileUrl());
        setAvatarUrl(other.getAvatarUrl());
        setPrimarySiteId(other.getPrimarySiteId());
        setSiteCount(other.getSiteCount());
        setVisibleSiteCount(other.getVisibleSiteCount());
        setEmail(other.getEmail());
        setHasUnseenNotes(other.getHasUnseenNotes());
        setEmailVerified(other.getEmailVerified());
    }

    /**
     * Copies Account Settings attributes from another {@link AccountModel} to this instance.
     */
    public void copyAccountSettingsAttributes(AccountModel other) {
        if (other == null) return;
        setUserName(other.getUserName());
        setPrimarySiteId(other.getPrimarySiteId());
        setFirstName(other.getFirstName());
        setLastName(other.getLastName());
        setAboutMe(other.getAboutMe());
        setDate(other.getDate());
        setNewEmail(other.getNewEmail());
        setPendingEmailChange(other.getPendingEmailChange());
        setTwoStepEnabled(other.getTwoStepEnabled());
        setTracksOptOut(other.getTracksOptOut());
        setCrashReportingOptOut(other.getCrashReportingOptOut());
        setWebAddress(other.getWebAddress());
        setDisplayName(other.getDisplayName());
        setUsernameCanBeChanged(other.getUsernameCanBeChanged());
    }

    public long getUserId() {
        return mUserId;
    }

    public void setUserId(long userId) {
        mUserId = userId;
    }

    public void setPrimarySiteId(long primarySiteId) {
        mPrimarySiteId = primarySiteId;
    }

    public long getPrimarySiteId() {
        return mPrimarySiteId;
    }

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) {
        mUserName = userName;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public String getProfileUrl() {
        return mProfileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        mProfileUrl = profileUrl;
    }

    public String getAvatarUrl() {
        return mAvatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        mAvatarUrl = avatarUrl;
    }

    public boolean getEmailVerified() {
        return mEmailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        mEmailVerified = emailVerified;
    }

    public int getSiteCount() {
        return mSiteCount;
    }

    public void setSiteCount(int siteCount) {
        mSiteCount = siteCount;
    }

    public int getVisibleSiteCount() {
        return mVisibleSiteCount;
    }

    public void setVisibleSiteCount(int visibleSiteCount) {
        mVisibleSiteCount = visibleSiteCount;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setFirstName(String firstName) {
        mFirstName = firstName;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public void setLastName(String lastName) {
        mLastName = lastName;
    }

    public String getLastName() {
        return mLastName;
    }

    public void setAboutMe(String aboutMe) {
        mAboutMe = aboutMe;
    }

    public String getAboutMe() {
        return mAboutMe;
    }

    public void setDate(String date) {
        mDate = date;
    }

    public String getDate() {
        return mDate;
    }

    public void setNewEmail(String newEmail) {
        mNewEmail = newEmail;
    }

    public String getNewEmail() {
        return mNewEmail;
    }

    public void setPendingEmailChange(boolean pendingEmailChange) {
        mPendingEmailChange = pendingEmailChange;
    }

    public boolean getPendingEmailChange() {
        return mPendingEmailChange;
    }

    public void setTwoStepEnabled(boolean twoStepEnabled) {
        mTwoStepEnabled = twoStepEnabled;
    }

    public boolean getTwoStepEnabled() {
        return mTwoStepEnabled;
    }

    public void setWebAddress(String webAddress) {
        mWebAddress = webAddress;
    }

    public String getWebAddress() {
        return mWebAddress;
    }

    public boolean getHasUnseenNotes() {
        return mHasUnseenNotes;
    }

    public void setHasUnseenNotes(boolean hasUnseenNotes) {
        mHasUnseenNotes = hasUnseenNotes;
    }

    public boolean getTracksOptOut() {
        return mTracksOptOut;
    }

    public void setTracksOptOut(boolean tracksOptOut) {
        mTracksOptOut = tracksOptOut;
    }

    @Nullable
    public Boolean getCrashReportingOptOut() {
        return mCrashReportingOptOut;
    }

    public void setCrashReportingOptOut(@Nullable Boolean crashReportingOptOut) {
        mCrashReportingOptOut = crashReportingOptOut;
    }

    public boolean getUsernameCanBeChanged() {
        return mUsernameCanBeChanged;
    }

    public void setUsernameCanBeChanged(boolean usernameCanBeChanged) {
        mUsernameCanBeChanged = usernameCanBeChanged;
    }
}

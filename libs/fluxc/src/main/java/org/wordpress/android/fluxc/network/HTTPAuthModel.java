package org.wordpress.android.fluxc.network;

public class HTTPAuthModel {
    private String mRootUrl;
    private String mRealm;
    private String mUsername;
    private String mPassword;

    public HTTPAuthModel() {
    }

    public String getRealm() {
        return mRealm;
    }

    public void setRealm(String realm) {
        mRealm = realm;
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

    public String getRootUrl() {
        return mRootUrl;
    }

    public void setRootUrl(String rootUrl) {
        mRootUrl = rootUrl;
    }
}

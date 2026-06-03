package org.wordpress.android.login;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload;
import org.wordpress.android.fluxc.store.SiteStore.SiteError;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;

class LoginSiteAddressErrorMapper {
    @StringRes
    int getSiteInfoErrorResId(SiteError error, boolean networkAvailable) {
        if (error.type == SiteErrorType.TLS_CERTIFICATE_VALIDITY_ERROR) {
            return R.string.error_site_url_certificate_validity;
        } else if (error.type == SiteErrorType.REMOTE_SITE_CERTIFICATE_ERROR) {
            return R.string.error_site_url_remote_certificate;
        } else if (error.type == SiteErrorType.WORDPRESS_COM_CONNECTIVITY_ERROR) {
            return R.string.error_wordpress_com_connectivity;
        } else if (networkAvailable) {
            return R.string.invalid_site_url_message;
        } else {
            return R.string.error_generic_network;
        }
    }

    @Nullable
    @StringRes
    Integer getWooSiteInfoInlineErrorResId(ConnectSiteInfoPayload siteInfo) {
        if (!siteInfo.exists) {
            return R.string.invalid_site_url_message;
        }
        return null;
    }
}

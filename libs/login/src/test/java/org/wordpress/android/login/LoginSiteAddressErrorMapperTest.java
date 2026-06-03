package org.wordpress.android.login;

import org.junit.Test;
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload;
import org.wordpress.android.fluxc.store.SiteStore.SiteError;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginSiteAddressErrorMapperTest {
    private final LoginSiteAddressErrorMapper mMapper = new LoginSiteAddressErrorMapper();

    @Test
    public void mapsInvalidSiteToInvalidUrlWhenNetworkIsAvailable() {
        SiteError error = new SiteError(SiteErrorType.INVALID_SITE);

        Integer result = mMapper.getSiteInfoErrorResId(error, true);

        assertThat(result).isEqualTo(R.string.invalid_site_url_message);
    }

    @Test
    public void mapsWordPressComConnectivityErrorToConnectivityMessage() {
        SiteError error = new SiteError(SiteErrorType.WORDPRESS_COM_CONNECTIVITY_ERROR);

        Integer result = mMapper.getSiteInfoErrorResId(error, true);

        assertThat(result).isEqualTo(R.string.error_wordpress_com_connectivity);
    }

    @Test
    public void mapsTlsCertificateValidityErrorToCertificateValidityMessage() {
        SiteError error = new SiteError(SiteErrorType.TLS_CERTIFICATE_VALIDITY_ERROR);

        Integer result = mMapper.getSiteInfoErrorResId(error, true);

        assertThat(result).isEqualTo(R.string.error_site_url_certificate_validity);
    }

    @Test
    public void mapsAnyErrorToGenericNetworkWhenNetworkIsUnavailable() {
        SiteError error = new SiteError(SiteErrorType.INVALID_SITE);

        Integer result = mMapper.getSiteInfoErrorResId(error, false);

        assertThat(result).isEqualTo(R.string.error_generic_network);
    }

    @Test
    public void mapsNonExistentWooSiteInfoToInvalidUrl() {
        ConnectSiteInfoPayload siteInfo = new ConnectSiteInfoPayload("test.com");

        Integer result = mMapper.getWooSiteInfoInlineErrorResId(siteInfo);

        assertThat(result).isEqualTo(R.string.invalid_site_url_message);
    }

    @Test
    public void mapsExistingWooSiteInfoToNoInlineError() {
        ConnectSiteInfoPayload siteInfo = new ConnectSiteInfoPayload(
                "test.com",
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                null);

        Integer result = mMapper.getWooSiteInfoInlineErrorResId(siteInfo);

        assertThat(result).isNull();
    }
}

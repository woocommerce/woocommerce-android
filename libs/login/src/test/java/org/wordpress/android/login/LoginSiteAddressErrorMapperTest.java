package org.wordpress.android.login;

import org.junit.Test;
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
    public void mapsTlsCertificateValidityErrorToSecureConnectionDateTimeMessage() {
        SiteError error = new SiteError(SiteErrorType.TLS_CERTIFICATE_VALIDITY_ERROR);

        Integer result = mMapper.getSiteInfoErrorResId(error, true);

        assertThat(result).isEqualTo(R.string.error_site_url_certificate_validity);
    }

    @Test
    public void mapsRemoteSiteCertificateErrorToRemoteCertificateMessage() {
        SiteError error = new SiteError(SiteErrorType.REMOTE_SITE_CERTIFICATE_ERROR);

        Integer result = mMapper.getSiteInfoErrorResId(error, true);

        assertThat(result).isEqualTo(R.string.error_site_url_remote_certificate);
    }

    @Test
    public void mapsAnyErrorToGenericNetworkWhenNetworkIsUnavailable() {
        SiteError error = new SiteError(SiteErrorType.INVALID_SITE);

        Integer result = mMapper.getSiteInfoErrorResId(error, false);

        assertThat(result).isEqualTo(R.string.error_generic_network);
    }
}

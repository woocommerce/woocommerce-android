package org.wordpress.android.fluxc.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URI;

import javax.inject.Inject;

public class HTTPAuthManager {
    @Inject public HTTPAuthManager() {}

    /**
     * Get an HTTPAuthModel containing username and password for the url parameter
     *
     * @param url to test
     * @return null if url is not matching any known HTTP auth credentials
     */
    // TODO: HTTP Basic Auth persistence was removed (AINFRA-540). This always returns null.
    @Nullable
    public HTTPAuthModel getHTTPAuthModel(String url) {
        return null;
    }

    // TODO: HTTP Basic Auth persistence was removed (AINFRA-540). This is a no-op.
    public void addHTTPAuthCredentials(@NonNull String username, @NonNull String password,
                                       @NonNull String url, @Nullable String realm) {
    }

    // TODO: Previously used by addHTTPAuthCredentials() to normalize URLs before persisting
    //  to WellSQL. Kept for potential future use.
    @SuppressWarnings("unused")
    private String normalizeURL(String url) {
        try {
            URI uri = URI.create(url);
            return uri.normalize().toString();
        } catch (IllegalArgumentException e) {
            return url;
        }
    }
}

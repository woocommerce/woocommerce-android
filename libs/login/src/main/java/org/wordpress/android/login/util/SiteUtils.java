package org.wordpress.android.login.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload;
import org.wordpress.android.fluxc.store.SiteStore.SiteFilter;
import org.wordpress.android.util.UrlUtils;

import java.util.ArrayList;
import java.util.List;

public class SiteUtils {
    @NonNull
    public static ArrayList<Integer> getCurrentSiteIds(@NonNull SiteStore siteStore) {
        ArrayList<Integer> siteIDs = new ArrayList<>();
        for (SiteModel site : siteStore.getSites()) {
            siteIDs.add(site.getId());
        }

        return siteIDs;
    }

    @Nullable
    public static SiteModel getSiteByMatchingUrl(SiteStore siteStore, String url) {
        return getSiteByMatchingUrl(siteStore.getSites(), url);
    }

    @Nullable
    @VisibleForTesting
    public static SiteModel getSiteByMatchingUrl(List<SiteModel> siteModelList, String url) {
        if (siteModelList == null || siteModelList.isEmpty()) {
            return null;
        }
        String incomingHost = toHost(url);
        for (SiteModel siteModel : siteModelList) {
            if (toHost(siteModel.getUrl()).equalsIgnoreCase(incomingHost)) {
                return siteModel;
            }
        }
        String incomingHostWithoutWww = stripWww(incomingHost);
        for (SiteModel siteModel : siteModelList) {
            if (stripWww(toHost(siteModel.getUrl())).equalsIgnoreCase(incomingHostWithoutWww)) {
                return siteModel;
            }
        }
        return null;
    }

    private static String toHost(String url) {
        return UrlUtils.removeScheme(url).replace("/", "");
    }

    private static String stripWww(String host) {
        return host.regionMatches(true, 0, "www.", 0, 4) ? host.substring(4) : host;
    }

    @NonNull
    public static FetchSitesPayload getFetchSitesPayload(boolean isJetpackAppLogin,
                                                         boolean isWooAppLogin) {
        ArrayList<SiteFilter> siteFilters = new ArrayList<>();
        return new FetchSitesPayload(
                siteFilters, !isJetpackAppLogin && !isWooAppLogin
        );
    }
}

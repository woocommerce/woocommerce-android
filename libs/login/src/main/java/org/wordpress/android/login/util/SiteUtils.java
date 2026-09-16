package org.wordpress.android.login.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    public static SiteModel getSiteByMatchingUrl(List<SiteModel> siteModelList, String url) {
        if (siteModelList != null && !siteModelList.isEmpty()) {
            String incomingSiteUrl = toComparableHost(url);
            for (SiteModel siteModel : siteModelList) {
                if (toComparableHost(siteModel.getUrl()).equalsIgnoreCase(incomingSiteUrl)) {
                    return siteModel;
                }
            }
        }
        return null;
    }

    private static String toComparableHost(String url) {
        String host = UrlUtils.removeScheme(url).replace("/", "");
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

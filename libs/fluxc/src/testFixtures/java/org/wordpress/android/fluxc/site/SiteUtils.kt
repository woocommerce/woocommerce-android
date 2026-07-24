package org.wordpress.android.fluxc.site;

import org.wordpress.android.fluxc.model.SiteModel;

public class SiteUtils {
    public static SiteModel generateWPComSite() {
        return generateTestSite(556, "", true);
    }

    public static SiteModel generateTestSite(long remoteId, String url, boolean isWPCom) {
        SiteModel example = new SiteModel();
        example.setUrl(url);
        example.setSiteId(remoteId);
        example.setIsWPCom(isWPCom);
        if (isWPCom) {
            example.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        } else {
            example.setOrigin(SiteModel.ORIGIN_XMLRPC);
        }
        return example;
    }

    public static SiteModel generateSelfHostedNonJPSite() {
        SiteModel example = new SiteModel();
        example.setSelfHostedSiteId(6);
        example.setIsWPCom(false);
        example.setIsJetpackInstalled(false);
        example.setIsJetpackConnected(false);
        example.setUrl("http://some.url");
        example.setOrigin(SiteModel.ORIGIN_XMLRPC);
        return example;
    }

    public static SiteModel generateJetpackSiteOverXMLRPC() {
        SiteModel example = new SiteModel();
        example.setSiteId(982);
        example.setSelfHostedSiteId(8);
        example.setIsWPCom(false);
        example.setIsJetpackInstalled(true);
        example.setIsJetpackConnected(true);
        example.setUsername("ponyuser");
        example.setPassword("ponypass");
        example.setUrl("http://jetpack.url");
        example.setOrigin(SiteModel.ORIGIN_XMLRPC);
        return example;
    }

    public static SiteModel generateJetpackSiteOverRestOnly() {
        SiteModel example = new SiteModel();
        example.setSiteId(5623);
        example.setIsWPCom(false);
        example.setIsJetpackInstalled(true);
        example.setIsJetpackConnected(true);
        example.setUrl("http://jetpack2.url");
        example.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        return example;
    }

    public static SiteModel generateSelfHostedSiteFutureJetpack() {
        SiteModel example = new SiteModel();
        example.setSelfHostedSiteId(8);
        example.setIsWPCom(false);
        example.setIsJetpackInstalled(false);
        example.setIsJetpackConnected(false);
        example.setUrl("http://jetpack2.url");
        example.setOrigin(SiteModel.ORIGIN_XMLRPC);
        return example;
    }
}

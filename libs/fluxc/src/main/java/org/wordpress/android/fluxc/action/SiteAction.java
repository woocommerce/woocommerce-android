package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload;
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainPayload;
import org.wordpress.android.fluxc.store.SiteStore.DesignatedPrimaryDomainPayload;
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchConnectSiteInfoPayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsResponsePayload;

@ActionEnum
public enum SiteAction implements IAction {
    // Remote actions
    @Action(payloadType = SiteModel.class)
    FETCH_PROFILE_XML_RPC,
    @Action(payloadType = SiteModel.class)
    FETCH_SITE,
    @Action(payloadType = FetchSitesPayload.class)
    FETCH_SITES,
    @Action(payloadType = RefreshSitesXMLRPCPayload.class)
    FETCH_SITES_XML_RPC,
    @Action(payloadType = SuggestDomainsPayload.class)
    SUGGEST_DOMAINS,
    @Action(payloadType = FetchConnectSiteInfoPayload.class)
    FETCH_CONNECT_SITE_INFO,
    @Action(payloadType = String.class)
    FETCH_DOMAIN_SUPPORTED_STATES,
    @Action(payloadType = DesignatePrimaryDomainPayload.class)
    DESIGNATE_PRIMARY_DOMAIN,

    // Remote responses
    @Action(payloadType = SiteModel.class)
    FETCHED_PROFILE_XML_RPC,
    @Action(payloadType = ConnectSiteInfoPayload.class)
    FETCHED_CONNECT_SITE_INFO,
    @Action(payloadType = DomainSupportedStatesResponsePayload.class)
    FETCHED_DOMAIN_SUPPORTED_STATES,
    @Action(payloadType = DesignatedPrimaryDomainPayload.class)
    DESIGNATED_PRIMARY_DOMAIN,

    // Local actions
    @Action(payloadType = SiteModel.class)
    UPDATE_SITE,
    @Action(payloadType = SiteModel.class)
    REMOVE_SITE,
    @Action
    REMOVE_ALL_SITES,
    @Action(payloadType = SuggestDomainsResponsePayload.class)
    SUGGESTED_DOMAINS,
}

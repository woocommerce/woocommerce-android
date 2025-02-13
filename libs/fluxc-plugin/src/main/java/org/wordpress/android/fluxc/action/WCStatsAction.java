package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.WCStatsStore.FetchNewVisitorStatsPayload;
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsAvailabilityPayload;
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsAvailabilityResponsePayload;

@ActionEnum
public enum WCStatsAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchRevenueStatsAvailabilityPayload.class)
    FETCH_REVENUE_STATS_AVAILABILITY,

    @Action(payloadType = FetchNewVisitorStatsPayload.class)
    FETCH_NEW_VISITOR_STATS,

    // Remote responses
    @Action(payloadType = FetchRevenueStatsAvailabilityResponsePayload.class)
    FETCHED_REVENUE_STATS_AVAILABILITY
}

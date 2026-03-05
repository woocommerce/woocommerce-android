package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersByIdsPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersByIdsResponsePayload;

@ActionEnum
public enum WCOrderAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchOrderListPayload.class)
    FETCH_ORDER_LIST,
    @Action(payloadType = FetchOrdersByIdsPayload.class)
    FETCH_ORDERS_BY_IDS,
    @Action(payloadType = FetchOrderStatusOptionsPayload.class)
    FETCH_ORDER_STATUS_OPTIONS,

    // Remote responses
    @Action(payloadType = FetchOrderListResponsePayload.class)
    FETCHED_ORDER_LIST,
    @Action(payloadType = FetchOrdersByIdsResponsePayload.class)
    FETCHED_ORDERS_BY_IDS,
    @Action(payloadType = FetchOrderStatusOptionsResponsePayload.class)
    FETCHED_ORDER_STATUS_OPTIONS,

    // Deprecated actions
    @Deprecated // Only used as causeOfChange value. The action is never dispatched.
    UPDATE_ORDER_STATUS,
}

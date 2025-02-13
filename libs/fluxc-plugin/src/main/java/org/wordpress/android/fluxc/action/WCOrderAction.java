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
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload;

@ActionEnum
public enum WCOrderAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchOrdersPayload.class)
    FETCH_ORDERS,
    @Action(payloadType = FetchOrderListPayload.class)
    FETCH_ORDER_LIST,
    @Action(payloadType = FetchOrdersByIdsPayload.class)
    FETCH_ORDERS_BY_IDS,
    @Action(payloadType = FetchOrdersCountPayload.class)
    FETCH_ORDERS_COUNT,
    @Action(payloadType = UpdateOrderStatusPayload.class)
    @Deprecated // Use suspendable WCOrderStore.updateOrderStatus(..) directly.
    UPDATE_ORDER_STATUS,
    @Action(payloadType = SearchOrdersPayload.class)
    SEARCH_ORDERS,
    @Action(payloadType = FetchOrderStatusOptionsPayload.class)
    FETCH_ORDER_STATUS_OPTIONS,

    // Remote responses
    @Action(payloadType = FetchOrdersResponsePayload.class)
    FETCHED_ORDERS,
    @Action(payloadType = FetchOrderListResponsePayload.class)
    FETCHED_ORDER_LIST,
    @Action(payloadType = FetchOrdersByIdsResponsePayload.class)
    FETCHED_ORDERS_BY_IDS,
    @Action(payloadType = FetchOrdersCountResponsePayload.class)
    FETCHED_ORDERS_COUNT,
    @Action(payloadType = SearchOrdersResponsePayload.class)
    SEARCHED_ORDERS,
    @Action(payloadType = FetchOrderStatusOptionsResponsePayload.class)
    FETCHED_ORDER_STATUS_OPTIONS,
}

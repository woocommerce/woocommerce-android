package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsPayload
import org.wordpress.android.fluxc.store.ListStore.MarkListsNeedRefreshPayload
import org.wordpress.android.fluxc.store.ListStore.OnListDataFailure

@ActionEnum
enum class ListAction : IAction {
    @Action(payloadType = FetchedListItemsPayload::class)
    FETCHED_LIST_ITEMS,
    @Action(payloadType = ListDescriptorTypeIdentifier::class)
    LIST_REQUIRES_REFRESH,
    @Action(payloadType = ListDescriptorTypeIdentifier::class)
    LIST_DATA_INVALIDATED,
    @Action(payloadType = MarkListsNeedRefreshPayload::class)
    MARK_LISTS_OF_TYPE_NEED_REFRESH,
    @Action(payloadType = OnListDataFailure::class)
    LIST_DATA_FAILURE,
}

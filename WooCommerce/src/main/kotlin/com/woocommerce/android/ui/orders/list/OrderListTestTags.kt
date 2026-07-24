package com.woocommerce.android.ui.orders.list

internal object OrderListTestTags {
    const val LIST = "order_list"
    const val INITIAL_LOADING = "order_list_initial_loading"
    const val EMPTY = "order_list_empty"
    const val EMPTY_ACTION = "order_list_empty_action"
    const val APPEND_PROGRESS = "order_list_append_progress"
    const val DATE_SECTION = "order_list_date_section"
    const val NULL_PLACEHOLDER = "order_list_null_placeholder"
    const val SKELETON_DATE = "order_list_skeleton_date"
    const val SKELETON_TITLE = "order_list_skeleton_title"
    const val SKELETON_TOTAL = "order_list_skeleton_total"
    const val SKELETON_BADGE = "order_list_skeleton_badge"
    const val SKELETON_DIVIDER = "order_list_skeleton_divider"

    fun orderRow(orderId: Long) = "order_list_order_$orderId"
    fun orderDivider(orderId: Long) = "order_list_divider_$orderId"
    fun selectionSlot(orderId: Long) = "order_list_selection_slot_$orderId"
    fun selectionIndicator(orderId: Long) = "order_list_selection_indicator_$orderId"
    fun loadingItem(orderId: Long) = "order_list_loading_$orderId"
}

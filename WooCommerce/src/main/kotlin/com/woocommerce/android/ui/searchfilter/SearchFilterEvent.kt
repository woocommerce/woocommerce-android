package com.woocommerce.android.ui.searchfilter

sealed class SearchFilterEvent {
    data class ItemSelected(val selectedItemValue: String, val requestKey: String) : SearchFilterEvent()
}

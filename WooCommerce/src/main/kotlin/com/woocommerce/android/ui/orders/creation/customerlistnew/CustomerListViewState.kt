package com.woocommerce.android.ui.orders.creation.customerlistnew

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

data class CustomerListViewState(
    val searchQuery: String = "",
    val searchModes: List<SearchMode>,
    val customers: CustomerList = CustomerList.Loading,
) {
    sealed class CustomerList {
        object Loading : CustomerList()
        object Empty : CustomerList()
        object Error : CustomerList()
        data class Loaded(val customers: List<Item>) : CustomerList()

        data class Item(
            val remoteId: Long,
            val firstName: String,
            val lastName: String,
            val email: String,
        )
    }
}

@Parcelize
data class SearchMode(
    @StringRes val labelResId: Int,
    val searchParam: String,
    val isSelected: Boolean,
): Parcelable

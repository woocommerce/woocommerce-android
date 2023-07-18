package com.woocommerce.android.ui.orders.creation.customerlistnew

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.model.Address
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.customer.WCCustomerModel

data class CustomerListViewState(
    val searchQuery: String = "",
    val searchModes: List<SearchMode>,
    val partialLoading: Boolean = false,
    val body: CustomerList = CustomerList.Loading,
) {
    sealed class CustomerList {
        object Loading : CustomerList()
        object Empty : CustomerList()
        object Error : CustomerList()
        data class Loaded(
            val customers: List<Item>,
            val firstPageLoaded: Boolean,
        ) : CustomerList()

        sealed class Item {
            data class Customer(
                val remoteId: Long,
                val firstName: String,
                val lastName: String,
                val email: String,

                val payload: WCCustomerModel,
            ) : Item()

            object Loading : Item()
        }
    }
}

@Parcelize
data class SearchMode(
    @StringRes val labelResId: Int,
    val searchParam: String,
    val isSelected: Boolean,
) : Parcelable

data class PaginationState(
    val currentPage: Int,
    val hasNextPage: Boolean,
)

data class CustomerSelected(
    val customerId: Long,
    val billingAddress: Address,
    val shippingAddress: Address
) : MultiLiveEvent.Event()

object AddCustomer: MultiLiveEvent.Event()

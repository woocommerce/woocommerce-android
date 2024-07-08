package com.woocommerce.android.ui.orders.creation.customerlist

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.customer.WCCustomerModel

data class CustomerListViewState(
    @StringRes val searchHint: Int,
    val searchQuery: String = "",
    val searchModes: List<SearchMode>,
    val showFab: Boolean,
    val searchFocused: Boolean,
    val partialLoading: Boolean = false,
    val body: CustomerList = CustomerList.Loading
) {
    sealed class CustomerList {
        object Loading : CustomerList()
        data class Empty(
            @StringRes val message: Int,
            @DrawableRes val image: Int,
            val button: Button?,
        ) : CustomerList()
        data class Error(@StringRes val message: Int) : CustomerList()
        data class Loaded(
            val customers: List<Item>,
            val shouldResetScrollPosition: Boolean
        ) : CustomerList()

        sealed class Item {
            data class Customer(
                val remoteId: Long,
                val name: Text,
                val email: Text,
                val username: Text,
                val payload: WCCustomerModel,
            ) : Item() {

                val isGuest
                    get() = remoteId == 0L

                sealed class Text {
                    data class Highlighted(val text: String, val start: Int, val end: Int) : Text()
                    data class Placeholder(val text: String) : Text()
                }
            }

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

data class Button(
    @StringRes val text: Int,
    val onClick: () -> Unit,
)

data class CustomerSelected(val customer: Order.Customer) : MultiLiveEvent.Event()

data class AddCustomer(val email: String?) : MultiLiveEvent.Event()

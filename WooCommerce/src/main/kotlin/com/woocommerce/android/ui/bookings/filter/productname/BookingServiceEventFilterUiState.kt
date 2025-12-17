package com.woocommerce.android.ui.bookings.filter.productname

import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.bookings.filter.BookingFilterListItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

data class BookingServiceEventFilterUiState(
    val availableProducts: List<BookableProduct> = emptyList(),
    val selectedProducts: BookingsFilterOption.ServiceEvents = BookingsFilterOption.ServiceEvents.DEFAULT,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val onProductSelected: (BookableProduct) -> Unit = {},
    val onSearchQueryChanged: (String) -> Unit = {},
) {
    val items: List<BookingFilterListItem> = filteredProducts().map { product ->
        BookingFilterListItem(
            title = product.name,
            selected = isSelected(product),
            onClick = { onProductSelected(product) }
        )
    }

    private fun filteredProducts(): List<BookableProduct> {
        return listOf(BookableProduct.Any) +
            if (searchQuery.isBlank()) {
                availableProducts
            } else {
                availableProducts.filter { product ->
                    product.name is UiStringText &&
                        product.name.text.contains(searchQuery.trim(), ignoreCase = true)
                }
            }
    }

    private fun isSelected(product: BookableProduct): Boolean = if (product == BookableProduct.Any) {
        selectedProducts.values.isEmpty()
    } else {
        selectedProducts.values.any { it.productId == product.id }
    }
}

data class BookableProduct(
    val id: Long,
    val name: UiString
) {
    companion object {
        val Any = BookableProduct(id = -1, name = UiStringRes(R.string.bookings_filter_default))
    }
}

package com.woocommerce.android.ui.bookings.filter.productname

import com.woocommerce.android.R
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.bookings.filter.BookingFilterListItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

data class BookingServiceEventFilterUiState(
    val availableProducts: List<BookableProduct> = emptyList(),
    val selectedProducts: BookingsFilterOption.ServiceEvents = BookingsFilterOption.ServiceEvents.DEFAULT,
    val searchQuery: String = "",
    val onProductSelected: (BookableProduct?) -> Unit = {},
    val onSearchQueryChanged: (String) -> Unit = {},
) {
    val items: List<BookingFilterListItem> = filteredProducts().map { product ->
        BookingFilterListItem(
            title = product.name?.let { UiStringText(it) }
                ?: UiStringRes(R.string.bookings_filter_default),
            selected = isSelected(product),
            onClick = { onProductSelected(product) }
        )
    }

    private fun filteredProducts(): List<BookableProduct> {
        val selectedProductsList = selectedProducts.values.map {
            BookableProduct(id = it.productId, name = it.productName)
        }

        val productsToFilter = (
            listOf(BookableProduct.Any) +
                selectedProductsList.filter { it !in availableProducts } +
                availableProducts
            ).distinctBy { it.id }

        return if (searchQuery.isBlank()) {
            productsToFilter
        } else {
            productsToFilter.filter { product ->
                product.name?.contains(searchQuery, ignoreCase = true) ?: false
            }
        }
    }

    private fun isSelected(product: BookableProduct): Boolean = if (product == BookableProduct.Any) {
        selectedProducts.values.isEmpty()
    } else {
        selectedProducts.values.any { it.productId == product.id }
    }
}

data class BookableProductItem(
    val product: BookableProduct,
    val selected: Boolean = false,
    val onClick: () -> Unit = {}
)

data class BookableProduct(
    val id: Long?,
    val name: String?
) {
    companion object {
        val Any = BookableProduct(id = null, name = null)
    }
}

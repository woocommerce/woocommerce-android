package com.woocommerce.android.ui.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CouponListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val couponRepository: CouponRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState) {
    private val currencyCode by lazy {
        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    }

    val couponsState = couponRepository.couponsFlow
        .map { coupons ->
            CouponListState(
                coupons = coupons.map { it.toUiModel() }
            )
        }
        .asLiveData()

    init {
        viewModelScope.launch {
            couponRepository.loadCoupons()
        }
    }

    private fun formatDiscount(amount: BigDecimal?, couponType: Coupon.Type?): String {
        return when (couponType) {
            Coupon.Type.Percent -> "$amount%"
            else -> {
                if (amount != null) {
                    currencyCode?.let { currencyFormatter.formatCurrency(amount, it) }
                        ?: amount.toString()
                } else {
                    ""
                }
            }
        }
    }

    /*
    - When only specific products or categories are defined: Display "x products" or "x categories"
    - When specific products/categories and exceptions are defined: Display "x products excl. y categories" etc.
    - When both specific products and categories are defined: Display "x products and y categories"
    - When only exceptions are defined: Display "everything excl. x products" or "everything excl. y categories"
     */
    private fun formatAffectedArticles(
        includedProducts: Int,
        excludedProducts: Int,
        includedCategories: Int,
        excludedCategories: Int
    ): String {
        val included = when {
            includedProducts != 0 && includedCategories != 0 -> {
                resourceProvider.getString(
                    R.string.coupon_list_item_label_products_and_categories,
                    formatProducts(includedProducts),
                    formatCategories(includedCategories)
                )
            }
            includedProducts != 0 -> formatProducts(includedProducts)
            includedCategories != 0 -> formatCategories(includedCategories)
            else -> resourceProvider.getString(R.string.coupon_list_item_label_everything)
        }

        val excluded = when {
            excludedProducts != 0 && excludedCategories != 0 -> {
                resourceProvider.getString(
                    R.string.coupon_list_item_label_products_and_categories,
                    formatProducts(excludedProducts),
                    formatCategories(excludedCategories)
                )
            }
            excludedProducts != 0 -> formatProducts(excludedProducts)
            excludedCategories != 0 -> formatCategories(excludedCategories)
            else -> ""
        }

        return if (excluded.isNotEmpty()) {
            resourceProvider.getString(
                R.string.coupon_list_item_label_included_and_excluded,
                included,
                excluded
            )
        } else {
            included
        }
    }

    private fun formatProducts(products: Int): String {
        return if (products > 0) {
            StringUtils.getQuantityString(
                resourceProvider,
                products,
                default = R.string.product_count_many,
                one = R.string.product_count_one
            )
        } else ""
    }

    private fun formatCategories(categories: Int): String {
        return if (categories > 0) {
            StringUtils.getQuantityString(
                resourceProvider,
                categories,
                default = R.string.category_count_many,
                one = R.string.category_count_one
            )
        } else ""
    }

    private fun Coupon.toUiModel(): CouponListItem {
        return CouponListItem(
            id = id,
            code = code,
            formattedDiscount = formatDiscount(amount, type),
            affectedArticles = formatAffectedArticles(
                products.size,
                excludedProducts.size,
                categories.size,
                excludedCategories.size
            ),
            isActive = dateExpiresGmt?.after(Date()) ?: true
        )
    }

    data class CouponListState(
        val isLoading: Boolean = false,
        val coupons: List<CouponListItem> = emptyList()
    )

    data class CouponListItem(
        val id: Long,
        val code: String? = null,
        val formattedDiscount: String,
        val affectedArticles: String,
        val isActive: Boolean
    )
}

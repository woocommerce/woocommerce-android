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
                isLoading = false,
                coupons = coupons.map {
                    it.toUiModel(currencyFormatter, currencyCode)
                }
            )
        }
        .asLiveData()

    init {
        viewModelScope.launch {
            couponRepository.loadCoupons()
        }
    }

    data class CouponListState(
        val currencyCode: String? = null,
        val isLoading: Boolean = false,
        val coupons: List<CouponItem> = emptyList()
    )

    data class CouponItem(
        val id: Long,
        val code: String? = null,
        val formattedDiscount: String,
        val affectedArticles: String,
        val isActive: Boolean
    )

    private fun Coupon.toUiModel(currencyFormatter: CurrencyFormatter, currencyCode: String?): CouponItem {
        fun formatDiscount(amount: BigDecimal?, couponType: Coupon.Type?): String {
            return when (couponType) {
                Coupon.Type.Percent -> "$amount%"
                else -> {
                    if (amount != null) {
                        currencyCode?.let {
                            currencyFormatter.formatCurrency(amount, currencyCode)
                        } ?: amount.toString()
                    } else {
                        ""
                    }
                }
            }
        }

        fun formatAffectedArticles(includedProductsCount: Int, includedCategoriesCount: Int): String {
            return if (includedProductsCount == 0 && includedCategoriesCount == 0) {
                resourceProvider.getString(R.string.coupon_list_item_label_all_products)
            } else {
                val products = StringUtils.getQuantityString(
                    resourceProvider,
                    includedProductsCount,
                    default = R.string.product_count_many,
                    one = R.string.product_count_one
                )

                val categories = StringUtils.getQuantityString(
                    resourceProvider,
                    includedCategoriesCount,
                    default = R.string.category_count_many,
                    one = R.string.category_count_one
                )
                "$products, $categories"
            }
        }

        return CouponItem(
            id = id,
            code = code,
            formattedDiscount = formatDiscount(amount, type),
            affectedArticles = formatAffectedArticles(products.size, categories.size),
            isActive = dateExpiresGmt?.after(Date()) ?: true
        )
    }
}

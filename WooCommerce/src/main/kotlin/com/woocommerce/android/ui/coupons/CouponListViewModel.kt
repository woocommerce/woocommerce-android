package com.woocommerce.android.ui.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.persistence.entity.CouponDataModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.DateTimeUtils
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
    val couponsState = couponRepository.couponsFlow
        .map { coupons ->
            CouponListState(
                isLoading = false,
                coupons = coupons.map {
                    it.toAppModel(
                        currencyFormatter,
                        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
                    )
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
        val discount: String,
        val affectedArticles: String,
        val isActive: Boolean
    )

    fun CouponDataModel.toAppModel(currencyFormatter: CurrencyFormatter, currencyCode: String?): CouponItem {
        fun formatDiscount(amount: String?, discountType: String?): String {
            return when (discountType) {
                "percent" -> "$amount%"
                else -> {
                    if (amount != null) {
                        currencyCode?.let { currencyFormatter.formatCurrency(amount, currencyCode) } ?: amount
                    } else {
                        ""
                    }
                }
            }
        }

        fun formatAffectedArticles(
            includedProductsCount: Int,
            includedCategoriesCount: Int
        ): String {
            return if (includedProductsCount == 0 && includedCategoriesCount == 0) {
                resourceProvider.getString(R.string.coupon_list_item_label_all_products)
            }
            else {
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

        fun isCouponActive(expiryDate: String?): Boolean {
            return if (expiryDate != null) {
                val date = if (expiryDate.endsWith("Z")) expiryDate else "${expiryDate}Z"
                DateTimeUtils.dateUTCFromIso8601(date).after(Date())
            } else {
                true
            }
        }

        return CouponItem(
            id = couponEntity.id,
            code = couponEntity.code,
            discount = formatDiscount(couponEntity.amount, couponEntity.discountType),
            affectedArticles = formatAffectedArticles(products.size, categories.size),
            isActive = isCouponActive(couponEntity.dateExpiresGmt)
        )
    }
}

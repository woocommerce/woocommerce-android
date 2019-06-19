package com.woocommerce.android.ui.products

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel

interface ProductDetailContract {
    interface Presenter : BasePresenter<View> {
        fun getSiteSettings(): WCSettingsModel?
        fun getProductSiteSettings(): WCProductSettingsModel?
        fun getProduct(remoteProductId: Long): WCProductModel?
        fun fetchProduct(remoteProductId: Long, forced: Boolean)
        fun loadProductDetail(remoteProductId: Long)
        fun formatCurrency(rawValue: String): String
        fun getWeightUnit(): String
        fun getDimensionUnit(): String
    }

    interface View : BaseView<Presenter> {
        fun showProduct(product: WCProductModel)
        fun showFetchProductError()
        fun showSkeleton(show: Boolean)
    }
}

package com.woocommerce.android.ui.products

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCProductModel

interface ProductDetailContract {
    interface Presenter : BasePresenter<View> {
        fun getProduct(remoteProductId: Long): WCProductModel?
        fun fetchProduct(remoteProductId: Long)
        fun formatCurrency(rawValue: String): String
        fun getWeightUnit(): String
        fun getDimensionUnit(): String
        fun getTitle(): String
    }

    interface View : BaseView<Presenter> {
        fun showProduct(product: WCProductModel)
        fun showFetchProductError()
        fun showSkeleton(show: Boolean)
    }
}

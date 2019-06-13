package com.woocommerce.android.ui.products

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_LOADED
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.CurrencyFormatter
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@OpenClassOnDebug
class ProductDetailPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wooCommerceStore: WooCommerceStore,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val uiMessageResolver: UIMessageResolver,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter
) : ProductDetailContract.Presenter {
    private var remoteProductId = 0L
    private var currencyCode: String? = null
    private var weightUnit: String? = null
    private var dimensionUnit: String? = null

    private var view: ProductDetailContract.View? = null

    override fun takeView(view: ProductDetailContract.View) {
        this.view = view
        dispatcher.register(this)
        ConnectionChangeReceiver.getEventBus().register(this)
    }

    override fun dropView() {
        view = null
        dispatcher.unregister(this)
        ConnectionChangeReceiver.getEventBus().unregister(this)
    }

    override fun loadProductDetail(remoteProductId: Long) {
        getSiteSettings()?.let { currencyCode = it.currencyCode }
        getProductSiteSettings()?.let { settings ->
            weightUnit = settings.weightUnit
            dimensionUnit = settings.dimensionUnit
        }
        val shouldFetch = remoteProductId != this.remoteProductId
        this.remoteProductId = remoteProductId
        getProduct(remoteProductId)?.let { product ->
            view?.showProduct(product)
            if (shouldFetch) {
                fetchProduct(remoteProductId, false)
            }
        } ?: fetchProduct(remoteProductId, true)
    }

    override fun getSiteSettings(): WCSettingsModel? =
            wooCommerceStore.getSiteSettings(selectedSite.get())

    override fun getProductSiteSettings(): WCProductSettingsModel? =
            wooCommerceStore.getProductSettings(selectedSite.get())

    override fun getProduct(remoteProductId: Long): WCProductModel? =
            productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)

    override fun fetchProduct(remoteProductId: Long, forced: Boolean) {
        if (networkStatus.isConnected()) {
            this.remoteProductId = remoteProductId
            if (forced) {
                view?.showSkeleton(true)
            }
            val payload = WCProductStore.FetchSingleProductPayload(selectedSite.get(), remoteProductId)
            dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductAction(payload))
        } else {
            view?.showSkeleton(false)
            uiMessageResolver.showOfflineSnack()
        }
    }

    override fun formatCurrency(rawValue: String): String {
        if (rawValue.isEmpty()) return rawValue

        return currencyCode?.let {
            currencyFormatter.formatCurrency(rawValue, it)
        } ?: rawValue
    }

    override fun getWeightUnit() = weightUnit ?: ""
    override fun getDimensionUnit() = dimensionUnit ?: ""

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_SINGLE_PRODUCT) {
            if (event.isError) {
                view?.showFetchProductError()
            } else {
                AnalyticsTracker.track(PRODUCT_DETAIL_LOADED)
                getProduct(remoteProductId)?.let {
                    view?.showProduct(it)
                } ?: view?.showFetchProductError()
            }
            view?.showSkeleton(false)
        }
    }
}

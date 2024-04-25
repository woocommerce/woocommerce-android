package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.R
import com.woocommerce.android.di.SiteComponentEntryPoint
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.toDataModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.util.WooLog
import dagger.hilt.EntryPoints
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.withIndex
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.HasOrdersResult
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import javax.inject.Inject

@ActivityRetainedScoped
class DashboardRepository @Inject constructor(
    selectedSite: SelectedSite,
    private val dashboardDataStore: DashboardDataStore,
    orderStore: WCOrderStore,
    private val isBlazeEnabled: IsBlazeEnabled,
    productListRepository: ProductListRepository
) {
    private val siteCoroutineScope = EntryPoints.get(
        selectedSite.siteComponent!!,
        SiteComponentEntryPoint::class.java
    ).siteCoroutineScope()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val statsWidgetsAvailability = selectedSite.observe()
        .filterNotNull()
        .flatMapLatest { orderStore.observeOrderCountForSite(it) }
        .map { count -> count != 0 }
        .distinctUntilChanged()
        .transform { hasOrders ->
            if (!hasOrders) {
                val fetchResult = orderStore.fetchHasOrders(selectedSite.get(), null).let {
                    when (it) {
                        is HasOrdersResult.Success -> it.hasOrders
                        // Default to true if we can't determine if there are orders
                        is HasOrdersResult.Failure -> true
                    }
                }
                emit(fetchResult)
            } else {
                emit(true)
            }
        }
        .stateIn(siteCoroutineScope, SharingStarted.Lazily, true)

    private val blazeAvailability = selectedSite.observe()
        .filterNotNull()
        .map { isBlazeEnabled() }
        .combine(
            productListRepository
                .observeProductsCount(
                    filterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
                    excludeSampleProducts = true
                )
                .withIndex()
                .map { (index, productsCount) ->
                    if (productsCount == 0L && index == 0) {
                        productListRepository.fetchProductList()
                        false
                    } else {
                        productsCount > 0
                    }
                }
                .distinctUntilChanged()
        ) { isBlazeEnabled, hasPublishedProducts ->
            isBlazeEnabled && hasPublishedProducts
        }
        .stateIn(siteCoroutineScope, SharingStarted.Lazily, false)

    val widgets = combine(
        dashboardDataStore.dashboard.map { it.widgetsList },
        statsWidgetsAvailability,
        blazeAvailability
    ) { widgets, statsWidgetsAvailability, blazeAvailability ->
        widgets.toDomainModel(statsWidgetsAvailability, blazeAvailability)
    }

    suspend fun updateWidgets(widgets: List<DashboardWidget>) = runCatching {
        dashboardDataStore.updateDashboard(
            DashboardDataModel.newBuilder()
                .addAllWidgets(widgets.map { it.toDataModel() })
                .build()
        )
    }.onFailure { throwable ->
        WooLog.e(WooLog.T.DASHBOARD, "Failed to update dashboard data", throwable)
    }

    suspend fun updateWidgetVisibility(type: DashboardWidget.Type, isVisible: Boolean) {
        val dataStoreWidgets = widgets.first()
            .toMutableList()
            .apply {
                val index = indexOfFirst { it.type == type }
                if (index != -1) {
                    set(index, get(index).copy(isVisible = isVisible))
                }
            }
        updateWidgets(dataStoreWidgets)
    }

    private fun List<DashboardWidgetDataModel>.toDomainModel(
        statsWidgetsAvailability: Boolean,
        blazeAvailability: Boolean
    ): List<DashboardWidget> {
        return map { widget ->
            val type = DashboardWidget.Type.valueOf(widget.type)
            DashboardWidget(
                type = type,
                isVisible = widget.isAdded,
                status = when (type) {
                    DashboardWidget.Type.STATS,
                    DashboardWidget.Type.POPULAR_PRODUCTS -> {
                        if (statsWidgetsAvailability) {
                            DashboardWidget.Status.Available
                        } else {
                            DashboardWidget.Status.Unavailable(
                                badgeText = R.string.my_store_widget_unavailable
                            )
                        }
                    }

                    DashboardWidget.Type.BLAZE -> {
                        if (blazeAvailability) DashboardWidget.Status.Available else DashboardWidget.Status.Hidden
                    }

                    else -> DashboardWidget.Status.Available
                }
            )
        }.sortedBy { it.isAvailable }
    }
}

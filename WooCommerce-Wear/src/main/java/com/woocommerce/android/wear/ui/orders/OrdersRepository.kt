package com.woocommerce.android.wear.ui.orders

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.wearable.DataMap
import com.google.gson.Gson
import com.woocommerce.android.wear.datastore.DataStoreQualifier
import com.woocommerce.android.wear.datastore.DataStoreType
import com.woocommerce.android.wear.extensions.getSiteId
import com.woocommerce.android.wear.extensions.toOrderEntity
import com.woocommerce.android.wear.extensions.toWearOrder
import com.woocommerce.android.wear.model.toAppModel
import com.woocommerce.android.wear.ui.login.LoginRepository
import com.woocommerce.commons.DataParameters.ORDERS_JSON
import com.woocommerce.commons.DataParameters.ORDER_ID
import com.woocommerce.commons.DataParameters.ORDER_PRODUCTS_JSON
import com.woocommerce.commons.WearOrder
import com.woocommerce.commons.WearOrderedProduct
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCWearableStore
import javax.inject.Inject

class OrdersRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.ORDERS) private val ordersDataStore: DataStore<Preferences>,
    private val loginRepository: LoginRepository,
    private val wearableStore: WCWearableStore,
    private val orderStore: WCOrderStore,
    private val refundStore: WCRefundStore
) {
    private val gson by lazy { Gson() }

    suspend fun fetchOrders(
        selectedSite: SiteModel
    ) = wearableStore.fetchOrders(
        site = selectedSite,
        shouldStoreData = true
    )

    suspend fun fetchOrderRefunds(
        selectedSite: SiteModel,
        orderId: Long
    ) = refundStore.fetchAllRefunds(selectedSite, orderId)
        .takeUnless { it.isError }
        ?.model
        ?.map { it.toAppModel() }
        ?: emptyList()

    suspend fun getStoredOrders(
        selectedSite: SiteModel
    ) = orderStore.getOrdersForSite(selectedSite)

    suspend fun getOrderFromId(
        selectedSite: SiteModel,
        orderId: Long
    ) = orderStore.getOrderByIdAndSite(
        site = selectedSite,
        orderId = orderId
    )

    fun getOrderRefunds(
        selectedSite: SiteModel,
        orderId: Long
    ) = refundStore.getAllRefunds(selectedSite, orderId)
        .map { it.toAppModel() }

    fun observeOrdersDataChanges(
        selectedSite: SiteModel
    ) = orderStore.observeOrdersForSite(selectedSite)
        .distinctUntilChanged()
        .map { orders ->
            orders.sortedByDescending { it.dateCreated }
                .map { it.toWearOrder() }
        }

    suspend fun receiveOrdersDataFromPhone(data: DataMap) {
        val ordersJson = data.getString(ORDERS_JSON.value, "")
        val receivedOrders = gson.fromJson(ordersJson, Array<WearOrder>::class.java).toList()
        wearableStore.insertOrders(
            orders = receivedOrders.map { it.toOrderEntity() }
        )
    }

    fun observeOrderProductsDataChanges(
        orderId: Long,
        siteId: Long
    ) = ordersDataStore.data
        .mapNotNull { it[stringPreferencesKey(generateProductsKey(orderId, siteId))] }
        .map { gson.fromJson(it, Array<WearOrderedProduct>::class.java).toList() }

    suspend fun receiveOrderProductsDataFromPhone(data: DataMap) {
        val orderId = data.getLong(ORDER_ID.value, 0)
        val productsJson = data.getString(ORDER_PRODUCTS_JSON.value, "")
        val siteId = data.getSiteId(loginRepository.selectedSite)

        ordersDataStore.edit { prefs ->
            val key = generateProductsKey(orderId, siteId)
            prefs[stringPreferencesKey(key)] = productsJson
        }
    }

    private fun generateProductsKey(
        orderId: Long,
        siteId: Long
    ) = "${ORDERS_KEY_PREFIX}:$siteId:$orderId"

    companion object {
        private const val ORDERS_KEY_PREFIX = "store-orders"
    }
}

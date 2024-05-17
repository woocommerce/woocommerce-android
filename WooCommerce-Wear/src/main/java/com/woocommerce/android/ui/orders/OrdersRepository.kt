package com.woocommerce.android.ui.orders

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.wearable.DataMap
import com.google.gson.Gson
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.extensions.getSiteId
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.commons.wear.DataParameters.ORDERS_JSON
import com.woocommerce.commons.wear.DataParameters.ORDER_ID
import com.woocommerce.commons.wear.DataParameters.ORDER_PRODUCTS_JSON
import com.woocommerce.commons.wear.orders.WearOrderProduct
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCWearableStore

class OrdersRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.ORDERS) private val ordersDataStore: DataStore<Preferences>,
    private val loginRepository: LoginRepository,
    private val wearableStore: WCWearableStore,
    private val orderStore: WCOrderStore
) {
    private val gson by lazy { Gson() }

    suspend fun fetchOrders(
        selectedSite: SiteModel
    ) = wearableStore.fetchOrders(
        site = selectedSite,
        shouldStoreData = true
    )

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

    fun observeOrdersDataChanges(
        siteId: Long
    ) = ordersDataStore.data
        .mapNotNull { it[stringPreferencesKey(generateOrdersKey(siteId))] }
        .map { gson.fromJson(it, Array<OrderEntity>::class.java).toList() }

    suspend fun receiveOrdersDataFromPhone(data: DataMap) {
        val ordersJson = data.getString(ORDERS_JSON.value, "")
        val siteId = data.getSiteId(loginRepository.selectedSite)
        val receivedOrders = gson.fromJson(ordersJson, Array<OrderEntity>::class.java).toList()
        wearableStore.insertOrders(receivedOrders)

        ordersDataStore.edit { prefs ->
            prefs[stringPreferencesKey(generateOrdersKey(siteId))] = ordersJson
        }
    }

    private fun generateOrdersKey(siteId: Long) = "${ORDERS_KEY_PREFIX}:$siteId"

    fun observeOrderProductsDataChanges(
        orderId: Long,
        siteId: Long
    ) = ordersDataStore.data
        .mapNotNull { it[stringPreferencesKey(generateProductsKey(orderId, siteId))] }
        .map { gson.fromJson(it, Array<WearOrderProduct>::class.java).toList() }

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

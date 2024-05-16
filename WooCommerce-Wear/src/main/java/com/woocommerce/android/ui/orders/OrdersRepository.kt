package com.woocommerce.android.ui.orders

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.wearable.DataMap
import com.google.gson.Gson
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.commons.wear.DataParameters.ORDERS_JSON
import com.woocommerce.commons.wear.DataParameters.ORDER_ID
import com.woocommerce.commons.wear.DataParameters.ORDER_PRODUCTS_JSON
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class OrdersRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.ORDERS) private val ordersDataStore: DataStore<Preferences>,
    private val loginRepository: LoginRepository,
    private val orderStore: WCOrderStore
) {
    private val gson by lazy { Gson() }

    suspend fun fetchOrders(
        selectedSite: SiteModel
    ) = orderStore.fetchOrdersForWearables(
        site = selectedSite,
        shouldStoreData = true
    )

    suspend fun getOrderFromId(
        selectedSite: SiteModel,
        orderId: Long
    ) = orderStore.getOrderByIdAndSite(
        site = selectedSite,
        orderId = orderId
    )

    fun observeOrdersDataChanges() = ordersDataStore.data
        .mapNotNull { it[stringPreferencesKey(generateOrdersKey())] }
        .map { gson.fromJson(it, Array<OrderEntity>::class.java).toList() }

    suspend fun receiveOrdersDataFromPhone(data: DataMap) {
        val ordersJson = data.getString(ORDERS_JSON.value, "")

        ordersDataStore.edit { prefs ->
            prefs[stringPreferencesKey(generateOrdersKey())] = ordersJson
        }
    }

    suspend fun receiveOrderProductsDataFromPhone(data: DataMap) {
        val orderId = data.getLong(ORDER_ID.value, 0)
        val productsJson = data.getString(ORDER_PRODUCTS_JSON.value, "")

        ordersDataStore.edit { prefs ->
            prefs[stringPreferencesKey(generateProductsKey(orderId))] = productsJson
        }
    }

    private fun generateOrdersKey(): String {
        val siteId = loginRepository.selectedSite?.siteId ?: 0
        return "${ORDERS_KEY_PREFIX}:$siteId"
    }

    private fun generateProductsKey(orderId: Long): String {
        val siteId = loginRepository.selectedSite?.siteId ?: 0
        return "${ORDERS_KEY_PREFIX}:$siteId:$orderId"
    }

    companion object {
        private const val ORDERS_KEY_PREFIX = "store-orders"
    }
}

@file:Suppress("MagicNumber")

package org.wordpress.android.fluxc.wc.order

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderFulfillmentModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderNoteApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderShipmentTrackingApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderSummaryApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.toDataModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.persistence.entity.OrderNoteEntity
import org.wordpress.android.fluxc.site.SiteUtils
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.fluxc.wc.utils.TestSiteSqlUtils
import kotlin.collections.MutableMap.MutableEntry

@Suppress("TooManyFunctions")
object OrderTestUtils {
    @Suppress("LongParameterList")
    fun generateSampleOrder(
        orderId: Long,
        orderStatus: String = CoreOrderStatus.PROCESSING.value,
        siteId: Int = 6,
        modified: String = "1955-11-05T14:15:00Z",
        paymentMethod: String = "",
        paymentMethodTitle: String = ""
    ): OrderEntity {
        return OrderEntity(
            orderId = orderId,
            localSiteId = LocalId(siteId),
            status = orderStatus,
            dateModified = modified,
            dateCreated = "1955-11-05T14:15:00Z",
            datePaid = "1956-11-05T14:15:00Z",
            currency = "USD",
            total = "10.0",
            paymentMethod = paymentMethod,
            paymentMethodTitle = paymentMethodTitle,
        )
    }

    fun generateSampleOrderSummary(
        id: Number,
        remoteId: Number,
        modified: String = "1955-11-05T14:15:00Z"
    ): WCOrderSummaryModel {
        return WCOrderSummaryModel(
            siteId = LocalId(id.toInt()),
            orderId = RemoteId(remoteId.toLong()),
            dateCreated = "1955-11-05T14:15:00Z",
        ).apply {
            dateModified = modified
        }
    }

    fun getOrderNotesFromJsonString(json: String, localSiteId: Int, orderId: Long): List<OrderNoteEntity> {
        val responseType = object : TypeToken<List<OrderNoteApiResponse>>() {}.type
        val converted = Gson().fromJson(json, responseType) as? List<OrderNoteApiResponse> ?: emptyList()
        return converted.map {
            it.toDataModel(localSiteId = LocalId(localSiteId), orderId = RemoteId(orderId))
        }
    }

    fun getOrderShipmentTrackingsFromJson(
        json: String,
        siteId: Int,
        orderId: Long
    ): List<WCOrderShipmentTrackingModel> {
        val responseType = object : TypeToken<List<OrderShipmentTrackingApiResponse>>() {}.type
        val converted = Gson().fromJson(json, responseType) as? List<OrderShipmentTrackingApiResponse> ?: emptyList()
        return converted.map {
            WCOrderShipmentTrackingModel(
                localSiteId = LocalId(siteId),
                orderId = RemoteId(orderId),
                remoteTrackingId = it.tracking_id ?: "",
                trackingNumber = it.tracking_number ?: "",
                trackingProvider = it.tracking_provider ?: "",
                trackingLink = it.tracking_link ?: "",
                dateShipped = it.date_shipped ?: ""
            )
        }
    }

    fun generateOrderShipmentTracking(siteId: Int, orderId: Long): WCOrderShipmentTrackingModel {
        return WCOrderShipmentTrackingModel(
            localSiteId = LocalId(siteId),
            orderId = RemoteId(orderId),
            remoteTrackingId = "3290834092801",
            trackingNumber = "ZZ9939921",
            trackingProvider = "USPS",
            trackingLink = "",
            dateShipped = "2019-01-01"
        )
    }

    fun generateOrderFulfillment(
        siteId: Int,
        orderId: Long,
        fulfillmentId: Long = 42L,
        status: String = "fulfilled",
        isFulfilled: Boolean = true,
        dateUpdated: String? = "2026-03-18 21:00:00",
        dateFulfilled: String? = "2026-03-18 14:30:00",
        trackingNumber: String? = "1Z999AA10123456784",
        shipmentProvider: String? = "ups",
        trackingUrl: String? = "https://www.ups.com/track?tracknum=1Z999AA10123456784"
    ) = WCOrderFulfillmentModel(
        localSiteId = LocalId(siteId),
        orderId = RemoteId(orderId),
        fulfillmentId = fulfillmentId,
        status = status,
        isFulfilled = isFulfilled,
        dateUpdated = dateUpdated,
        dateFulfilled = dateFulfilled,
        trackingNumber = trackingNumber,
        shipmentProvider = shipmentProvider,
        trackingUrl = trackingUrl
    )

    fun getOrderShipmentProvidersFromJson(json: String, siteId: Int): List<WCOrderShipmentProviderModel> {
        val providers = mutableListOf<WCOrderShipmentProviderModel>()
        val jsonElement = JsonParser().parse(json)
        jsonElement.asJsonObject.entrySet().forEach { countryEntry: MutableEntry<String, JsonElement> ->
            countryEntry.value.asJsonObject.entrySet().map { carrierEntry ->
                carrierEntry?.let { carrier ->
                    val provider = WCOrderShipmentProviderModel(
                        localSiteId = LocalId(siteId),
                        country = countryEntry.key,
                        carrierName = carrier.key,
                        carrierLink = carrier.value.asString
                    )
                    providers.add(provider)
                }
            }
        }
        return providers
    }

    fun generateOrderShipmentProvider(siteId: Int): WCOrderShipmentProviderModel {
        return WCOrderShipmentProviderModel(
            localSiteId = LocalId(siteId),
            country = "Australia",
            carrierName = "Amanda Test",
            carrierLink = "http://google.com"
        )
    }

    fun getOrderSummariesFromJsonString(json: String, siteId: Int): List<WCOrderSummaryModel> {
        val responseType = object : TypeToken<List<OrderSummaryApiResponse>>() {}.type
        val converted = Gson().fromJson(json, responseType) as? List<OrderSummaryApiResponse> ?: emptyList()
        return converted.map { response ->
            WCOrderSummaryModel(
                siteId = LocalId(siteId),
                orderId = RemoteId(response.id ?: 0),
                dateCreated = response.dateCreatedGmt?.let { DateUtils.formatGmtAsUtcDateString(it) } ?: "",
            ).apply {
                dateModified = response.dateModifiedGmt?.let { DateUtils.formatGmtAsUtcDateString(it) } ?: ""
            }
        }
    }

    fun getAndSaveTestSite(): SiteModel {
        var siteModel = SiteUtils.generateTestSite(556, "", "", false, true).apply {
            name = "Generic WP site"
        }
        TestSiteSqlUtils.siteStorePersistence.insertOrUpdateSite(siteModel)
        siteModel = SiteSqlUtils().getSitesByNameOrUrlMatching("Generic").firstOrNull()
        assert(siteModel != null)

        return siteModel
    }

    fun getTestOrderSummaryList(site: SiteModel): List<WCOrderSummaryModel> {
        val json = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order-summaries.json")
        val summaryList = getOrderSummariesFromJsonString(json, site.id)
        assert(summaryList.size == 10)

        return summaryList
    }

    fun getTestOrderSummaryExtendedList(site: SiteModel): List<WCOrderSummaryModel> {
        val json = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order-summaries-extended.json")
        val summaryList = getOrderSummariesFromJsonString(json, site.id)
        assert(summaryList.size == 300)

        return summaryList
    }
}

package com.woocommerce.android.ui.woopos.bookings.data

import android.util.Log
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.bookings.WooPosBooking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.utils.toWooPayload
import java.time.LocalDate
import javax.inject.Inject

class WooPosBookingsApiService @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooNetwork: WooNetwork
) {
    suspend fun fetchBookings(
        page: Int = 1,
        perPage: Int = 50
    ): Result<List<WooPosBooking>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "fetchBookings API call: page=$page, perPage=$perPage")
        try {
            val site = selectedSite.get()
            val params = mapOf(
                "page" to page.toString(),
                "per_page" to perPage.toString()
            )
            
            Log.d(TAG, "fetchBookings API: making request to wc-bookings/v1/bookings with params=$params")

            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = "wc-bookings/v1/bookings",
                clazz = Array::class.java,
                params = params
            ).toWooPayload { bookingsArray ->
                (bookingsArray as? Array<*>)?.mapNotNull { booking ->
                    (booking as? Map<*, *>)?.let { bookingMap ->
                        parseBookingFromMap(bookingMap.mapKeys { it.key.toString() }.mapValues { it.value ?: "" })
                    }
                } ?: emptyList()
            }

            if (response.isError) {
                Log.e(TAG, "fetchBookings API error: ${response.error?.message}")
                return@withContext Result.failure(Exception("API Error: ${response.error?.message}"))
            }

            val result = response.result ?: emptyList()
            Log.d(TAG, "fetchBookings API success: returned ${result.size} bookings")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "fetchBookings API exception: page=$page", e)
            Result.failure(e)
        }
    }

    suspend fun fetchBookingSlots(
        productIds: List<Int>,
        minDate: LocalDate,
        maxDate: LocalDate
    ): Result<List<BookingSlot>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "fetchBookingSlots API call: productIds=$productIds, minDate=$minDate, maxDate=$maxDate")
        try {
            val site = selectedSite.get()
            val params = mapOf(
                "min_date" to minDate.toString(),
                "max_date" to maxDate.toString(),
                "product_ids" to productIds.joinToString(",")
            )

            Log.d(TAG, "fetchBookingSlots API: making request to wc-bookings/v1/products/slots with params=$params")

            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = "wc-bookings/v1/products/slots",
                clazz = Map::class.java,
                params = params
            ).toWooPayload { slotsMap ->
                val records = slotsMap["records"] as? List<*>
                records?.mapNotNull { record ->
                    (record as? Map<*, *>)?.let { parseSlotFromMap(it) }
                } ?: emptyList()
            }

            if (response.isError) {
                Log.e(TAG, "fetchBookingSlots API error: ${response.error?.message}")
                return@withContext Result.failure(Exception("API Error: ${response.error?.message}"))
            }

            val result = response.result ?: emptyList()
            Log.d(TAG, "fetchBookingSlots API success: returned ${result.size} slots")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "fetchBookingSlots API exception: productIds=$productIds", e)
            Result.failure(e)
        }
    }

    suspend fun confirmBooking(bookingId: Long): Result<WooPosBooking> = withContext(Dispatchers.IO) {
        Log.d(TAG, "confirmBooking API call: bookingId=$bookingId")
        try {
            val site = selectedSite.get()
            val params = mapOf(
                "status" to "confirmed"
            )

            Log.d(TAG, "confirmBooking API: making PUT request to wc-bookings/v1/bookings/$bookingId")

            val response = wooNetwork.executePutGsonRequest(
                site = site,
                path = "wc-bookings/v1/bookings/$bookingId",
                clazz = Map::class.java,
                body = params
            ).toWooPayload { bookingMap ->
                (bookingMap as? Map<*, *>)?.let { 
                    parseBookingFromMap(it.mapKeys { it.key.toString() }.mapValues { it.value ?: "" })
                }
            }

            if (response.isError) {
                Log.e(TAG, "confirmBooking API error: ${response.error?.message}")
                return@withContext Result.failure(Exception("API Error: ${response.error?.message}"))
            }

            val result = response.result
            if (result != null) {
                Log.d(TAG, "confirmBooking API success: booking confirmed with status ${result.status}")
                Result.success(result)
            } else {
                Log.e(TAG, "confirmBooking API error: null response")
                Result.failure(Exception("Failed to confirm booking: null response"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "confirmBooking API exception: bookingId=$bookingId", e)
            Result.failure(e)
        }
    }

    private fun parseBookingFromMap(map: Map<String, Any>): WooPosBooking {
        return WooPosBooking(
            id = (map["id"] as? Number)?.toLong() ?: 0L,
            allDay = map["all_day"] as? Boolean ?: false,
            cost = map["cost"] as? String ?: "0",
            customerId = (map["customer_id"] as? Number)?.toInt() ?: 0,
            dateCreated = (map["date_created"] as? Number)?.toLong() ?: 0L,
            dateModified = (map["date_modified"] as? Number)?.toLong() ?: 0L,
            start = (map["start"] as? Number)?.toLong() ?: 0L,
            end = (map["end"] as? Number)?.toLong() ?: 0L,
            googleCalendarEventId = map["google_calendar_event_id"] as? String,
            orderId = (map["order_id"] as? Number)?.toInt() ?: 0,
            orderItemId = (map["order_item_id"] as? Number)?.toInt() ?: 0,
            parentId = (map["parent_id"] as? Number)?.toInt() ?: 0,
            personCounts = (map["person_counts"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
            productId = (map["product_id"] as? Number)?.toInt() ?: 0,
            resourceId = (map["resource_id"] as? Number)?.toInt() ?: 0,
            status = map["status"] as? String ?: "unpaid",
            localTimezone = map["local_timezone"] as? String ?: ""
        )
    }

    private fun parseSlotFromMap(map: Map<*, *>): BookingSlot? {
        return try {
            BookingSlot(
                productId = (map["product_id"] as? Number)?.toInt() ?: 0,
                date = map["date"] as? String ?: "",
                duration = (map["duration"] as? Number)?.toInt() ?: 0,
                available = (map["available"] as? Number)?.toInt() ?: 0,
                booked = (map["booked"] as? Number)?.toInt() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class BookingSlot(
    val productId: Int,
    val date: String,
    val duration: Int,
    val available: Int,
    val booked: Int
) {
    val isAvailable: Boolean get() = available > 0
    val isBooked: Boolean get() = booked > 0
}

const val TAG = "WooPosBookingsApiService"

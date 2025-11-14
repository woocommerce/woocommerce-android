package com.woocommerce.android.ui.bookings.filter.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType.BOOKINGS_FILTERS
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.Instant
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class BookingFilterRepository @Inject constructor(
    @DataStoreQualifier(BOOKINGS_FILTERS) private val dataStore: DataStore<Preferences>,
    private val selectedSite: SelectedSite,
) {
    // Keys are built per-site to keep selections isolated across sites
    private fun bookingTypeKey(siteId: Int) = stringPreferencesKey("bfilters_${siteId}_booking_type")
    private fun attendanceStatusesKey(siteId: Int) = stringPreferencesKey("bfilters_${siteId}_attendance_statuses")
    private fun customerIdKey(siteId: Int) = longPreferencesKey("bfilters_${siteId}_customer_id")
    private fun customerNameKey(siteId: Int) = stringPreferencesKey("bfilters_${siteId}_customer_name")
    private fun dateBeforeKey(siteId: Int) = longPreferencesKey("bfilters_${siteId}_date_before")
    private fun dateAfterKey(siteId: Int) = longPreferencesKey("bfilters_${siteId}_date_after")

    private val siteIdFlow = selectedSite.observe().map { it?.id ?: -1 }.distinctUntilChanged()

    val bookingFiltersFlow: Flow<BookingFilters> = siteIdFlow.flatMapLatest { siteId ->
        dataStore.data.map { prefs ->
            BookingFilters(
                bookingType = prefs.getBookingType(siteId),
                attendanceStatuses = prefs.getAttendanceStatuses(siteId)
                    ?: BookingsFilterOption.AttendanceStatuses.DEFAULT,
                customer = prefs.getCustomerValue(siteId),
                dateRange = prefs.getDateRangeValue(siteId)
            )
        }
    }

    suspend fun save(bookingFilters: BookingFilters) {
        val siteId = selectedSite.getSelectedSiteId()
        dataStore.edit { prefs ->
            // Booking type
            val bookingTypeKey = bookingTypeKey(siteId)
            val bookingTypeValue = bookingFilters.bookingType?.value?.name
            if (bookingTypeValue != null) {
                prefs[bookingTypeKey] = bookingTypeValue
            } else {
                // Clear if not provided
                prefs.remove(bookingTypeKey)
            }

            // Attendance statuses
            val attendanceStatusesKey = attendanceStatusesKey(siteId)
            val attendanceStatusesValues = bookingFilters.attendanceStatuses.values
            if (attendanceStatusesValues.isEmpty()) {
                prefs.remove(attendanceStatusesKey)
            } else {
                prefs[attendanceStatusesKey] = attendanceStatusesValues
                    .joinToString(ATTENDANCE_STATUSES_DELIMITER) { it.key }
            }

            // Customer
            val customerIdKey = customerIdKey(siteId)
            val customerNameKey = customerNameKey(siteId)
            val customer = bookingFilters.customer
            if (customer != null) {
                val id = customer.customerId
                val name = customer.customerName
                prefs[customerIdKey] = id
                prefs[customerNameKey] = name
            } else {
                // Clear if not provided
                prefs.remove(customerIdKey)
                prefs.remove(customerNameKey)
            }

            // Date range
            val dateRange = bookingFilters.dateRange
            val beforeKey = dateBeforeKey(siteId)
            val afterKey = dateAfterKey(siteId)
            if (dateRange != null) {
                val before = dateRange.before?.toEpochMilli()
                val after = dateRange.after?.toEpochMilli()
                if (before == null) prefs.remove(beforeKey) else prefs[beforeKey] = before
                if (after == null) prefs.remove(afterKey) else prefs[afterKey] = after
            } else {
                // Clear if not provided
                prefs.remove(beforeKey)
                prefs.remove(afterKey)
            }

            // Other filters currently have no persisted payload; ignore for now
        }
    }

    private fun Preferences.getBookingType(siteId: Int): BookingsFilterOption.BookingType? {
        val stored = this[bookingTypeKey(siteId)] ?: return null
        val value = runCatching { BookingsFilterOption.BookingType.Type.valueOf(stored) }.getOrNull()
        return value?.let { BookingsFilterOption.BookingType(value = it) }
    }

    private fun Preferences.getAttendanceStatuses(siteId: Int): BookingsFilterOption.AttendanceStatuses? {
        val stored = this[attendanceStatusesKey(siteId)] ?: return null
        val set = stored.split(ATTENDANCE_STATUSES_DELIMITER)
            .mapNotNull { runCatching { BookingEntity.AttendanceStatus.fromKey(it) }.getOrNull() }
            .filterNot { it is BookingEntity.AttendanceStatus.Unknown }
            .toSet()
        return BookingsFilterOption.AttendanceStatuses(set)
    }

    private fun Preferences.getCustomerValue(siteId: Int): BookingsFilterOption.Customer? {
        val customerId = this[customerIdKey(siteId)]
        val customerName = this[customerNameKey(siteId)]
        return if (customerId != null && customerName != null) {
            BookingsFilterOption.Customer(customerId = customerId, customerName = customerName)
        } else {
            null
        }
    }

    private fun Preferences.getDateRangeValue(siteId: Int): BookingsFilterOption.DateRange? {
        val before = this[dateBeforeKey(siteId)]?.let { Instant.ofEpochMilli(it) }
        val after = this[dateAfterKey(siteId)]?.let { Instant.ofEpochMilli(it) }
        return if (before != null || after != null) {
            BookingsFilterOption.DateRange(before = before, after = after)
        } else {
            null
        }
    }

    companion object {
        private const val ATTENDANCE_STATUSES_DELIMITER = ","
    }
}

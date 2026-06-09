package com.woocommerce.android.ui.bookings.filter.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType.BOOKINGS_FILTERS
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.LocalOrRemoteId
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
    private fun teamMembersKey(siteId: Int) = stringSetPreferencesKey("bfilters_${siteId}_team_members")
    private fun bookingTypeKey(siteId: Int) = stringPreferencesKey("bfilters_${siteId}_booking_type")
    private fun attendanceStatusKey(siteId: Int) = stringPreferencesKey("bfilters_${siteId}_attendance_status")

    @Deprecated("Legacy key for migration")
    private fun legacyAttendanceStatusesKey(siteId: Int) =
        stringSetPreferencesKey("bfilters_${siteId}_attendance_statuses")

    private fun customerIdKey(siteId: Int) = longPreferencesKey("bfilters_${siteId}_customer_id")
    private fun customerNameKey(siteId: Int) = stringPreferencesKey("bfilters_${siteId}_customer_name")
    private fun dateBeforeKey(siteId: Int) = longPreferencesKey("bfilters_${siteId}_date_before")
    private fun dateAfterKey(siteId: Int) = longPreferencesKey("bfilters_${siteId}_date_after")
    private fun serviceEventsKey(siteId: Int) = stringSetPreferencesKey("bfilters_${siteId}_service_events")

    private val siteIdFlow = selectedSite.observe().map { it?.id ?: -1 }.distinctUntilChanged()

    val bookingFiltersFlow: Flow<BookingFilters> = siteIdFlow.flatMapLatest { siteId ->
        dataStore.data.map { prefs ->
            BookingFilters(
                teamMembers = prefs.getTeamMembers(siteId) ?: BookingsFilterOption.TeamMembers.DEFAULT,
                bookingType = prefs.getBookingType(siteId),
                attendanceStatus = prefs.getAttendanceStatus(siteId),
                customer = prefs.getCustomerValue(siteId),
                dateRange = prefs.getDateRangeValue(siteId),
                serviceEvents = prefs.getServiceEventsValue(siteId)
                    ?: BookingsFilterOption.ServiceEvents.DEFAULT
            )
        }
    }

    suspend fun save(bookingFilters: BookingFilters) {
        val siteId = selectedSite.getSelectedSiteId()
        dataStore.edit { prefs ->
            val teamMembersKey = teamMembersKey(siteId)
            val teamMembersValues = bookingFilters.teamMembers.values
            if (teamMembersValues.isEmpty()) {
                prefs.remove(teamMembersKey)
            } else {
                prefs[teamMembersKey] = teamMembersValues.map { it.value.toString() }.toSet()
            }

            val bookingTypeKey = bookingTypeKey(siteId)
            val bookingTypeValue = bookingFilters.bookingType?.value?.name
            if (bookingTypeValue != null) {
                prefs[bookingTypeKey] = bookingTypeValue
            } else {
                prefs.remove(bookingTypeKey)
            }

            val attendanceStatusValue = bookingFilters.attendanceStatus.value?.key
            if (attendanceStatusValue != null) {
                prefs[attendanceStatusKey(siteId)] = attendanceStatusValue
            } else {
                prefs.remove(attendanceStatusKey(siteId))
            }
            @Suppress("DEPRECATION")
            prefs.remove(legacyAttendanceStatusesKey(siteId))

            val customerIdKey = customerIdKey(siteId)
            val customerNameKey = customerNameKey(siteId)
            val customer = bookingFilters.customer
            if (customer != null) {
                prefs[customerIdKey] = customer.userId
                prefs[customerNameKey] = customer.customerName
            } else {
                // Clear if not provided
                prefs.remove(customerIdKey)
                prefs.remove(customerNameKey)
            }

            // Date range
            val dateRange = bookingFilters.dateRange
            val beforeKey = dateBeforeKey(siteId)
            val afterKey = dateAfterKey(siteId)
            if (dateRange != BookingsFilterOption.DateRange.DEFAULT) {
                val before = dateRange.before?.toEpochMilli()
                val after = dateRange.after?.toEpochMilli()
                if (before == null) prefs.remove(beforeKey) else prefs[beforeKey] = before
                if (after == null) prefs.remove(afterKey) else prefs[afterKey] = after
            } else {
                // Clear if not provided
                prefs.remove(beforeKey)
                prefs.remove(afterKey)
            }

            // Service events
            val serviceEventsKey = serviceEventsKey(siteId)
            val serviceEventsValues = bookingFilters.serviceEvents.values
            if (serviceEventsValues.isEmpty()) {
                prefs.remove(serviceEventsKey)
            } else {
                prefs[serviceEventsKey] = serviceEventsValues
                    .map { "${it.productId}${SERVICE_EVENTS_PRODUCT_DELIMITER}${it.productName}" }
                    .toSet()
            }
        }
    }

    private fun Preferences.getTeamMembers(siteId: Int): BookingsFilterOption.TeamMembers? {
        val stored = this[teamMembersKey(siteId)] ?: return null
        val set = stored.mapNotNull { runCatching { LocalOrRemoteId.RemoteId(it.toLong()) }.getOrNull() }
            .toSet()
        return BookingsFilterOption.TeamMembers(set)
    }

    private fun Preferences.getBookingType(siteId: Int): BookingsFilterOption.BookingType? {
        val stored = this[bookingTypeKey(siteId)] ?: return null
        val value = runCatching { BookingsFilterOption.BookingType.Type.valueOf(stored) }.getOrNull()
        return value?.let { BookingsFilterOption.BookingType(value = it) }
    }

    private fun Preferences.getAttendanceStatus(siteId: Int): BookingsFilterOption.AttendanceStatus {
        val stored = this[attendanceStatusKey(siteId)]
            ?: return migrateLegacyAttendanceStatus(siteId)
        val status = runCatching { BookingEntity.AttendanceStatus.fromKey(stored) }.getOrNull()
            ?.takeIf { it !is BookingEntity.AttendanceStatus.Unknown }
        return BookingsFilterOption.AttendanceStatus(status)
    }

    @Suppress("DEPRECATION")
    private fun Preferences.migrateLegacyAttendanceStatus(siteId: Int): BookingsFilterOption.AttendanceStatus {
        val legacy = this[legacyAttendanceStatusesKey(siteId)]
            ?: return BookingsFilterOption.AttendanceStatus.DEFAULT
        val status = legacy.firstNotNullOfOrNull { key ->
            runCatching { BookingEntity.AttendanceStatus.fromKey(key) }.getOrNull()
                ?.takeIf { it !is BookingEntity.AttendanceStatus.Unknown }
        }
        return BookingsFilterOption.AttendanceStatus(status)
    }

    private fun Preferences.getCustomerValue(siteId: Int): BookingsFilterOption.Customer? {
        val customerId = this[customerIdKey(siteId)]
        val customerName = this[customerNameKey(siteId)]
        return if (customerId != null && customerName != null) {
            BookingsFilterOption.Customer(userId = customerId, customerName = customerName)
        } else {
            null
        }
    }

    private fun Preferences.getDateRangeValue(siteId: Int): BookingsFilterOption.DateRange {
        val before = this[dateBeforeKey(siteId)]?.let { Instant.ofEpochMilli(it) }
        val after = this[dateAfterKey(siteId)]?.let { Instant.ofEpochMilli(it) }
        return if (before != null || after != null) {
            BookingsFilterOption.DateRange(before = before, after = after)
        } else {
            BookingsFilterOption.DateRange.DEFAULT
        }
    }

    private fun Preferences.getServiceEventsValue(siteId: Int): BookingsFilterOption.ServiceEvents? =
        this[serviceEventsKey(siteId)]?.mapNotNull { entry ->
            entry.split(SERVICE_EVENTS_PRODUCT_DELIMITER)
                .takeIf { it.size == 2 }
                ?.let { (prodId, name) ->
                    runCatching {
                        BookingsFilterOption.ProductInfo(productId = prodId.toLong(), productName = name)
                    }.getOrNull()
                }
        }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?.let(BookingsFilterOption::ServiceEvents)

    companion object {
        private const val SERVICE_EVENTS_PRODUCT_DELIMITER = ":"
    }
}

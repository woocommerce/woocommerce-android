package com.woocommerce.android.ui.bookings

import com.automattic.eventhorizon.BookingAttendanceValueType
import com.automattic.eventhorizon.BookingSortValueType
import com.automattic.eventhorizon.BookingTabValueType
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.list.BookingListSortOption
import com.woocommerce.android.ui.bookings.list.BookingListTab

fun BookingListTab.toEventHorizonValue(): BookingTabValueType = when (this) {
    BookingListTab.Today -> BookingTabValueType.Today
    BookingListTab.Upcoming -> BookingTabValueType.Upcoming
    BookingListTab.All -> BookingTabValueType.All
}

fun BookingListSortOption.toEventHorizonValue(): BookingSortValueType = when (this) {
    BookingListSortOption.NewestToOldest -> BookingSortValueType.NewestFirst
    BookingListSortOption.OldestToNewest -> BookingSortValueType.OldestFirst
}

fun BookingAttendanceStatus.toEventHorizonValue(): BookingAttendanceValueType = when (this) {
    BookingAttendanceStatus.Attended -> BookingAttendanceValueType.Attended
    BookingAttendanceStatus.Unattended -> BookingAttendanceValueType.Unattended
}

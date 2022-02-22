package com.woocommerce.android.ui.inbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val dateutils: DateUtils,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    val inboxState = loadInboxNotes().asLiveData()

    private fun loadInboxNotes(): Flow<InboxState> = flow {
        emit(
            InboxState(
                notes = listOf(
                    InboxNoteUi(
                        id = "1",
                        title = "Install the Facebook free extension",
                        description = "Now that your store is set up, you’re ready to begin marketing it. " +
                            "Head over to the WooCommerce marketing panel to get started.",
                        updatedTime = getRelativeDateToCurrentDate("2022-02-22T04:47:37"),
                        callToActionText = "Learn more",
                        onCallToActionClick = {},
                        dismissText = resourceProvider.getString(R.string.dismiss_inbox_note),
                        onDismissNote = {},
                        isRead = false
                    ),
                    InboxNoteUi(
                        id = "2",
                        title = "Connect with your audience",
                        description = "Grow your customer base and increase your sales with marketing tools " +
                            "built for WooCommerce.",
                        updatedTime = getRelativeDateToCurrentDate("2021-10-22T04:47:37"),
                        callToActionText = "Learn more",
                        onCallToActionClick = {},
                        dismissText = resourceProvider.getString(R.string.dismiss_inbox_note),
                        onDismissNote = {},
                        isRead = false
                    ),
                    InboxNoteUi(
                        id = "1",
                        title = "Install the Facebook free extension",
                        description = "Now that your store is set up, you’re ready to begin marketing it. " +
                            "Head over to the WooCommerce marketing panel to get started.",
                        updatedTime = getRelativeDateToCurrentDate("2022-01-25T04:47:37"),
                        callToActionText = "Learn more",
                        onCallToActionClick = {},
                        dismissText = resourceProvider.getString(R.string.dismiss_inbox_note),
                        onDismissNote = {},
                        isRead = false
                    ),
                    InboxNoteUi(
                        id = "1",
                        title = "Install the Facebook free extension",
                        description = "Now that your store is set up, you’re ready to begin marketing it. " +
                            "Head over to the WooCommerce marketing panel to get started.",
                        updatedTime = getRelativeDateToCurrentDate("2022-02-21T04:47:37"),
                        callToActionText = "Learn more",
                        onCallToActionClick = {},
                        dismissText = resourceProvider.getString(R.string.dismiss_inbox_note),
                        onDismissNote = {},
                        isRead = false
                    )
                )
            )
        )
    }

    @SuppressWarnings("MagicNumber", "ReturnCount")
    private fun getRelativeDateToCurrentDate(createdDate: String): String {
        val creationDate = dateutils.getDateFromIso8601String(createdDate)
        val now = Date()

        val minutes = DateTimeUtils.minutesBetween(now, creationDate)
        when {
            minutes < 1 -> return resourceProvider.getString(R.string.inbox_note_recency_now)
            minutes < 60 -> return resourceProvider.getString(R.string.inbox_note_recency_minutes, minutes)
        }
        val hours = DateTimeUtils.hoursBetween(now, creationDate)
        when {
            hours == 1 -> return resourceProvider.getString(R.string.inbox_note_recency_one_hour)
            hours < 24 -> return resourceProvider.getString(R.string.inbox_note_recency_hours, hours)
        }
        val days = DateTimeUtils.daysBetween(now, creationDate)
        when {
            days == 1 -> return resourceProvider.getString(R.string.inbox_note_recency_one_day)
            days < 30 -> return resourceProvider.getString(R.string.inbox_note_recency_days, days)
        }
        return resourceProvider.getString(
            R.string.inbox_note_recency_date_time,
            dateutils.toDisplayMMMddYYYYDate(creationDate?.time ?: 0) ?: ""
        )
    }

    data class InboxState(
        val isLoading: Boolean = false,
        val isEmpty: Boolean = false,
        val isError: Boolean = false,
        val notes: List<InboxNoteUi> = emptyList()
    )

    data class InboxNoteUi(
        val id: String,
        val title: String,
        val description: String,
        val updatedTime: String,
        val callToActionText: String,
        val onCallToActionClick: (String) -> Unit,
        val dismissText: String,
        val onDismissNote: (String) -> Unit,
        val isRead: Boolean
    )
}

package com.woocommerce.android.ui.inbox

import android.os.Build
import android.text.Html
import androidx.annotation.ColorRes
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.compose.utils.toAnnotatedString
import com.woocommerce.android.ui.inbox.domain.FetchInboxNotes
import com.woocommerce.android.ui.inbox.domain.InboxNote
import com.woocommerce.android.ui.inbox.domain.InboxNoteAction
import com.woocommerce.android.ui.inbox.domain.ObserveInboxNotes
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val dateUtils: DateUtils,
    private val fetchInboxNotes: FetchInboxNotes,
    private val observeInboxNotes: ObserveInboxNotes,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    val inboxState: LiveData<InboxState> =
        refreshInboxNotes()
            .combine(inboxNotesLocalUpdates()) { fetchInboxState, localInboxState ->
                when {
                    fetchInboxState.isLoading -> fetchInboxState
                    else -> localInboxState
                }
            }.asLiveData()

    private fun inboxNotesLocalUpdates() =
        observeInboxNotes()
            .map { inboxNotes ->
                val notes = inboxNotes.map { it.toInboxNoteUi() }
                InboxState(isLoading = false, notes = notes)
            }

    private fun refreshInboxNotes(): Flow<InboxState> = flow {
        emit(InboxState(isLoading = true))
        fetchInboxNotes()
        emit(InboxState(isLoading = false))
    }

    private fun InboxNote.toInboxNoteUi() =
        InboxNoteUi(
            id = id,
            title = title,
            description = getContentFromHtml(description),
            dateCreated = formatNoteCreationDate(dateCreated),
            actions = actions.map { it.toInboxActionUi() },
        )

    private fun InboxNoteAction.toInboxActionUi() =
        InboxNoteActionUi(
            id = id,
            label = label,
            textColor = getActionTextColor(),
            url = url,
            onClick = {} // TODO set action lambda
        )

    private fun InboxNoteAction.getActionTextColor() =
        if (isPrimary) R.color.color_secondary
        else R.color.color_surface_variant

    private fun getContentFromHtml(htmlContent: String): AnnotatedString =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(htmlContent)
        }.toAnnotatedString()

    @SuppressWarnings("MagicNumber", "ReturnCount")
    private fun formatNoteCreationDate(createdDate: String): String {
        val creationDate = dateUtils.getDateFromIso8601String(createdDate)
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
            dateUtils.toDisplayMMMddYYYYDate(creationDate?.time ?: 0) ?: ""
        )
    }

    data class InboxState(
        val isLoading: Boolean = false,
        val notes: List<InboxNoteUi> = emptyList()
    )

    data class InboxNoteUi(
        val id: Long,
        val title: String,
        val description: AnnotatedString,
        val dateCreated: String,
        val actions: List<InboxNoteActionUi>
    )

    data class InboxNoteActionUi(
        val id: Long,
        val label: String,
        @ColorRes val textColor: Int,
        val url: String,
        val onClick: (String) -> Unit
    )
}

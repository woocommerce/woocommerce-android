package com.woocommerce.android.ui.inbox

import android.os.Build
import android.text.Html
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.compose.utils.toAnnotatedString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxNoteActionDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxNoteDto
import org.wordpress.android.fluxc.store.WCInboxStore
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val inboxStore: WCInboxStore,
    private val selectedSite: SelectedSite,
    private val dateutils: DateUtils,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    val inboxState = loadInboxNotes().asLiveData()

    private fun loadInboxNotes(): Flow<InboxState> = flow {
        emit(InboxState(isLoading = true))
        val result = inboxStore.fetchNotes(selectedSite.get())
        val notes = when {
            result.isError -> emptyList()
            else -> result.model?.map { it.toInboxNoteUi() } ?: emptyList()
        }
        emit(InboxState(isLoading = false, notes = notes))
    }

    private fun InboxNoteDto.toInboxNoteUi() =
        InboxNoteUi(
            id = id,
            title = title!!,
            description = getContentFromHtml(content!!),
            updatedTime = formatNoteCreationDate(dateCreated!!),
            actions = actions.map { it.toInboxActionUi() },
        )

    private fun InboxNoteActionDto.toInboxActionUi() =
        InboxNoteActionUi(
            id = id,
            label = label!!,
            primary = primary,
            url = url!!,
            onClick = {}
        )

    private fun getContentFromHtml(htmlContent: String): AnnotatedString =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(htmlContent)
        }.toAnnotatedString()

    @SuppressWarnings("MagicNumber", "ReturnCount")
    private fun formatNoteCreationDate(createdDate: String): String {
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
        val notes: List<InboxNoteUi> = emptyList()
    )

    data class InboxNoteUi(
        val id: Long,
        val title: String,
        val description: AnnotatedString,
        val updatedTime: String,
        val actions: List<InboxNoteActionUi>
    )

    data class InboxNoteActionUi(
        val id: Long,
        val label: String,
        val primary: Boolean = false,
        val url: String,
        val onClick: (String) -> Unit
    )
}

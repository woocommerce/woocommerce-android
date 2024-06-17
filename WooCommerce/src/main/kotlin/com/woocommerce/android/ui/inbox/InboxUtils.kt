package com.woocommerce.android.ui.inbox

import com.woocommerce.android.R
import com.woocommerce.android.ui.inbox.InboxViewModel.Companion.DEFAULT_DISMISS_LABEL
import com.woocommerce.android.ui.inbox.domain.InboxNote
import com.woocommerce.android.ui.inbox.domain.InboxNote.NoteType.SURVEY
import com.woocommerce.android.ui.inbox.domain.InboxNote.Status.ACTIONED
import com.woocommerce.android.ui.inbox.domain.InboxNoteAction
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

@SuppressWarnings("MagicNumber", "ReturnCount")
fun String.formatNoteCreationDate(dateUtils: DateUtils, resourceProvider: ResourceProvider): String {
    val creationDate = dateUtils.getDateFromIso8601String(this)
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

fun InboxNote.toInboxNoteUi(
    dateUtils: DateUtils,
    resourceProvider: ResourceProvider,
    onAction: (Long, Long) -> Unit,
    onDismiss: (Long, Long) -> Unit
) =
    InboxNoteUi(
        id = id,
        title = title,
        description = description,
        dateCreated = dateCreated.formatNoteCreationDate(dateUtils, resourceProvider),
        isSurvey = type == SURVEY,
        isActioned = status == ACTIONED,
        actions = mapInboxActionsToUi(onAction, onDismiss),
    )

fun InboxNote.mapInboxActionsToUi(
    onAction: (Long, Long) -> Unit,
    onDismiss: (Long, Long) -> Unit
): List<InboxNoteActionUi> {
    if (type == SURVEY && status == ACTIONED) {
        return emptyList()
    }

    val noteActionsUi = actions
        .map { it.toInboxActionUi(id, onAction) }
        .toMutableList()

    addDismissActionIfMissing(noteActionsUi, onDismiss)

    return noteActionsUi
}

fun InboxNote.addDismissActionIfMissing(
    noteActionsUi: MutableList<InboxNoteActionUi>,
    onDismiss: (Long, Long) -> Unit
) {
    if (!actionsHaveDismiss(noteActionsUi)) {
        noteActionsUi.add(
            InboxNoteActionUi(
                id = 0,
                parentNoteId = id,
                label = DEFAULT_DISMISS_LABEL,
                textColor = R.color.color_surface_variant,
                url = "",
                onClick = { actionId, noteId -> onDismiss(actionId, noteId) }
            )
        )
    }
}

fun actionsHaveDismiss(noteActionsUi: List<InboxNoteActionUi>) =
    noteActionsUi.any { it.label == DEFAULT_DISMISS_LABEL }

fun InboxNoteAction.toInboxActionUi(parentNoteId: Long, onClick: (Long, Long) -> Unit) =
    InboxNoteActionUi(
        id = id,
        parentNoteId = parentNoteId,
        label = label,
        textColor = getActionTextColor(),
        url = url,
        onClick = onClick
    )

fun InboxNoteAction.getActionTextColor() =
    if (isPrimary) {
        R.color.color_secondary
    } else {
        R.color.color_surface_variant
    }

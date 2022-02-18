package com.woocommerce.android.ui.inbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.ui.inbox.InboxViewModel.InboxNoteUi
import com.woocommerce.android.ui.inbox.InboxViewModel.InboxState

@Composable
fun Inbox(viewModel: InboxViewModel) {
    val inboxState by viewModel.inboxState.observeAsState(InboxState())
    Inbox(state = inboxState)
}

@Composable
fun Inbox(state: InboxState) {
    InboxNotesList(notes = state.notes)
}

@Composable
fun InboxNotesList(notes: List<InboxNoteUi>) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        items(notes) { note ->
            InboxNoteRow(note = note)
        }
    }
}

@Composable
fun InboxNoteRow(note: InboxNoteUi) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = note.updatedTime,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = note.title,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = note.description,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = note.callToActionText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = note.dismissText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

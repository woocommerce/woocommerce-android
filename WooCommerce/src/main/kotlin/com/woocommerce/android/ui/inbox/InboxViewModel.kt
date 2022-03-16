package com.woocommerce.android.ui.inbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxNoteDto
import org.wordpress.android.fluxc.store.WCInboxStore
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

    @Suppress("MagicNumber", "LongMethod")
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
            description = content!!,
            updatedTime = getReladateCreated!!,
            actions = emptyList(),
        )

    data class InboxState(
        val isLoading: Boolean = false,
        val notes: List<InboxNoteUi> = emptyList()
    )

    data class InboxNoteUi(
        val id: Long,
        val title: String,
        val description: String,
        val updatedTime: String,
        val actions: List<InboxNoteActionUi>
    )

    data class InboxNoteActionUi(
        val id: Long,
        val label: String,
        val primary: Boolean = false,
        val onClick: (String) -> Unit,
        val url: String
    )
}

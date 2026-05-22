package com.woocommerce.android.ui.aisupportchat

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@ExperimentalCoroutinesApi
class AiSupportChatHistoryViewModelTest : BaseUnitTest() {
    private val repository: SupportChatRepository = mock()

    private lateinit var viewModel: AiSupportChatHistoryViewModel

    @Before
    fun setUp() {
        viewModel = AiSupportChatHistoryViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository
        )
    }

    @Test
    fun `when history loads, then bookmarks are shown`() = testBlocking {
        val bookmark = createBookmark()
        whenever(repository.loadChatHistory()).thenReturn(listOf(bookmark))

        viewModel.loadHistory()

        val state = viewModel.viewState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.bookmarks).containsExactly(bookmark)
        assertThat(state.showError).isFalse()
    }

    @Test
    fun `given history load fails, when history loads, then error state is shown`() = testBlocking {
        whenever(repository.loadChatHistory()).thenThrow(RuntimeException("DB unavailable"))

        viewModel.loadHistory()

        val state = viewModel.viewState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.bookmarks).isEmpty()
        assertThat(state.showError).isTrue()
    }

    @Test
    fun `given history loaded, when refresh fails, then stale bookmarks are cleared`() = testBlocking {
        val bookmark = createBookmark()
        whenever(repository.loadChatHistory())
            .thenReturn(listOf(bookmark))
            .thenThrow(RuntimeException("DB unavailable"))

        viewModel.loadHistory()
        viewModel.loadHistory()

        val state = viewModel.viewState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.bookmarks).isEmpty()
        assertThat(state.showError).isTrue()
    }

    @Test
    fun `given history load is cancelled, when history loads, then error state is not shown`() = testBlocking {
        whenever(repository.loadChatHistory()).thenThrow(CancellationException("Cancelled"))

        viewModel.loadHistory()

        val state = viewModel.viewState.value
        assertThat(state.showError).isFalse()
        assertThat(state.bookmarks).isEmpty()
    }

    @Test
    fun `given history loaded, when deleting bookmark succeeds, then bookmark is removed`() = testBlocking {
        val bookmark = createBookmark(chatId = 1L)
        whenever(repository.loadChatHistory()).thenReturn(listOf(bookmark))
        viewModel.loadHistory()

        viewModel.onDeleteBookmark(bookmark)

        verify(repository).deleteChat(bookmark.chatId)
        assertThat(viewModel.viewState.value.bookmarks).isEmpty()
    }

    @Test
    fun `given multiple bookmarks loaded, when deleting bookmark succeeds, then only target bookmark is removed`() =
        testBlocking {
            val firstBookmark = createBookmark(chatId = 1L)
            val secondBookmark = createBookmark(chatId = 2L)
            whenever(repository.loadChatHistory()).thenReturn(listOf(firstBookmark, secondBookmark))
            viewModel.loadHistory()

            viewModel.onDeleteBookmark(firstBookmark)

            verify(repository).deleteChat(firstBookmark.chatId)
            assertThat(viewModel.viewState.value.bookmarks).containsExactly(secondBookmark)
        }

    @Test
    fun `given delete fails, when deleting bookmark, then bookmark is restored and error is shown`() = testBlocking {
        val bookmark = createBookmark(chatId = 1L)
        whenever(repository.loadChatHistory()).thenReturn(listOf(bookmark))
        whenever(repository.deleteChat(bookmark.chatId)).thenThrow(RuntimeException("DB unavailable"))
        viewModel.loadHistory()

        val events = viewModel.event.runAndCaptureValues {
            viewModel.onDeleteBookmark(bookmark)
        }

        assertThat(viewModel.viewState.value.bookmarks).containsExactly(bookmark)
        assertThat(events.last()).isEqualTo(ShowSnackbar(R.string.ai_support_chat_history_delete_error))
    }

    private fun createBookmark(chatId: Long = 1234L) = SupportChatBookmark(
        chatId = chatId,
        localSiteId = LocalId(10),
        remoteSiteId = 20L,
        botSlug = AiSupportChatViewModel.DEFAULT_BOT_SLUG,
        sessionId = "session-id",
        hasCreatedTicket = false,
        isResolved = false,
        title = "My orders are fine, all is good",
        createdAt = 1_000L,
        updatedAt = 2_000L
    )
}

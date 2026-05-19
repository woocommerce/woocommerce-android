package com.woocommerce.android.ui.aisupportchat

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
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

    private fun createBookmark() = SupportChatBookmark(
        chatId = 1234L,
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

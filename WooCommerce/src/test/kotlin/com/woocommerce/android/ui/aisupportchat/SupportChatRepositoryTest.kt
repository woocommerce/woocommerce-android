package com.woocommerce.android.ui.aisupportchat

import com.android.volley.VolleyError
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.aisupportchat.networking.SupportChatRestClient
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatResponse
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.persistence.dao.SupportChatBookmarkDao
import org.wordpress.android.fluxc.persistence.entity.SupportChatBookmarkEntity
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date

@ExperimentalCoroutinesApi
class SupportChatRepositoryTest : BaseUnitTest() {
    private val restClient: SupportChatRestClient = mock()
    private val bookmarkDao: SupportChatBookmarkDao = mock()
    private val selectedSite: SelectedSite = mock()
    private val accountStore: AccountStore = mock()
    private val currentTimeProvider: CurrentTimeProvider = mock()

    private lateinit var repository: SupportChatRepository

    @Before
    fun setUp() {
        whenever(currentTimeProvider.currentDate()).thenReturn(Date(CURRENT_TIME))
        repository = SupportChatRepository(
            restClient = restClient,
            bookmarkDao = bookmarkDao,
            selectedSite = selectedSite,
            accountStore = accountStore,
            currentTimeProvider = currentTimeProvider,
            dispatchers = CoroutineDispatchers(
                main = coroutinesTestRule.testDispatcher,
                computation = coroutinesTestRule.testDispatcher,
                io = coroutinesTestRule.testDispatcher
            )
        )
    }

    @Test
    fun `given new chat, when sending message, then rest client sends initial message`() = testBlocking {
        val response = createResponse()
        whenever(restClient.sendMessage(BOT_SLUG, MESSAGE, CONTEXT)).thenReturn(Response.Success(response, emptyList()))

        val result = repository.sendMessage(
            botSlug = BOT_SLUG,
            message = MESSAGE,
            context = CONTEXT
        )

        assertThat(result.getOrNull()).isEqualTo(response)
        verify(restClient).sendMessage(BOT_SLUG, MESSAGE, CONTEXT)
        verify(restClient, never()).sendFollowUpMessage(any(), any(), any())
    }

    @Test
    fun `given existing chat, when sending message, then rest client sends follow up message`() = testBlocking {
        val response = createResponse()
        whenever(restClient.sendFollowUpMessage(BOT_SLUG, CHAT_ID, MESSAGE))
            .thenReturn(Response.Success(response, emptyList()))

        val result = repository.sendMessage(
            botSlug = BOT_SLUG,
            message = MESSAGE,
            context = CONTEXT,
            chatId = CHAT_ID
        )

        assertThat(result.getOrNull()).isEqualTo(response)
        verify(restClient).sendFollowUpMessage(BOT_SLUG, CHAT_ID, MESSAGE)
        verify(restClient, never()).sendMessage(any(), any(), any())
    }

    @Test
    fun `given successful fetch response, when fetching chat, then success result is returned`() = testBlocking {
        val response = createResponse()
        whenever(restClient.fetchChat(BOT_SLUG, CHAT_ID)).thenReturn(Response.Success(response, emptyList()))

        val result = repository.fetchChat(BOT_SLUG, CHAT_ID)

        assertThat(result.getOrNull()).isEqualTo(response)
    }

    @Test
    fun `given failed fetch response, when fetching chat, then failure result is returned`() = testBlocking {
        whenever(restClient.fetchChat(BOT_SLUG, CHAT_ID)).thenReturn(Response.Error(createNetworkError()))

        val result = repository.fetchChat(BOT_SLUG, CHAT_ID)

        val exception = requireNotNull(result.exceptionOrNull()) as SupportChatRepositoryException
        assertThat(exception.message).isEqualTo(ERROR_MESSAGE)
        assertThat(exception.type).isEqualTo(BaseRequest.GenericErrorType.NOT_FOUND.name)
    }

    @Test
    fun `given first message, when registering chat, then bookmark is created with metadata and clamped title`() =
        testBlocking {
            stubSelectedSite()
            stubAccountStore()
            val bookmarkCaptor = argumentCaptor<SupportChatBookmarkEntity>()
            val firstMessage = "  abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ  "

            repository.registerChat(
                chatId = CHAT_ID,
                botSlug = BOT_SLUG,
                firstUserMessage = firstMessage
            )

            verify(bookmarkDao).insertIgnore(bookmarkCaptor.capture())
            assertThat(bookmarkCaptor.firstValue.chatId).isEqualTo(CHAT_ID)
            assertThat(bookmarkCaptor.firstValue.localSiteId).isEqualTo(LocalId(LOCAL_SITE_ID))
            assertThat(bookmarkCaptor.firstValue.remoteSiteId).isEqualTo(REMOTE_SITE_ID)
            assertThat(bookmarkCaptor.firstValue.wpcomUserId).isEqualTo(WPCOM_USER_ID)
            assertThat(bookmarkCaptor.firstValue.botSlug).isEqualTo(BOT_SLUG)
            assertThat(bookmarkCaptor.firstValue.title).isEqualTo("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWX")
            assertThat(bookmarkCaptor.firstValue.createdAt).isEqualTo(CURRENT_TIME)
            assertThat(bookmarkCaptor.firstValue.updatedAt).isEqualTo(CURRENT_TIME)
        }

    @Test
    fun `given blank first message, when registering chat, then title is null`() = testBlocking {
        stubSelectedSite()
        stubAccountStore()
        val bookmarkCaptor = argumentCaptor<SupportChatBookmarkEntity>()

        repository.registerChat(
            chatId = CHAT_ID,
            botSlug = BOT_SLUG,
            firstUserMessage = "   "
        )

        verify(bookmarkDao).insertIgnore(bookmarkCaptor.capture())
        assertThat(bookmarkCaptor.firstValue.title).isNull()
    }

    @Test
    fun `when touching chat, then dao is updated with current timestamp`() = testBlocking {
        repository.touchChat(CHAT_ID)

        verify(bookmarkDao).touch(CHAT_ID, CURRENT_TIME)
    }

    @Test
    fun `when deleting chat, then dao deletes chat id`() = testBlocking {
        repository.deleteChat(CHAT_ID)

        verify(bookmarkDao).delete(CHAT_ID)
    }

    @Test
    fun `when loading chat history, then dao loads bookmarks for selected site`() = testBlocking {
        stubSelectedSite()
        val entity = createBookmarkEntity()
        whenever(bookmarkDao.getForSite(LocalId(LOCAL_SITE_ID))).thenReturn(listOf(entity))

        val bookmarks = repository.loadChatHistory()

        verify(bookmarkDao).getForSite(LocalId(LOCAL_SITE_ID))
        assertThat(bookmarks).containsExactly(
            SupportChatBookmark(
                chatId = entity.chatId,
                localSiteId = entity.localSiteId,
                remoteSiteId = entity.remoteSiteId,
                wpcomUserId = entity.wpcomUserId,
                botSlug = entity.botSlug,
                title = entity.title,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        )
    }

    private fun stubSelectedSite() {
        whenever(selectedSite.get()).thenReturn(
            SiteModel().apply {
                id = LOCAL_SITE_ID
                siteId = REMOTE_SITE_ID
            }
        )
    }

    private fun stubAccountStore(userId: Long = WPCOM_USER_ID) {
        whenever(accountStore.account).thenReturn(
            AccountModel().apply {
                this.userId = userId
            }
        )
    }

    private fun createResponse(): SupportChatResponse = SupportChatResponse(
        chatId = CHAT_ID,
        sessionId = null,
        botSlug = BOT_SLUG,
        botVersion = null
    )

    private fun createBookmarkEntity(): SupportChatBookmarkEntity = SupportChatBookmarkEntity(
        chatId = CHAT_ID,
        localSiteId = LocalId(LOCAL_SITE_ID),
        remoteSiteId = REMOTE_SITE_ID,
        wpcomUserId = WPCOM_USER_ID,
        botSlug = BOT_SLUG,
        title = "Support chat",
        createdAt = 1_000L,
        updatedAt = 2_000L
    )

    private fun createNetworkError(): WPComGsonNetworkError = WPComGsonNetworkError(
        BaseNetworkError(
            BaseRequest.GenericErrorType.NOT_FOUND,
            ERROR_MESSAGE,
            VolleyError()
        )
    )

    private companion object {
        const val BOT_SLUG = "woo-workflow-support_mobile_inapp"
        const val CHAT_ID = 1234L
        const val LOCAL_SITE_ID = 10
        const val REMOTE_SITE_ID = 20L
        const val WPCOM_USER_ID = 30L
        const val CURRENT_TIME = 1_234_567L
        const val MESSAGE = "I need help with orders"
        const val ERROR_MESSAGE = "Not found"
        val CONTEXT = mapOf<String, Any>("site_id" to REMOTE_SITE_ID)
    }
}

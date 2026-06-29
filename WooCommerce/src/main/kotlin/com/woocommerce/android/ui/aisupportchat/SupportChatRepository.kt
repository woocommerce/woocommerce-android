package com.woocommerce.android.ui.aisupportchat

import com.google.gson.JsonObject
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.aisupportchat.networking.SupportChatRestClient
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatResponse
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.Reusable
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.persistence.dao.SupportChatBookmarkDao
import org.wordpress.android.fluxc.persistence.entity.SupportChatBookmarkEntity
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import javax.inject.Inject

@Reusable
class SupportChatRepository @Inject constructor(
    private val restClient: SupportChatRestClient,
    private val bookmarkDao: SupportChatBookmarkDao,
    private val selectedSite: SelectedSite,
    private val currentTimeProvider: CurrentTimeProvider,
    private val dispatchers: CoroutineDispatchers
) {
    suspend fun sendMessage(
        botSlug: String,
        message: String,
        context: JsonObject,
        chatId: Long? = null,
        sessionId: String? = null
    ): Result<SupportChatResponse> = withContext(dispatchers.io) {
        val response = if (chatId == null) {
            restClient.sendMessage(botSlug = botSlug, message = message, context = context)
        } else {
            restClient.sendFollowUpMessage(
                botSlug = botSlug,
                chatId = chatId,
                sessionId = sessionId,
                message = message
            )
        }
        response.toResult()
    }

    suspend fun fetchChat(
        botSlug: String,
        chatId: Long,
        sessionId: String?
    ): Result<SupportChatResponse> = withContext(dispatchers.io) {
        restClient.fetchChat(botSlug = botSlug, chatId = chatId, sessionId = sessionId).toResult()
    }

    suspend fun submitFeedback(
        botSlug: String,
        chatId: Long,
        messageId: Long,
        sessionId: String,
        upvoted: Boolean
    ): Result<Unit> = withContext(dispatchers.io) {
        restClient.submitFeedback(
            botSlug = botSlug,
            chatId = chatId,
            messageId = messageId,
            sessionId = sessionId,
            upvoted = upvoted
        ).toResult()
    }

    suspend fun registerChat(
        chatId: Long,
        botSlug: String,
        sessionId: String?,
        firstUserMessage: String,
        extraTags: List<String> = emptyList()
    ): Unit = withContext(dispatchers.io) {
        val selectedSiteModel = selectedSite.get()
        val now = currentTimeProvider.currentDate().time
        bookmarkDao.insertOrReplace(
            SupportChatBookmarkEntity(
                chatId = chatId,
                localSiteId = LocalId(selectedSiteModel.id),
                remoteSiteId = selectedSiteModel.siteId,
                botSlug = botSlug,
                sessionId = sessionId,
                hasCreatedTicket = false,
                isResolved = false,
                extraTags = extraTags,
                title = firstUserMessage.trim().take(MAX_TITLE_LENGTH).ifBlank { null },
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun markChatAsUpdated(chatId: Long, sessionId: String?): Unit = withContext(dispatchers.io) {
        bookmarkDao.markAsUpdated(
            chatId = chatId,
            sessionId = sessionId,
            updatedAt = currentTimeProvider.currentDate().time
        )
    }

    suspend fun loadChatHistory(): List<SupportChatBookmark> = withContext(dispatchers.io) {
        bookmarkDao.getForSite(LocalId(selectedSite.get().id)).map { it.toSupportChatBookmark() }
    }

    suspend fun markChatAsTicketCreated(chatId: Long): Unit = withContext(dispatchers.io) {
        bookmarkDao.markTicketCreated(chatId)
    }

    suspend fun markChatAsResolved(chatId: Long): Unit = withContext(dispatchers.io) {
        bookmarkDao.markResolved(chatId)
    }

    suspend fun deleteChat(chatId: Long): Unit = withContext(dispatchers.io) {
        bookmarkDao.delete(chatId)
    }

    private fun <T> Response<T>.toResult(): Result<T> =
        when (this) {
            is Response.Success -> Result.success(data)
            is Response.Error -> Result.failure(
                SupportChatRepositoryException(
                    message = error.message,
                    type = error.type.name
                )
            )
        }

    private fun SupportChatBookmarkEntity.toSupportChatBookmark(): SupportChatBookmark =
        SupportChatBookmark(
            chatId = chatId,
            localSiteId = localSiteId,
            remoteSiteId = remoteSiteId,
            botSlug = botSlug,
            sessionId = sessionId,
            hasCreatedTicket = hasCreatedTicket,
            isResolved = isResolved,
            extraTags = extraTags,
            title = title,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    private companion object {
        const val MAX_TITLE_LENGTH = 50
    }
}

data class SupportChatBookmark(
    val chatId: Long,
    val localSiteId: LocalId,
    val remoteSiteId: Long,
    val botSlug: String,
    val sessionId: String?,
    val hasCreatedTicket: Boolean,
    val isResolved: Boolean,
    val extraTags: List<String> = emptyList(),
    val title: String?,
    val createdAt: Long,
    val updatedAt: Long
)

class SupportChatRepositoryException(
    message: String?,
    val type: String
) : Exception(message)

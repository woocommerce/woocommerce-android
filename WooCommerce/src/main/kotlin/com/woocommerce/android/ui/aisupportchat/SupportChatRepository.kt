package com.woocommerce.android.ui.aisupportchat

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
        context: Map<String, Any>,
        chatId: Long? = null
    ): Result<SupportChatResponse> = withContext(dispatchers.io) {
        val response = if (chatId == null) {
            restClient.sendMessage(botSlug = botSlug, message = message, context = context)
        } else {
            restClient.sendFollowUpMessage(botSlug = botSlug, chatId = chatId, message = message)
        }
        response.toResult()
    }

    suspend fun fetchChat(botSlug: String, chatId: Long): Result<SupportChatResponse> = withContext(dispatchers.io) {
        restClient.fetchChat(botSlug = botSlug, chatId = chatId).toResult()
    }

    suspend fun registerChat(chatId: Long, botSlug: String, firstUserMessage: String): Unit = withContext(dispatchers.io) {
        val selectedSiteModel = selectedSite.get()
        val now = currentTimeProvider.currentDate().time
        bookmarkDao.insertOrReplace(
            SupportChatBookmarkEntity(
                chatId = chatId,
                localSiteId = LocalId(selectedSiteModel.id),
                remoteSiteId = selectedSiteModel.siteId,
                botSlug = botSlug,
                title = firstUserMessage.trim().take(MAX_TITLE_LENGTH).ifBlank { null },
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun markChatAsUpdated(chatId: Long): Unit = withContext(dispatchers.io) {
        bookmarkDao.markAsUpdated(chatId = chatId, updatedAt = currentTimeProvider.currentDate().time)
    }

    suspend fun loadChatHistory(): List<SupportChatBookmark> = withContext(dispatchers.io) {
        bookmarkDao.getForSite(LocalId(selectedSite.get().id)).map { it.toSupportChatBookmark() }
    }

    suspend fun deleteChat(chatId: Long): Unit = withContext(dispatchers.io) {
        bookmarkDao.delete(chatId)
    }

    private fun Response<SupportChatResponse>.toResult(): Result<SupportChatResponse> =
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
    val title: String?,
    val createdAt: Long,
    val updatedAt: Long
)

class SupportChatRepositoryException(
    message: String?,
    val type: String
) : Exception(message)

package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.entity.SupportChatBookmarkEntity

@RunWith(RobolectricTestRunner::class)
class SupportChatBookmarkDaoTest {
    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext<Application>())

    private lateinit var dao: SupportChatBookmarkDao

    @Before
    fun setUp() {
        dao = databaseRule.db.supportChatBookmarkDao
    }

    @Test
    fun `when bookmark is inserted, then it can be loaded by chat id`(): Unit = runBlocking {
        val bookmark = createBookmark(chatId = 1L)

        dao.insertOrReplace(bookmark)

        assertThat(dao.getByChatId(1L)).isEqualTo(bookmark)
    }

    @Test
    fun `given duplicate chat id, when inserted, then existing bookmark is replaced`(): Unit = runBlocking {
        val first = createBookmark(chatId = 1L, title = "First")
        val duplicate = createBookmark(chatId = 1L, title = "Duplicate")

        dao.insertOrReplace(first)
        dao.insertOrReplace(duplicate)

        assertThat(dao.getByChatId(1L)).isEqualTo(duplicate)
    }

    @Test
    fun `when loading bookmarks for site, then they are sorted by updated date descending`(): Unit = runBlocking {
        val oldest = createBookmark(chatId = 1L, updatedAt = 100L)
        val newest = createBookmark(chatId = 2L, updatedAt = 300L)
        val middle = createBookmark(chatId = 3L, updatedAt = 200L)
        listOf(oldest, newest, middle).forEach { dao.insertOrReplace(it) }

        val bookmarks = dao.getForSite(DEFAULT_SITE_ID)

        assertThat(bookmarks).containsExactly(newest, middle, oldest)
    }

    @Test
    fun `when loading bookmarks for site, then other sites are excluded`(): Unit = runBlocking {
        val selectedSiteBookmark = createBookmark(chatId = 1L, localSiteId = DEFAULT_SITE_ID)
        val otherSiteBookmark = createBookmark(chatId = 2L, localSiteId = OTHER_SITE_ID)
        dao.insertOrReplace(selectedSiteBookmark)
        dao.insertOrReplace(otherSiteBookmark)

        val bookmarks = dao.getForSite(DEFAULT_SITE_ID)

        assertThat(bookmarks).containsExactly(selectedSiteBookmark)
    }

    @Test
    fun `when touching bookmark, then only updated date changes`(): Unit = runBlocking {
        val bookmark = createBookmark(chatId = 1L, updatedAt = 100L)
        dao.insertOrReplace(bookmark)

        val touchedRows = dao.touch(chatId = 1L, updatedAt = 200L)

        val touchedBookmark = requireNotNull(dao.getByChatId(1L))
        assertThat(touchedRows).isEqualTo(1)
        assertThat(touchedBookmark).isEqualTo(bookmark.copy(updatedAt = 200L))
    }

    @Test
    fun `given missing bookmark, when touched, then no rows are changed`(): Unit = runBlocking {
        val touchedRows = dao.touch(chatId = 1L, updatedAt = 200L)

        assertThat(touchedRows).isEqualTo(0)
    }

    @Test
    fun `when bookmark is deleted, then only target row is removed`(): Unit = runBlocking {
        val targetBookmark = createBookmark(chatId = 1L)
        val otherBookmark = createBookmark(chatId = 2L)
        dao.insertOrReplace(targetBookmark)
        dao.insertOrReplace(otherBookmark)

        val deletedRows = dao.delete(chatId = 1L)

        assertThat(deletedRows).isEqualTo(1)
        assertThat(dao.getByChatId(1L)).isNull()
        assertThat(dao.getByChatId(2L)).isEqualTo(otherBookmark)
    }

    @Test
    fun `given missing bookmark, when deleted, then no rows are changed`(): Unit = runBlocking {
        val deletedRows = dao.delete(chatId = 1L)

        assertThat(deletedRows).isEqualTo(0)
    }

    private fun createBookmark(
        chatId: Long,
        localSiteId: LocalId = DEFAULT_SITE_ID,
        remoteSiteId: Long = 100L,
        wpcomUserId: Long = 200L,
        botSlug: String = "woo-workflow-support_mobile_inapp",
        title: String? = "Support chat",
        createdAt: Long = 1_000L,
        updatedAt: Long = 1_000L
    ): SupportChatBookmarkEntity = SupportChatBookmarkEntity(
        chatId = chatId,
        localSiteId = localSiteId,
        remoteSiteId = remoteSiteId,
        wpcomUserId = wpcomUserId,
        botSlug = botSlug,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private companion object {
        val DEFAULT_SITE_ID = LocalId(1)
        val OTHER_SITE_ID = LocalId(2)
    }
}

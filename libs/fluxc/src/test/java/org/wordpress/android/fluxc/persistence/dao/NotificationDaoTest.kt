package org.wordpress.android.fluxc.persistence.dao

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule
import org.wordpress.android.fluxc.persistence.entity.NotificationEntity

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class NotificationDaoTest {
    private lateinit var dao: NotificationDao

    @Rule
    @JvmField
    val databaseRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        dao = databaseRule.db.notificationDao()
    }

    // region insert
    @Test
    fun `given new notification, when insert, then notification is inserted`() = runTest {
        val notification = notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1)

        dao.insert(notification)

        val result = dao.getNotificationByRemoteId(REMOTE_NOTE_ID_1)
        assertThat(result?.remoteNoteId).isEqualTo(REMOTE_NOTE_ID_1)
    }

    @Test
    fun `given existing notification, when insert, then notification is replaced`() = runTest {
        val notification = notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1, title = "Original")
        dao.insert(notification)

        val updated = notification.copy(title = "Updated")
        dao.insert(updated)

        val result = dao.getNotificationByRemoteId(REMOTE_NOTE_ID_1)
        assertThat(result?.title).isEqualTo("Updated")
    }

    @Test
    fun `given existing notification, when insert with same keys, then replaces`() = runTest {
        val notification = notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1)
        dao.insert(notification)
        val existing = dao.getNotificationByRemoteId(REMOTE_NOTE_ID_1)!!

        val updated = existing.copy(title = "Updated Title")
        dao.insert(updated)

        val count = dao.getNotificationsCount()
        assertThat(count).isEqualTo(1)
        assertThat(dao.getNotificationByRemoteId(REMOTE_NOTE_ID_1)?.title).isEqualTo("Updated Title")
    }
    // endregion

    // region insertAll
    @Test
    fun `given empty list, when insert all, then no notifications are inserted`() = runTest {
        val emptyList = emptyList<NotificationEntity>()

        dao.insertAll(emptyList)

        assertThat(dao.getNotificationsCount()).isEqualTo(0)
    }

    @Test
    fun `given list of notifications, when insert all, then all are inserted`() = runTest {
        val notifications = listOf(
            notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1),
            notification(remoteNoteId = REMOTE_NOTE_ID_2, remoteSiteId = SITE_ID_1),
            notification(remoteNoteId = REMOTE_NOTE_ID_3, remoteSiteId = SITE_ID_2)
        )

        dao.insertAll(notifications)

        assertThat(dao.getNotificationsCount()).isEqualTo(3)
    }

    @Test
    fun `given existing notifications, when insert all with same keys, then replaces all`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1, title = "Original 1"))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_2, remoteSiteId = SITE_ID_1, title = "Original 2"))
        val updatedNotifications = listOf(
            notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1, title = "Updated 1"),
            notification(remoteNoteId = REMOTE_NOTE_ID_2, remoteSiteId = SITE_ID_1, title = "Updated 2")
        )

        dao.insertAll(updatedNotifications)

        assertThat(dao.getNotificationsCount()).isEqualTo(2)
        assertThat(dao.getNotificationByRemoteId(REMOTE_NOTE_ID_1)?.title).isEqualTo("Updated 1")
        assertThat(dao.getNotificationByRemoteId(REMOTE_NOTE_ID_2)?.title).isEqualTo("Updated 2")
    }

    @Test
    fun `given mixed new and existing, when insert all, then inserts new and replaces existing`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1, title = "Original"))
        val notifications = listOf(
            notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1, title = "Updated"),
            notification(remoteNoteId = REMOTE_NOTE_ID_2, remoteSiteId = SITE_ID_1, title = "New")
        )

        dao.insertAll(notifications)

        assertThat(dao.getNotificationsCount()).isEqualTo(2)
        assertThat(dao.getNotificationByRemoteId(REMOTE_NOTE_ID_1)?.title).isEqualTo("Updated")
        assertThat(dao.getNotificationByRemoteId(REMOTE_NOTE_ID_2)?.title).isEqualTo("New")
    }
    // endregion

    // region getNotificationsCount
    @Test
    fun `given no notifications, when get count, then returns zero`() = runTest {
        val count = dao.getNotificationsCount()

        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `given notifications exist, when get count, then returns correct count`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_2, remoteSiteId = SITE_ID_1))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_3, remoteSiteId = SITE_ID_2))

        val count = dao.getNotificationsCount()

        assertThat(count).isEqualTo(3)
    }
    // endregion

    // region getAllNotifications
    @Test
    fun `given notifications exist, when get all, then returns all ordered by timestamp desc`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1, timestamp = "2024-01-01"))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_2, remoteSiteId = SITE_ID_1, timestamp = "2024-01-03"))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_3, remoteSiteId = SITE_ID_2, timestamp = "2024-01-02"))

        val result = dao.getAllNotifications()

        assertThat(result).hasSize(3)
        assertThat(result[0].remoteNoteId).isEqualTo(REMOTE_NOTE_ID_2)
        assertThat(result[1].remoteNoteId).isEqualTo(REMOTE_NOTE_ID_3)
        assertThat(result[2].remoteNoteId).isEqualTo(REMOTE_NOTE_ID_1)
    }
    // endregion

    // region getNotificationsForSite
    @Test
    fun `given notifications for multiple sites, when get for site, then returns only site notifications`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_2, remoteSiteId = SITE_ID_1))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_3, remoteSiteId = SITE_ID_2))

        val result = dao.getNotificationsForSite(SITE_ID_1, null, null)

        assertThat(result).hasSize(2)
        assertThat(result.all { it.remoteSiteId == SITE_ID_1 }).isTrue()
    }

    @Test
    fun `given notifications, when get for site with type filter, then returns filtered`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1, type = "store_order"))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_2, remoteSiteId = SITE_ID_1, type = "comment"))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_3, remoteSiteId = SITE_ID_1, type = "store_order"))

        val result = dao.getNotificationsForSite(SITE_ID_1, listOf("store_order"), null)

        assertThat(result).hasSize(2)
        assertThat(result.all { it.type == "store_order" }).isTrue()
    }

    @Test
    fun `given notifications, when get for site with subtype filter, then returns filtered`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1, subtype = "store_review"))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_2, remoteSiteId = SITE_ID_1, subtype = null))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_3, remoteSiteId = SITE_ID_1, subtype = "store_review"))

        val result = dao.getNotificationsForSite(SITE_ID_1, null, listOf("store_review"))

        assertThat(result).hasSize(2)
    }

    @Test
    fun `given notifications, when get for site ordered, then returns by timestamp desc`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1, timestamp = "2024-01-01"))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_2, remoteSiteId = SITE_ID_1, timestamp = "2024-01-03"))

        val result = dao.getNotificationsForSite(SITE_ID_1, null, null)

        assertThat(result[0].timestamp).isEqualTo("2024-01-03")
        assertThat(result[1].timestamp).isEqualTo("2024-01-01")
    }
    // endregion

    // region observeNotificationsForSite
    @Test
    fun `given notifications, when observe for site, then emits site notifications`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_2, remoteSiteId = SITE_ID_2))

        val result = dao.observeNotificationsForSite(SITE_ID_1, null, null).first()

        assertThat(result).hasSize(1)
        assertThat(result[0].remoteSiteId).isEqualTo(SITE_ID_1)
    }
    // endregion

    // region hasUnreadNotificationsForSite
    @Test
    fun `given unread notifications, when has unread for site, then returns true`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1, read = false))

        val result = dao.hasUnreadNotificationsForSite(SITE_ID_1, null, null)

        assertThat(result).isTrue()
    }

    @Test
    fun `given all read notifications, when has unread for site, then returns false`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1, read = true))

        val result = dao.hasUnreadNotificationsForSite(SITE_ID_1, null, null)

        assertThat(result).isFalse()
    }

    @Test
    fun `given no notifications, when has unread for site, then returns false`() = runTest {
        val result = dao.hasUnreadNotificationsForSite(SITE_ID_1, null, null)

        assertThat(result).isFalse()
    }

    @Test
    fun `given unread for different site, when has unread for site, then returns false`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_2, read = false))

        val result = dao.hasUnreadNotificationsForSite(SITE_ID_1, null, null)

        assertThat(result).isFalse()
    }

    @Test
    fun `given unread with type filter, when has unread for site, then filters correctly`() = runTest {
        dao.insert(
            notification(
                remoteNoteId = REMOTE_NOTE_ID_1,
                remoteSiteId = SITE_ID_1,
                read = false,
                type = "store_order"
            )
        )
        dao.insert(
            notification(
                remoteNoteId = REMOTE_NOTE_ID_2,
                remoteSiteId = SITE_ID_1,
                read = false,
                type = "comment"
            )
        )

        val result = dao.hasUnreadNotificationsForSite(SITE_ID_1, listOf("store_order"), null)

        assertThat(result).isTrue()
    }
    // endregion

    // region getNotificationByRemoteId
    @Test
    fun `given notification exists, when get by remote id, then returns notification`() = runTest {
        dao.insert(notification(remoteNoteId = RemoteId(123L), remoteSiteId = SITE_ID_1))

        val result = dao.getNotificationByRemoteId(RemoteId(123L))

        assertThat(result).isNotNull
        assertThat(result?.remoteNoteId).isEqualTo(RemoteId(123L))
    }

    @Test
    fun `given notification not exists, when get by remote id, then returns null`() = runTest {
        val result = dao.getNotificationByRemoteId(RemoteId(999L))

        assertThat(result).isNull()
    }
    // endregion

    // region deleteAllByRemoteIds
    @Test
    fun `given empty list, when delete all by remote ids, then no notifications are deleted`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_2, remoteSiteId = SITE_ID_1))

        dao.deleteAllByRemoteIds(emptyList())

        assertThat(dao.getNotificationsCount()).isEqualTo(2)
    }

    @Test
    fun `given notifications exist, when delete all by remote ids, then deletes matching notifications`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_2, remoteSiteId = SITE_ID_1))
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_3, remoteSiteId = SITE_ID_2))

        dao.deleteAllByRemoteIds(listOf(REMOTE_NOTE_ID_1, REMOTE_NOTE_ID_3))

        assertThat(dao.getNotificationsCount()).isEqualTo(1)
        assertThat(dao.getNotificationByRemoteId(REMOTE_NOTE_ID_1)).isNull()
        assertThat(dao.getNotificationByRemoteId(REMOTE_NOTE_ID_2)).isNotNull
        assertThat(dao.getNotificationByRemoteId(REMOTE_NOTE_ID_3)).isNull()
    }

    @Test
    fun `given some ids not exist, when delete all by remote ids, then deletes only existing`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1))

        dao.deleteAllByRemoteIds(listOf(REMOTE_NOTE_ID_1, RemoteId(999L)))

        assertThat(dao.getNotificationsCount()).isEqualTo(0)
    }

    @Test
    fun `given all ids not exist, when delete all by remote ids, then no change`() = runTest {
        dao.insert(notification(remoteNoteId = REMOTE_NOTE_ID_1, remoteSiteId = SITE_ID_1))

        dao.deleteAllByRemoteIds(listOf(RemoteId(888L), RemoteId(999L)))

        assertThat(dao.getNotificationsCount()).isEqualTo(1)
    }

    @Test
    fun `given more ids than sqlite variable limit, when delete all by remote ids, then deletes all matching`() =
        runTest {
            val idsToDelete = (1L..1500L).map { RemoteId(it) }
            dao.insertAll(idsToDelete.map { notification(remoteNoteId = it, remoteSiteId = SITE_ID_1) })
            val keptId = RemoteId(9999L)
            dao.insert(notification(remoteNoteId = keptId, remoteSiteId = SITE_ID_1))

            dao.deleteAllByRemoteIds(idsToDelete)

            assertThat(dao.getNotificationsCount()).isEqualTo(1)
            assertThat(dao.getNotificationByRemoteId(keptId)).isNotNull
        }
    // endregion

    /* HELPER */

    @Suppress("LongParameterList")
    private fun notification(
        remoteNoteId: RemoteId,
        remoteSiteId: RemoteId,
        noteHash: Long = remoteNoteId.value * 100,
        type: String = "store_order",
        subtype: String? = null,
        read: Boolean = false,
        timestamp: String = "2024-01-01T00:00:00",
        title: String = "Test Notification"
    ) = NotificationEntity(
        remoteSiteId = remoteSiteId,
        remoteNoteId = remoteNoteId,
        noteHash = noteHash,
        type = type,
        subtype = subtype,
        read = read,
        icon = null,
        noticon = null,
        timestamp = timestamp,
        url = null,
        title = title,
        formattableBody = null,
        formattableSubject = null,
        formattableMeta = null
    )

    companion object {
        private val SITE_ID_1 = RemoteId(100L)
        private val SITE_ID_2 = RemoteId(200L)

        private val REMOTE_NOTE_ID_1 = RemoteId(1L)
        private val REMOTE_NOTE_ID_2 = RemoteId(2L)
        private val REMOTE_NOTE_ID_3 = RemoteId(3L)
    }
}

package com.woocommerce.android.model

import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.WooNotificationType
import com.woocommerce.android.notifications.push.NotificationTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NotificationTest {
    private val remoteSiteId = 123445L
    private val orderNotification = NotificationTestUtils.generateTestNotification(
        remoteNoteId = 1L,
        remoteSiteId = remoteSiteId,
        uniqueId = 0L,
        channelType = NotificationChannelType.NEW_ORDER,
        noteType = WooNotificationType.NEW_ORDER
    )

    private val reviewNotification = NotificationTestUtils.generateTestNotification(
        remoteNoteId = 1L,
        remoteSiteId = remoteSiteId,
        uniqueId = 0L,
        channelType = NotificationChannelType.REVIEW,
        noteType = WooNotificationType.PRODUCT_REVIEW
    )

    private val otherNotification = NotificationTestUtils.generateTestNotification(
        remoteNoteId = 1L,
        remoteSiteId = remoteSiteId,
        uniqueId = 0L,
        channelType = NotificationChannelType.OTHER,
        noteType = WooNotificationType.LOCAL_REMINDER
    )

    @Test
    fun `new order notification return correct group_id and group_push_id`() {
        val expectedOrderGroupId = "${NotificationChannelType.NEW_ORDER.name} $remoteSiteId"
        val expectedOrderGroupPushId = 30001 + remoteSiteId

        val actualGroupId = orderNotification.getGroup()
        assertThat(actualGroupId).isEqualTo(expectedOrderGroupId)

        val actualGroupPushId = orderNotification.getGroupPushId()
        assertThat(actualGroupPushId).isEqualTo(expectedOrderGroupPushId)
    }

    @Test
    fun `new review notification return correct group_id and group_push_id`() {
        val expectedGroupId = "${NotificationChannelType.REVIEW.name} $remoteSiteId"
        val expectedGroupPushId = 30002 + remoteSiteId

        val actualGroupId = reviewNotification.getGroup()
        assertThat(actualGroupId).isEqualTo(expectedGroupId)

        val actualGroupPushId = reviewNotification.getGroupPushId()
        assertThat(actualGroupPushId).isEqualTo(expectedGroupPushId)
    }

    @Test
    fun `new zendesk notification return correct group_id and group_push_id`() {
        val expectedGroupId = "${NotificationChannelType.OTHER.name} $remoteSiteId"
        val expectedGroupPushId = 30003 + remoteSiteId

        val actualGroupId = otherNotification.getGroup()
        assertThat(actualGroupId).isEqualTo(expectedGroupId)

        val actualGroupPushId = otherNotification.getGroupPushId()
        assertThat(actualGroupPushId).isEqualTo(expectedGroupPushId)
    }

    @Test
    fun `new notification for different site returns correct group_id and group_push_id`() {
        val siteId2 = 33456L
        val orderNotification2 = orderNotification.copy(remoteSiteId = siteId2)
        val reviewNotification2 = reviewNotification.copy(remoteSiteId = siteId2)
        val otherNotification2 = otherNotification.copy(remoteSiteId = siteId2)

        val expectedOrderGroupId = "${NotificationChannelType.NEW_ORDER.name} $siteId2"
        val expectedOrderGroupPushId = 30001 + siteId2
        val expectedReviewGroupId = "${NotificationChannelType.REVIEW.name} $siteId2"
        val expectedReviewGroupPushId = 30002 + siteId2
        val expectedZendeskGroupId = "${NotificationChannelType.OTHER.name} $siteId2"
        val expectedZendeskGroupPushId = 30003 + siteId2

        assertThat(orderNotification2.getGroup()).isEqualTo(expectedOrderGroupId)
        assertThat(orderNotification2.getGroupPushId()).isEqualTo(expectedOrderGroupPushId)
        assertThat(reviewNotification2.getGroup()).isEqualTo(expectedReviewGroupId)
        assertThat(reviewNotification2.getGroupPushId()).isEqualTo(expectedReviewGroupPushId)
        assertThat(otherNotification2.getGroup()).isEqualTo(expectedZendeskGroupId)
        assertThat(otherNotification2.getGroupPushId()).isEqualTo(expectedZendeskGroupPushId)
    }
}

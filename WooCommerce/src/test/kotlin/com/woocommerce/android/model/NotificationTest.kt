package com.woocommerce.android.model

import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.push.NotificationTestUtils
import com.woocommerce.android.push.WooNotificationType
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
        noteType = WooNotificationType.ZENDESK
    )

    @Test
    fun `new order notification return correct group_id and group_push_id`() {
        val expectedOrderGroupId = "wooandroid_notification_channel_order_id $remoteSiteId"
        val expectedOrderGroupPushId = 30001 + remoteSiteId

        val channelId = "wooandroid_notification_channel_order_id"
        val actualGroupId = orderNotification.getGroup(channelId)
        assertThat(actualGroupId).isEqualTo(expectedOrderGroupId)

        val actualGroupPushId = orderNotification.getGroupPushId()
        assertThat(actualGroupPushId).isEqualTo(expectedOrderGroupPushId)
    }

    @Test
    fun `new review notification return correct group_id and group_push_id`() {
        val expectedGroupId = "wooandroid_notification_channel_review_id $remoteSiteId"
        val expectedGroupPushId = 30002 + remoteSiteId

        val channelId = "wooandroid_notification_channel_review_id"
        val actualGroupId = reviewNotification.getGroup(channelId)
        assertThat(actualGroupId).isEqualTo(expectedGroupId)

        val actualGroupPushId = reviewNotification.getGroupPushId()
        assertThat(actualGroupPushId).isEqualTo(expectedGroupPushId)
    }

    @Test
    fun `new zendesk notification return correct group_id and group_push_id`() {
        val expectedGroupId = "wooandroid_notification_channel_other_id $remoteSiteId"
        val expectedGroupPushId = 30003 + remoteSiteId

        val channelId = "wooandroid_notification_channel_other_id"
        val actualGroupId = otherNotification.getGroup(channelId)
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

        val expectedOrderGroupId = "wooandroid_notification_channel_order_id $siteId2"
        val expectedOrderGroupPushId = 30001 + siteId2
        val expectedReviewGroupId = "wooandroid_notification_channel_review_id $siteId2"
        val expectedReviewGroupPushId = 30002 + siteId2
        val expectedZendeskGroupId = "wooandroid_notification_channel_other_id $siteId2"
        val expectedZendeskGroupPushId = 30003 + siteId2

        val orderChannelId = "wooandroid_notification_channel_order_id"
        val reviewChannelId = "wooandroid_notification_channel_review_id"
        val zendeskChannelId = "wooandroid_notification_channel_other_id"
        assertThat(orderNotification2.getGroup(orderChannelId)).isEqualTo(expectedOrderGroupId)
        assertThat(orderNotification2.getGroupPushId()).isEqualTo(expectedOrderGroupPushId)
        assertThat(reviewNotification2.getGroup(reviewChannelId)).isEqualTo(expectedReviewGroupId)
        assertThat(reviewNotification2.getGroupPushId()).isEqualTo(expectedReviewGroupPushId)
        assertThat(otherNotification2.getGroup(zendeskChannelId)).isEqualTo(expectedZendeskGroupId)
        assertThat(otherNotification2.getGroupPushId()).isEqualTo(expectedZendeskGroupPushId)
    }
}

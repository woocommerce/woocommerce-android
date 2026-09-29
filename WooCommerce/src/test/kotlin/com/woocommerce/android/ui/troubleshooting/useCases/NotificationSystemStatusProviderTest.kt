package com.woocommerce.android.ui.troubleshooting.useCases

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.WooNotificationBuilder
import com.woocommerce.android.ui.troubleshooting.useCases.NotificationSystemStatusProvider.ChannelImportance
import com.woocommerce.android.ui.troubleshooting.useCases.NotificationSystemStatusProvider.DoNotDisturbStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NotificationSystemStatusProviderTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val wooNotificationBuilder: WooNotificationBuilder = mock()
    private val notificationChannelsHandler: NotificationChannelsHandler = mock()

    private val sut = NotificationSystemStatusProvider(
        context = context,
        wooNotificationBuilder = wooNotificationBuilder,
        notificationChannelsHandler = notificationChannelsHandler
    )

    private val channelIds = NotificationChannelType.entries.associateWith { it.name }.toMutableMap()
    private var createdChannelCount = 0

    init {
        with(notificationChannelsHandler) {
            NotificationChannelType.entries.forEach { channelType ->
                whenever(channelType.getChannelId()).thenAnswer { channelIds.getValue(channelType) }
            }
        }
    }

    @Test
    fun `given a channel at each importance, when channels are read, then each importance is named`() {
        val expected = mapOf(
            NotificationManager.IMPORTANCE_NONE to ChannelImportance.OFF,
            NotificationManager.IMPORTANCE_MIN to ChannelImportance.SILENT,
            NotificationManager.IMPORTANCE_LOW to ChannelImportance.SILENT,
            NotificationManager.IMPORTANCE_DEFAULT to ChannelImportance.DEFAULT,
            NotificationManager.IMPORTANCE_HIGH to ChannelImportance.HIGH,
            NotificationManager.IMPORTANCE_MAX to ChannelImportance.HIGH,
            NotificationManager.IMPORTANCE_UNSPECIFIED to ChannelImportance.UNKNOWN
        )

        expected.forEach { (importance, channelImportance) ->
            createChannel(NotificationChannelType.NEW_ORDER, importance)

            assertThat(sut.wooNotificationChannels()[NotificationChannelType.NEW_ORDER]?.importance)
                .isEqualTo(channelImportance)
        }
    }

    @Test
    fun `given a channel the merchant silenced, when channels are read, then it is not reported as disabled`() {
        createChannel(NotificationChannelType.NEW_ORDER, NotificationManager.IMPORTANCE_LOW)

        assertThat(sut.wooNotificationChannels()[NotificationChannelType.NEW_ORDER]?.importance)
            .isEqualTo(ChannelImportance.SILENT)
        assertThat(sut.disabledWooNotificationChannels()).doesNotContain(NotificationChannelType.NEW_ORDER)
    }

    @Test
    fun `given a channel that was never created, when channels are read, then it is reported as not created`() {
        assertThat(sut.wooNotificationChannels()[NotificationChannelType.REVIEW]?.importance)
            .isEqualTo(ChannelImportance.NOT_CREATED)
    }

    @Test
    fun `given a channel turned off, when disabled channels are read, then it is listed`() {
        createChannel(NotificationChannelType.STOCK, NotificationManager.IMPORTANCE_NONE)
        createChannel(NotificationChannelType.OTHER, NotificationManager.IMPORTANCE_DEFAULT)

        assertThat(sut.disabledWooNotificationChannels()).containsExactly(NotificationChannelType.STOCK)
    }

    @Test
    fun `given a channel exempted from do not disturb, when channels are read, then the bypass is reported`() {
        createChannel(NotificationChannelType.NEW_ORDER, NotificationManager.IMPORTANCE_DEFAULT, bypassDnd = true)
        createChannel(NotificationChannelType.REVIEW, NotificationManager.IMPORTANCE_DEFAULT)

        val channels = sut.wooNotificationChannels()

        assertThat(channels[NotificationChannelType.NEW_ORDER]?.canBypassDnd).isTrue()
        assertThat(channels[NotificationChannelType.REVIEW]?.canBypassDnd).isFalse()
    }

    @Test
    fun `given each interruption filter, when do not disturb is read, then the matching status is returned`() {
        val expected = mapOf(
            NotificationManager.INTERRUPTION_FILTER_ALL to DoNotDisturbStatus.OFF,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY to DoNotDisturbStatus.PRIORITY_ONLY,
            NotificationManager.INTERRUPTION_FILTER_ALARMS to DoNotDisturbStatus.ALARMS_ONLY,
            NotificationManager.INTERRUPTION_FILTER_NONE to DoNotDisturbStatus.TOTAL_SILENCE,
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN to DoNotDisturbStatus.UNKNOWN
        )

        shadowOf(notificationManager).setNotificationPolicyAccessGranted(true)

        expected.forEach { (filter, status) ->
            notificationManager.setInterruptionFilter(filter)

            assertThat(sut.doNotDisturbStatus()).isEqualTo(status)
        }
    }

    /**
     * A channel that already exists keeps the importance it has, and so does a deleted one that is recreated under
     * the same id. Each channel therefore gets a fresh id - which is why the app's own `recreateNotificationChannel`
     * bumps a suffix rather than reusing the base id.
     */
    private fun createChannel(
        channelType: NotificationChannelType,
        importance: Int,
        bypassDnd: Boolean = false
    ) {
        val channelId = "${channelType.name}-${createdChannelCount++}"
        channelIds[channelType] = channelId
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, channelType.name, importance).apply {
                setBypassDnd(bypassDnd)
            }
        )
    }
}

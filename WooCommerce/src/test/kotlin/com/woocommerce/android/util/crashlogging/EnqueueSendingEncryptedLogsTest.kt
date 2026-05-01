package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.EventLevel.FATAL
import com.automattic.android.tracks.crashlogging.EventLevel.INFO
import com.automattic.encryptedlogging.EncryptedLogging
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class EnqueueSendingEncryptedLogsTest : BaseUnitTest() {
    private lateinit var sut: EnqueueSendingEncryptedLogs

    private val encryptedLogging: EncryptedLogging = mock()
    private val networkStatus: NetworkStatus = mock()

    private val uuid = "uuid"
    private val tempFile = File("temp")

    private val encryptedLogsFileProvider: EncryptedLogsFileProvider = mock {
        on { provide() } doReturn tempFile
    }

    @Before
    fun setUp() {
        sut = EnqueueSendingEncryptedLogs(
            encryptedLogging = encryptedLogging,
            encryptedLogsFileProvider = encryptedLogsFileProvider,
            networkStatus = networkStatus,
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
        )
    }

    @Test
    fun `should enqueue logs upload when log file is available and there's network connection`() {
        whenever(networkStatus.isConnected()).thenReturn(true)

        sut.invoke(uuid, INFO)

        verify(encryptedLogging, times(1)).enqueueSendingEncryptedLogs(
            uuid = uuid,
            file = tempFile,
            shouldUploadImmediately = true
        )
    }

    // If the connection is not available, we shouldn't try to upload immediately
    @Test
    fun `should not start upload immediately when event is not fatal but there's no network connection`() {
        whenever(networkStatus.isConnected()).thenReturn(false)

        sut.invoke(uuid, INFO)

        verify(encryptedLogging, times(1)).enqueueSendingEncryptedLogs(
            uuid = uuid,
            file = tempFile,
            shouldUploadImmediately = false
        )
    }

    @Test
    fun `should start upload immediately when event is not fatal and there's network connection`() {
        whenever(networkStatus.isConnected()).thenReturn(true)

        sut.invoke(uuid, INFO)

        verify(encryptedLogging, times(1)).enqueueSendingEncryptedLogs(
            uuid = uuid,
            file = tempFile,
            shouldUploadImmediately = true
        )
    }

    @Test
    fun `should not enqueue for immediately send when event is fatal`() {
        sut.invoke(uuid, FATAL)

        verify(encryptedLogging, times(1)).enqueueSendingEncryptedLogs(
            uuid = uuid,
            file = tempFile,
            shouldUploadImmediately = false
        )
    }
}

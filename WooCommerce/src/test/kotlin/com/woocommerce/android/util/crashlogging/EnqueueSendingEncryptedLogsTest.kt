package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.EventLevel.FATAL
import com.automattic.android.tracks.crashlogging.EventLevel.INFO
import com.woocommerce.android.tools.NetworkStatus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argForWhich
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.EncryptedLogAction.UPLOAD_LOG
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogPayload
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class EnqueueSendingEncryptedLogsTest {
    private lateinit var sut: EnqueueSendingEncryptedLogs

    private val eventBusDispatcher: Dispatcher = mock()
    private val networkStatus: NetworkStatus = mock()

    private val tempFile = File("temp")

    private val wooLogFileProvider: WooLogFileProvider = mock {
        on { provide() } doReturn tempFile
    }

    @Before
    fun setUp() {
        sut = EnqueueSendingEncryptedLogs(
            eventBusDispatcher = eventBusDispatcher,
            wooLogFileProvider = wooLogFileProvider,
            networkStatus = networkStatus
        )
    }

    @Test
    fun `should enqueue logs upload when log file is available and there's network connection`() {
        val uuid = "uuid"
        whenever(networkStatus.isConnected()).thenReturn(true)

        sut.invoke("uuid", INFO)

        verify(eventBusDispatcher, times(1)).dispatch(
            argForWhich {
                (payload as? UploadEncryptedLogPayload).let {
                    it?.shouldStartUploadImmediately == true &&
                        it.uuid == uuid &&
                        it.file == tempFile
                } && type == UPLOAD_LOG
            }
        )
    }

    // If the connection is not available, we shouldn't try to upload immediately
    @Test
    fun `should not start upload immediately when event is not fatal but there's no network connection`() {
        whenever(networkStatus.isConnected()).thenReturn(false)

        sut.invoke("uuid", INFO)

        verify(eventBusDispatcher, times(1)).dispatch(
            argForWhich {
                (payload as? UploadEncryptedLogPayload).let {
                    it?.shouldStartUploadImmediately == false
                }
            }
        )
    }

    @Test
    fun `should start upload immediately when event is not fatal and there's network connection`() {
        whenever(networkStatus.isConnected()).thenReturn(true)

        sut.invoke("uuid", INFO)

        verify(eventBusDispatcher, times(1)).dispatch(
            argForWhich {
                (payload as? UploadEncryptedLogPayload).let {
                    it?.shouldStartUploadImmediately == true
                }
            }
        )
    }

    @Test
    fun `should not enqueue for immediately send when event is fatal`() {
        sut.invoke("uuid", FATAL)

        verify(eventBusDispatcher, times(1)).dispatch(
            argForWhich {
                (payload as? UploadEncryptedLogPayload).let {
                    it?.shouldStartUploadImmediately == false
                }
            }
        )
    }
}

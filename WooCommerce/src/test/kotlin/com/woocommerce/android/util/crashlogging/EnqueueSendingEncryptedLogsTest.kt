package com.woocommerce.android.util.crashlogging

import com.nhaarman.mockitokotlin2.argForWhich
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.EncryptedLogAction.UPLOAD_LOG
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogPayload
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class EnqueueSendingEncryptedLogsTest {
    private lateinit var sut: EnqueueSendingEncryptedLogs

    private val eventBusDispatcher: Dispatcher = mock()
    private val wooLogFileProvider: WooLogFileProvider = mock()
    private val networkStatus: NetworkStatus = mock()

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

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
        val tempFile = File("temp")
        val startImmediately = true
        whenever(wooLogFileProvider.provide()).thenReturn(tempFile)
        whenever(networkStatus.isConnected()).thenReturn(true)

        sut.invoke("uuid", startImmediately)

        verify(eventBusDispatcher, times(1)).dispatch(argForWhich {
            (payload as? UploadEncryptedLogPayload).let {
                it?.shouldStartUploadImmediately == startImmediately &&
                    it.uuid == uuid &&
                    it.file == tempFile
            } && type == UPLOAD_LOG
        })
    }

    // If the connection is not available, we shouldn't try to upload immediately
    @Test
    fun `should not start upload immediately when requested but there's no network connection`() {
        whenever(wooLogFileProvider.provide()).thenReturn(File("temp"))
        whenever(networkStatus.isConnected()).thenReturn(false)

        sut.invoke("uuid", true)

        verify(eventBusDispatcher, times(1)).dispatch(argForWhich {
            (payload as? UploadEncryptedLogPayload).let {
                it?.shouldStartUploadImmediately == false
            }
        })
    }

    @Test
    fun `should start upload immediately when requested and there's network connection`() {
        whenever(wooLogFileProvider.provide()).thenReturn(File("temp"))
        whenever(networkStatus.isConnected()).thenReturn(true)

        sut.invoke("uuid", true)

        verify(eventBusDispatcher, times(1)).dispatch(argForWhich {
            (payload as? UploadEncryptedLogPayload).let {
                it?.shouldStartUploadImmediately == true
            }
        })
    }
}

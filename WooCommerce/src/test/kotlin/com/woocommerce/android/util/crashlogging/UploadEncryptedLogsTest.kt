package com.woocommerce.android.util.crashlogging

import com.woocommerce.android.viewmodel.BaseUnitTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.store.EncryptedLogStore

@RunWith(MockitoJUnitRunner::class)
class UploadEncryptedLogsTest : BaseUnitTest() {
    private lateinit var sut: UploadEncryptedLogs

    private val encryptedLogStore: EncryptedLogStore = mock()

    @Before
    fun setUp() {
        sut = UploadEncryptedLogs(
            encryptedLogStore = encryptedLogStore,
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `should start upload when invoked`() = testBlocking {
        sut.invoke()

        verify(encryptedLogStore, times(1)).uploadQueuedEncryptedLogs()
    }
}

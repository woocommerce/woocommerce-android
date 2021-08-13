package com.woocommerce.android.util.crashlogging

import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import com.woocommerce.android.util.CoroutineTestRule
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.EncryptedLogStore

@RunWith(MockitoJUnitRunner::class)
class UploadEncryptedLogsTest {
    private lateinit var sut: UploadEncryptedLogs

    private val encryptedLogStore: EncryptedLogStore = mock()

    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()

    @Before
    fun setUp() {
        sut = UploadEncryptedLogs(
            encryptedLogStore = encryptedLogStore,
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `should start upload when invoked`() = runBlockingTest {
        sut.invoke()

        verify(encryptedLogStore, times(1)).uploadQueuedEncryptedLogs()
    }
}

package com.woocommerce.android.util.crashlogging

import com.automattic.encryptedlogging.EncryptedLogging
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class UploadEncryptedLogsTest : BaseUnitTest() {
    private lateinit var sut: UploadEncryptedLogs

    private val encryptedLogging: EncryptedLogging = mock()

    @Before
    fun setUp() {
        sut = UploadEncryptedLogs(encryptedLogging)
    }

    @Test
    fun `should start upload when invoked`() = testBlocking {
        sut.invoke()

        verify(encryptedLogging, times(1)).uploadEncryptedLogs()
    }
}

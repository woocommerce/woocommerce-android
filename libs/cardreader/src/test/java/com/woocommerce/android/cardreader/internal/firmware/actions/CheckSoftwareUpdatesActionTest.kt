package com.woocommerce.android.cardreader.internal.firmware.actions

import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
internal class CheckSoftwareUpdatesActionTest {
    private lateinit var action: CheckSoftwareUpdatesAction
    private val terminal: TerminalWrapper = mock()
    private val updateData: ReaderSoftwareUpdate = mock()

    @Before
    fun setUp() {
        action = CheckSoftwareUpdatesAction(terminal, mock())
        whenever(updateData.hasConfigUpdate).thenReturn(false)
        whenever(updateData.hasFirmwareUpdate).thenReturn(false)
        whenever(updateData.hasKeyUpdate).thenReturn(false)
    }
}

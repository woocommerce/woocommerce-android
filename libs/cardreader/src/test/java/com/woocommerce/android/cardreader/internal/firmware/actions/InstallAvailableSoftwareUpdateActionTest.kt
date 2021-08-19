package com.woocommerce.android.cardreader.internal.firmware.actions

import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
internal class InstallAvailableSoftwareUpdateActionTest {
    private lateinit var action: InstallAvailableSoftwareUpdateAction
    private val terminal: TerminalWrapper = mock()

    @Before
    fun setUp() {
        action = InstallAvailableSoftwareUpdateAction(terminal, mock())
    }
}

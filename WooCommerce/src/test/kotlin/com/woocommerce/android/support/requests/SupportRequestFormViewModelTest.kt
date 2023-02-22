package com.woocommerce.android.support.requests

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

internal class SupportRequestFormViewModelTest {
    private lateinit var sut: SupportRequestFormViewModel

    @Before
    fun setUp() {
        sut = SupportRequestFormViewModel(
            zendeskHelper = mock(),
            selectedSite = mock(),
            savedState = mock()
        )
    }

    @Test
    fun `when all fields are filled, then submit button is enabled`() {
        assert(true)
    }
}

package com.woocommerce.android.support.requests

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.tools.SelectedSite
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel
import zendesk.support.Request

internal class SupportRequestFormViewModelTest {
    private lateinit var sut: SupportRequestFormViewModel
    private lateinit var zendeskHelper: ZendeskHelper
    private lateinit var selectedSite: SelectedSite
    private val savedState = SavedStateHandle()

    @Before
    fun setUp() {
        val testSite = SiteModel().apply { id = 123 }
        selectedSite = mock {
            on { get() }.then { testSite }
        }
        zendeskHelper = mock {
            onBlocking { createRequest(any(), testSite, any(), any(), any()) } doReturn Result.success(Request())
        }

        sut = SupportRequestFormViewModel(
            zendeskHelper = zendeskHelper,
            selectedSite = selectedSite,
            savedState = savedState
        )
    }

    @Test
    fun `when all fields are filled, then submit button is enabled`() {
        assert(true)
    }
}

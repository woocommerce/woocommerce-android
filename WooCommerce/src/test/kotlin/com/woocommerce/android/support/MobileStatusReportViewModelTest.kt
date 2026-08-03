package com.woocommerce.android.support

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.zendesk.MobileStatusProvider
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class MobileStatusReportViewModelTest : BaseUnitTest() {
    private val mobileStatusProvider: MobileStatusProvider = mock {
        on { invoke(selectedSite = SITE, siteAddress = null) } doReturn REPORT
    }

    private val selectedSite: SelectedSite = mock {
        on { getOrNull() } doReturn SITE
    }

    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: MobileStatusReportViewModel

    private fun setup() {
        viewModel = MobileStatusReportViewModel(
            savedState = SavedStateHandle(),
            mobileStatusProvider = mobileStatusProvider,
            selectedSite = selectedSite,
            analyticsTrackerWrapper = analyticsTrackerWrapper
        )
    }

    @Test
    fun `when the screen is opened, then the report for the selected store is shown`() = testBlocking {
        setup()

        assertThat(viewModel.viewState.value.report).isEqualTo(REPORT)
        assertThat(viewModel.viewState.value.isLoading).isFalse()
        verify(mobileStatusProvider).invoke(selectedSite = SITE, siteAddress = null)
    }

    @Test
    fun `given no store is selected, when the screen is opened, then the report is still produced`() = testBlocking {
        selectedSite.stub { on { getOrNull() } doReturn null }
        mobileStatusProvider.stub {
            on { invoke(selectedSite = null, siteAddress = null) } doReturn LOGGED_OUT_REPORT
        }

        setup()

        assertThat(viewModel.viewState.value.report).isEqualTo(LOGGED_OUT_REPORT)
    }

    @Test
    fun `when the copy button is clicked, then the report is copied`() = testBlocking {
        setup()

        val events = viewModel.event.runAndCaptureValues {
            viewModel.onCopyButtonClicked()
        }

        assertThat(events.last()).isEqualTo(CopyStatusReport(REPORT))
    }

    @Test
    fun `when the share button is clicked, then the report is shared`() = testBlocking {
        setup()

        val events = viewModel.event.runAndCaptureValues {
            viewModel.onShareButtonClicked()
        }

        assertThat(events.last()).isEqualTo(ShareStatusReport(REPORT))
    }

    @Test
    fun `given the report is empty, when the copy button is clicked, then nothing is copied`() = testBlocking {
        mobileStatusProvider.stub {
            on { invoke(selectedSite = SITE, siteAddress = null) } doReturn ""
        }
        setup()

        val events = viewModel.event.runAndCaptureValues {
            viewModel.onCopyButtonClicked()
        }

        assertThat(events).isEmpty()
    }

    @Test
    fun `when the back button is pressed, then the screen is closed`() = testBlocking {
        setup()

        val events = viewModel.event.runAndCaptureValues {
            viewModel.onBackPressed()
        }

        assertThat(events.last()).isEqualTo(Exit)
    }

    @Test
    fun `when the copy button is clicked, then the copy event is tracked`() = testBlocking {
        setup()

        viewModel.onCopyButtonClicked()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.SUPPORT_MOBILE_STATUS_REPORT_COPY_BUTTON_TAPPED)
    }

    @Test
    fun `given the report is empty, when the copy button is clicked, then nothing is tracked`() = testBlocking {
        mobileStatusProvider.stub {
            on { invoke(selectedSite = SITE, siteAddress = null) } doReturn ""
        }
        setup()

        viewModel.onCopyButtonClicked()

        verifyNoInteractions(analyticsTrackerWrapper)
    }

    private companion object {
        val SITE = SiteModel().apply { url = "https://example.com" }
        const val REPORT = "### Mobile Status Report ###\n\n## App (app-wide)\nVersion: 21.3"
        const val LOGGED_OUT_REPORT = "### Mobile Status Report ###\n\n## Store Details (no store selected)"
    }
}

package com.woocommerce.android.ui.login

import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class UnifiedLoginTrackerTest {
    private val tracker = UnifiedLoginTracker(analyticsTracker = mock())

    @Test
    fun `given help step is tracked, when reading step before help, then the previous step is returned`() {
        // GIVEN
        tracker.setStep(Step.ENTER_SITE_ADDRESS)

        // WHEN
        tracker.track(step = Step.HELP)

        // THEN
        assertThat(tracker.stepBeforeHelp).isEqualTo(Step.ENTER_SITE_ADDRESS)
    }

    @Test
    fun `given support details are set, when cleared, then they are null`() {
        // GIVEN
        tracker.onErrorShown("Error")
        tracker.onSiteAddressEntered("example.com")

        // WHEN
        tracker.clearSupportDetails()

        // THEN
        assertThat(tracker.lastShownError).isNull()
        assertThat(tracker.enteredSiteAddress).isNull()
    }
}

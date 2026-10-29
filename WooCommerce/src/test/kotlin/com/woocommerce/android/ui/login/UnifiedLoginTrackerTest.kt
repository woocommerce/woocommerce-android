package com.woocommerce.android.ui.login

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class UnifiedLoginTrackerTest {
    private val tracker = UnifiedLoginTracker(analyticsTracker = mock())

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

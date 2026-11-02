package com.woocommerce.android.ui.orders.wooshippinglabels.servicestatus

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiceStatusNoticesTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun givenNoticeWithLinkAndDismissWhenRenderedThenMessageIsOneTargetAndButtonsStaySeparate() {
        setNotices(listOf(DHL_NOTICE))

        composeTestRule.onNodeWithText(DHL_NOTICE.message).assertHasNoClickAction()
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(2)
        composeTestRule.onNodeWithText(viewStatusPageLabel).assertHasClickAction()
        composeTestRule.onNodeWithContentDescription(dismissLabelFor(DHL_NOTICE)).assertHasClickAction()
    }

    @Test
    fun givenNoticeWhenRenderedThenSeverityIsAnnouncedWithTheMessage() {
        setNotices(listOf(USPS_NOTICE))

        composeTestRule.onNodeWithText(USPS_NOTICE.message).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.woo_shipping_labels_service_status_severity_error)
            )
        )
    }

    @Test
    fun givenNoticeWithStatusPageWhenViewStatusPageIsTappedThenStatusPageOpens() {
        var openedUrl: String? = null
        setNotices(listOf(USPS_NOTICE), onActionClick = { openedUrl = it })

        composeTestRule.onNodeWithText(viewStatusPageLabel)
            .assertHasClickAction()
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(openedUrl).isEqualTo(USPS_NOTICE.actionUrl)
        }
    }

    @Test
    fun givenDismissibleNoticeWhenDismissButtonIsTappedThenNoticeIsDismissed() {
        var dismissedId: String? = null
        setNotices(listOf(UPS_NOTICE), onDismiss = { dismissedId = it })

        composeTestRule.onNodeWithContentDescription(dismissLabelFor(UPS_NOTICE))
            .assertHasClickAction()
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(dismissedId).isEqualTo(UPS_NOTICE.id)
        }
    }

    @Test
    fun givenNonDismissibleNoticeWithoutStatusPageWhenRenderedThenNoButtonsAreExposed() {
        setNotices(listOf(USPS_NOTICE.copy(actionUrl = null)))

        composeTestRule.onNodeWithContentDescription(dismissLabelFor(USPS_NOTICE)).assertDoesNotExist()
        composeTestRule.onNodeWithText(viewStatusPageLabel).assertDoesNotExist()
    }

    private val viewStatusPageLabel
        get() = context.getString(R.string.woo_shipping_labels_service_status_view_status_page)

    private fun dismissLabelFor(notice: ServiceStatusNotice) =
        context.getString(R.string.woo_shipping_labels_service_status_dismiss, notice.message)

    private fun setNotices(
        notices: List<ServiceStatusNotice>,
        onActionClick: (String) -> Unit = {},
        onDismiss: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            ServiceStatusNotices(notices = notices, onActionClick = onActionClick, onDismiss = onDismiss)
        }
    }

    private companion object {
        val USPS_NOTICE = ServiceStatusNotice(
            id = "usps",
            severity = ServiceStatusNotice.Severity.ERROR,
            message = "USPS: API Outage | Status: Investigating",
            isDismissible = false,
            actionUrl = "https://www.easypoststatus.com"
        )
        val DHL_NOTICE = ServiceStatusNotice(
            id = "dhl",
            severity = ServiceStatusNotice.Severity.INFO,
            message = "DHL Express: Scheduled maintenance tonight",
            isDismissible = true,
            actionUrl = "https://www.easypoststatus.com"
        )
        val UPS_NOTICE = ServiceStatusNotice(
            id = "ups",
            severity = ServiceStatusNotice.Severity.WARNING,
            message = "UPS: Label creation delays | Status: Monitoring",
            isDismissible = true,
            actionUrl = null
        )
    }
}

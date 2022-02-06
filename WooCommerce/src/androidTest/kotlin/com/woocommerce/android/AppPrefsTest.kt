package com.woocommerce.android

import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.AppPrefs.CardReaderOnboardingCompletedStatus
import com.woocommerce.android.ui.prefs.cardreader.onboarding.PluginType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class AppPrefsTest {
    @Before
    fun setup() {
        AppPrefs.init(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext)
    }

    @Test
    fun whenSetLastConnectedCardReaderIdThenIdStored() {
        val readerId = "id"

        AppPrefs.setLastConnectedCardReaderId(readerId)

        assertThat(AppPrefs.getLastConnectedCardReaderId()).isEqualTo(readerId)
    }

    @Test
    fun whenRemoveLastConnectedCardReaderIdThenIdIsNull() {
        val readerId = "id"
        AppPrefs.setLastConnectedCardReaderId(readerId)

        AppPrefs.removeLastConnectedCardReaderId()

        assertThat(AppPrefs.getLastConnectedCardReaderId()).isNull()
    }

    @Test
    fun whenCardReaderOnboardingCompletedWithStripeExtThenCorrectOnboardingStatusIsStored() {
        AppPrefs.setCardReaderOnboardingCompleted(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            pluginType = PluginType.STRIPE_EXTENSION_GATEWAY
        )

        assertThat(
            AppPrefs.getCardReaderOnboardingCompletedStatus(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(CardReaderOnboardingCompletedStatus.CARD_READER_ONBOARDING_COMPLETED_WITH_STRIPE_EXTENSION)
    }

    @Test
    fun whenCardReaderOnboardingPendingRequirementWithStripeExtThenCorrectOnboardingStatusIsStored() {
        AppPrefs.setCardReaderOnboardingPending(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            pluginType = PluginType.STRIPE_EXTENSION_GATEWAY
        )

        assertThat(
            AppPrefs.getCardReaderOnboardingCompletedStatus(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(
            CardReaderOnboardingCompletedStatus.CARD_READER_ONBOARDING_PENDING_REQUIREMENTS_WITH_STRIPE_EXTENSION
        )
    }

    @Test
    fun whenCardReaderOnboardingCompletedWithWCPayThenCorrectOnboardingStatusIsStored() {
        AppPrefs.setCardReaderOnboardingCompleted(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            pluginType = PluginType.WOOCOMMERCE_PAYMENTS
        )

        assertThat(
            AppPrefs.getCardReaderOnboardingCompletedStatus(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(CardReaderOnboardingCompletedStatus.CARD_READER_ONBOARDING_COMPLETED_WITH_WCPAY)
    }

    @Test
    fun whenCardReaderOnboardingPendingRequirementsWithWCPayThenCorrectOnboardingStatusIsStored() {
        AppPrefs.setCardReaderOnboardingPending(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            pluginType = PluginType.WOOCOMMERCE_PAYMENTS
        )

        assertThat(
            AppPrefs.getCardReaderOnboardingCompletedStatus(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(CardReaderOnboardingCompletedStatus.CARD_READER_ONBOARDING_PENDING_REQUIREMENTS_WITH_WCPAY)
    }

    @Test
    fun whenCardReaderOnboardingNotCompletedThenCorrectOnboardingStatusIsStored() {
        assertThat(
            AppPrefs.getCardReaderOnboardingCompletedStatus(
                localSiteId = 1,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(CardReaderOnboardingCompletedStatus.CARD_READER_ONBOARDING_NOT_COMPLETED)
    }

    @Test
    fun whenCardReaderOnboardingCompletedWithWCPayThenCorrectOnboardingStatusFlagIsReturned() {
        AppPrefs.setCardReaderOnboardingCompleted(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            pluginType = PluginType.WOOCOMMERCE_PAYMENTS
        )

        assertThat(
            AppPrefs.isCardReaderOnboardingCompleted(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isTrue
    }

    @Test
    fun whenCardReaderOnboardingPendingRequirementsWithWCPayThenCorrectOnboardingStatusFlagIsReturned() {
        AppPrefs.setCardReaderOnboardingPending(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            pluginType = PluginType.WOOCOMMERCE_PAYMENTS
        )

        assertThat(
            AppPrefs.isCardReaderOnboardingCompleted(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isFalse
    }

    @Test
    fun whenCardReaderOnboardingCompletedWithStripeExtThenCorrectOnboardingStatusFlagIsReturned() {
        AppPrefs.setCardReaderOnboardingCompleted(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            pluginType = PluginType.STRIPE_EXTENSION_GATEWAY
        )

        assertThat(
            AppPrefs.isCardReaderOnboardingCompleted(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isTrue
    }

    @Test
    fun whenCardReaderOnboardingPendingRequirementsWithStripeExtThenCorrectOnboardingStatusFlagIsReturned() {
        AppPrefs.setCardReaderOnboardingPending(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            pluginType = PluginType.STRIPE_EXTENSION_GATEWAY
        )

        assertThat(
            AppPrefs.isCardReaderOnboardingCompleted(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isFalse
    }

    @Test
    fun whenCardReaderOnboardingNotCompletedThenCorrectOnboardingStatusFlagIsReturned() {
        assertThat(
            AppPrefs.isCardReaderOnboardingCompleted(
                localSiteId = 1,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isFalse
    }

    @Test
    fun whenCardReaderOnboardingNotCompletedThenCorrectOnboardingStatusIsReturned() {
        AppPrefs.setCardReaderOnboardingCompleted(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            pluginType = null
        )

        assertThat(
            AppPrefs.isCardReaderOnboardingCompleted(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isFalse
    }

    @Test
    fun givenCardReaderStateDescriptorSetWhenGettingDescriptorThenSameDescriptorReturned() {
        val statementDescriptor = "descriptor"
        AppPrefs.setCardReaderStatementDescriptor(
            statementDescriptor = statementDescriptor,
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
        )

        assertThat(
            AppPrefs.getCardReaderStatementDescriptor(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(statementDescriptor)
    }
}

package com.woocommerce.android

import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.*
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
    fun whenFCMTokenIsSetThenGetReturnsStoredValue() {
        val token = "fcm_token"

        AppPrefs.setFCMToken(token)

        assertThat(AppPrefs.getFCMToken()).isEqualTo(token)
    }

    @Test
    fun whenFCMTokenIsRemovedThenGetReturnsEmptyString() {
        val token = "fcm_token"
        AppPrefs.setFCMToken(token)

        AppPrefs.removeFCMToken()

        assertThat(AppPrefs.getFCMToken()).isEmpty()
    }

    @Test
    fun whenCardReaderOnboardingCompletedWithStripeExtThenCorrectOnboardingStatusIsStored() {
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_COMPLETED,
            preferredPlugin = PluginType.STRIPE_EXTENSION_GATEWAY,
        )

        assertThat(
            AppPrefs.getCardReaderOnboardingStatus(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(CardReaderOnboardingStatus.CARD_READER_ONBOARDING_COMPLETED)
    }

    @Test
    fun whenCardReaderOnboardingCompletedWithStripeExtThenCorrectPreferredPluginIsStored() {
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_COMPLETED,
            preferredPlugin = PluginType.STRIPE_EXTENSION_GATEWAY,
        )

        assertThat(
            AppPrefs.getCardReaderPreferredPlugin(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(PluginType.STRIPE_EXTENSION_GATEWAY)
    }

    @Test
    fun whenCardReaderOnboardingPendingRequirementWithStripeExtThenCorrectOnboardingStatusIsStored() {
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_PENDING,
            preferredPlugin = PluginType.STRIPE_EXTENSION_GATEWAY,
        )

        assertThat(
            AppPrefs.getCardReaderOnboardingStatus(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(
            CardReaderOnboardingStatus.CARD_READER_ONBOARDING_PENDING
        )
    }

    @Test
    fun whenCardReaderOnboardingPendingRequirementWithStripeExtThenCorrectPreferredPluginIsStored() {
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_PENDING,
            preferredPlugin = PluginType.STRIPE_EXTENSION_GATEWAY,
        )

        assertThat(
            AppPrefs.getCardReaderPreferredPlugin(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(
            PluginType.STRIPE_EXTENSION_GATEWAY
        )
    }

    @Test
    fun whenCardReaderOnboardingCompletedWithWCPayThenCorrectOnboardingStatusIsStored() {
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_COMPLETED,
            preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS,
        )

        assertThat(
            AppPrefs.getCardReaderOnboardingStatus(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(CardReaderOnboardingStatus.CARD_READER_ONBOARDING_COMPLETED)
    }

    @Test
    fun whenCardReaderOnboardingCompletedWithWCPayThenCorrectPreferredPluginIsStored() {
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_COMPLETED,
            preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS,
        )

        assertThat(
            AppPrefs.getCardReaderPreferredPlugin(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(PluginType.WOOCOMMERCE_PAYMENTS)
    }

    @Test
    fun whenCardReaderOnboardingPendingRequirementsWithWCPayThenCorrectOnboardingStatusIsStored() {
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_PENDING,
            preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS,
        )

        assertThat(
            AppPrefs.getCardReaderOnboardingStatus(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(CardReaderOnboardingStatus.CARD_READER_ONBOARDING_PENDING)
    }

    @Test
    fun whenCardReaderOnboardingPendingRequirementsWithWCPayThenCorrectPreferredPluginIsStored() {
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_PENDING,
            preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS,
        )

        assertThat(
            AppPrefs.getCardReaderPreferredPlugin(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(PluginType.WOOCOMMERCE_PAYMENTS)
    }

    @Test
    fun whenCardReaderOnboardingNotCompletedThenCorrectOnboardingStatusIsStored() {
        assertThat(
            AppPrefs.getCardReaderOnboardingStatus(
                localSiteId = 1,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isEqualTo(CardReaderOnboardingStatus.CARD_READER_ONBOARDING_NOT_COMPLETED)
    }

    @Test
    fun whenCardReaderOnboardingCompletedWithWCPayThenCorrectOnboardingStatusFlagIsReturned() {
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_COMPLETED,
            preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS,
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
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_PENDING,
            preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS,
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
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_PENDING,
            preferredPlugin = PluginType.STRIPE_EXTENSION_GATEWAY,
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
    fun whenCardReaderOnboardingPendingRequirementsWithStripeExtThenCorrectOnboardingStatusFlagIsReturned() {
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_PENDING,
            preferredPlugin = PluginType.STRIPE_EXTENSION_GATEWAY,
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
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_NOT_COMPLETED,
            preferredPlugin = null,
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
    fun whenEmptyPreferredPluginSetThenNullReturned() {
        AppPrefs.setCardReaderOnboardingStatusAndPreferredPlugin(
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
            status = CARD_READER_ONBOARDING_NOT_COMPLETED,
            preferredPlugin = null,
        )

        assertThat(
            AppPrefs.getCardReaderPreferredPlugin(
                localSiteId = 0,
                remoteSiteId = 0L,
                selfHostedSiteId = 0L
            )
        ).isNull()
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

    @Test
    fun givenCardReaderStatementDescriptorNotEmptyWhenSettingItToNullThenValueOverriddenWithNull() {
        val statementDescriptor = "descriptor"
        AppPrefs.setCardReaderStatementDescriptor(
            statementDescriptor = statementDescriptor,
            localSiteId = 0,
            remoteSiteId = 0L,
            selfHostedSiteId = 0L,
        )

        AppPrefs.setCardReaderStatementDescriptor(
            statementDescriptor = null,
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
        ).isEqualTo(null)
    }

    @Test
    fun givenSpecificSiteIdWhenProductSortingChoiceIsSetThenGetReturnsStoredValue() {
        val siteIds = 1 to 2
        val productSortingChoices = "TITLE_ASC" to "TITLE_DESC"

        AppPrefs.setProductSortingChoice(siteIds.first, productSortingChoices.first)
        AppPrefs.setProductSortingChoice(siteIds.second, productSortingChoices.second)

        assertThat(AppPrefs.getProductSortingChoice(siteIds.first)).isEqualTo(productSortingChoices.first)
        assertThat(AppPrefs.getProductSortingChoice(siteIds.second)).isEqualTo(productSortingChoices.second)
    }
}

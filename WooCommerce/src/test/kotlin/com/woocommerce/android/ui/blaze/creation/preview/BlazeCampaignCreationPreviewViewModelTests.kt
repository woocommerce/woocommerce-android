package com.woocommerce.android.ui.blaze.creation.preview

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeRepository.BlazeCampaignImage
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.Device
import com.woocommerce.android.ui.blaze.Interest
import com.woocommerce.android.ui.blaze.Language
import com.woocommerce.android.ui.blaze.Location
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.AdDetailsUi
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BlazeCampaignCreationPreviewViewModelTests : BaseUnitTest() {
    companion object {
        private const val PRODUCT_ID = 1L
        private val defaultCampaignDetails = BlazeRepository.CampaignDetails(
            productId = PRODUCT_ID,
            tagLine = "",
            description = "",
            budget = BlazeRepository.Budget(
                totalBudget = 10f,
                spentBudget = 0f,
                currencyCode = "$",
                durationInDays = 7,
                startDate = Date(),
                isEndlessCampaign = false
            ),
            campaignImage = BlazeCampaignImage.None,
            destinationParameters = BlazeRepository.DestinationParameters(
                targetUrl = "http://test_url",
                parameters = emptyMap()
            ),
            targetingParameters = BlazeRepository.TargetingParameters()
        )
        private val locations = listOf(Location(1, "Location 1"), Location(2, "Location 2"))
        private val languages = listOf(Language("en", "English"), Language("es", "Spanish"))
        private val interests = listOf(Interest("1", "Interest 1"), Interest("2", "Interest 2"))
        private val devices = listOf(Device("1", "Device 1"), Device("2", "Device 2"))
    }

    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(amount = any(), any(), any()) }.doAnswer { it.getArgument<BigDecimal>(0).toString() }
    }
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.getArgument<Any?>(0).toString() }
        on { getString(any(), anyVararg()) } doAnswer { it.arguments.joinToString { it.toString() } }
    }
    private val blazeRepository: BlazeRepository = mock {
        onBlocking { generateDefaultCampaignDetails(PRODUCT_ID) } doReturn defaultCampaignDetails
        on { observeDevices() } doReturn flowOf(devices)
        on { observeInterests() } doReturn flowOf(interests)
        on { observeLanguages() } doReturn flowOf(languages)
    }
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private lateinit var viewModel: BlazeCampaignCreationPreviewViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = BlazeCampaignCreationPreviewViewModel(
            savedStateHandle = BlazeCampaignCreationPreviewFragmentArgs(
                source = BlazeFlowSource.CAMPAIGN_LIST,
                productId = PRODUCT_ID
            ).toSavedStateHandle(),
            blazeRepository = blazeRepository,
            resourceProvider = resourceProvider,
            currencyFormatter = currencyFormatter,
            analyticsTrackerWrapper = analyticsTracker
        )
    }

    @Test
    fun `when screen is opened, then fetch ad suggestions`() = testBlocking {
        val adSuggestions = listOf(
            BlazeRepository.AiSuggestionForAd(
                tagLine = "Ad suggestion 1",
                description = "Ad suggestion 1 description",
            )
        )
        setup {
            whenever(blazeRepository.fetchAdSuggestions(PRODUCT_ID)).doSuspendableAnswer {
                delay(10)
                Result.success(adSuggestions)
            }
        }

        val states = viewModel.viewState.captureValues()
        advanceUntilIdle()

        assertThat(states.first().adDetails).isInstanceOf(AdDetailsUi.Loading::class.java)
        assertThat(states.last().adDetails).isInstanceOf(AdDetailsUi.AdDetails::class.java)
        assertThat((states.last().adDetails as AdDetailsUi.AdDetails).tagLine)
            .isEqualTo(adSuggestions.first().tagLine)
        assertThat((states.last().adDetails as AdDetailsUi.AdDetails).description)
            .isEqualTo(adSuggestions.first().description)
    }

    @Test
    fun `when screen is opened, then fetch targeting options`() = testBlocking {
        setup {
            whenever(blazeRepository.fetchLanguages()).doReturn(Result.success(Unit))
            whenever(blazeRepository.fetchInterests()).doReturn(Result.success(Unit))
            whenever(blazeRepository.fetchDevices()).doReturn(Result.success(Unit))
        }

        advanceUntilIdle()

        verify(blazeRepository).fetchLanguages()
        verify(blazeRepository).fetchInterests()
        verify(blazeRepository).fetchDevices()
    }

    @Test
    fun `when screen is opened, then show default campaign details`() = testBlocking {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()

        assertThat(state.campaignDetails.budget.displayValue).isEqualTo(
            "${
                currencyFormatter.formatCurrency(
                    amount = defaultCampaignDetails.budget.totalBudget.toBigDecimal(),
                    currencyCode = defaultCampaignDetails.budget.currencyCode
                )
            }, ${
                resourceProvider.getString(
                    R.string.blaze_campaign_preview_days_duration,
                    defaultCampaignDetails.budget.durationInDays,
                    defaultCampaignDetails.budget.startDate.formatToMMMdd()
                )
            }"
        )
        assertThat(state.campaignDetails.targetDetails).hasSize(4)
        assertThat(state.campaignDetails.targetDetails).allMatch {
            it.displayValue == resourceProvider.getString(R.string.blaze_campaign_preview_target_default_value)
        }
        assertThat(state.campaignDetails.destinationUrl.displayValue)
            .isEqualTo(defaultCampaignDetails.destinationParameters.targetUrl)
    }

    @Test
    fun `when tapping on edit ad, then open edit ad screen`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onEditAdClicked()
        }.last()

        assertThat(event)
            .isInstanceOf(BlazeCampaignCreationPreviewViewModel.NavigateToEditAdScreen::class.java)
    }

    @Test
    fun `when ad details are changed, then update campaign details`() = testBlocking {
        setup()

        val newTagline = "New tagline"
        val newDescription = "New description"
        val newImage = BlazeCampaignImage.LocalImage("new_image")
        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onAdUpdated(newTagline, newDescription, newImage)
        }.last()

        val adDetailsUi = state.adDetails as AdDetailsUi.AdDetails
        assertThat(adDetailsUi.tagLine).isEqualTo(newTagline)
        assertThat(adDetailsUi.description).isEqualTo(newDescription)
        assertThat(adDetailsUi.campaignImageUrl).isEqualTo(newImage.uri)
    }

    @Test
    fun `when tapping on language, then open language selection screen`() = testBlocking {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()
        val event = viewModel.event.runAndCaptureValues {
            val languageTargetUi = state.campaignDetails.targetDetails.first {
                it.displayTitle == resourceProvider.getString(R.string.blaze_campaign_preview_details_language)
            }
            languageTargetUi.onItemSelected.invoke()
        }.last()

        assertThat(event)
            .isInstanceOf(BlazeCampaignCreationPreviewViewModel.NavigateToTargetSelectionScreen::class.java)
    }

    @Test
    fun `when languages are changed, then update campaign details`() = testBlocking {
        setup()

        val newLanguage = languages.last()
        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onTargetSelectionUpdated(BlazeTargetType.LANGUAGE, listOf(newLanguage.code))
        }.last()

        val languageTargetUi = state.campaignDetails.targetDetails.first {
            it.displayTitle == resourceProvider.getString(R.string.blaze_campaign_preview_details_language)
        }
        assertThat(languageTargetUi.displayValue).isEqualTo(newLanguage.name)
    }

    @Test
    fun `when tapping on interest, then open interest selection screen`() = testBlocking {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()
        val event = viewModel.event.runAndCaptureValues {
            val interestTargetUi = state.campaignDetails.targetDetails.first {
                it.displayTitle == resourceProvider.getString(R.string.blaze_campaign_preview_details_interests)
            }
            interestTargetUi.onItemSelected.invoke()
        }.last()

        assertThat(event)
            .isInstanceOf(BlazeCampaignCreationPreviewViewModel.NavigateToTargetSelectionScreen::class.java)
    }

    @Test
    fun `when interests are changed, then update campaign details`() = testBlocking {
        setup()

        val newInterest = interests.last()
        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onTargetSelectionUpdated(BlazeTargetType.INTEREST, listOf(newInterest.id))
        }.last()

        val interestTargetUi = state.campaignDetails.targetDetails.first {
            it.displayTitle == resourceProvider.getString(R.string.blaze_campaign_preview_details_interests)
        }
        assertThat(interestTargetUi.displayValue).isEqualTo(newInterest.description)
    }

    @Test
    fun `when tapping on device, then open device selection screen`() = testBlocking {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()
        val event = viewModel.event.runAndCaptureValues {
            val deviceTargetUi = state.campaignDetails.targetDetails.first {
                it.displayTitle == resourceProvider.getString(R.string.blaze_campaign_preview_details_devices)
            }
            deviceTargetUi.onItemSelected.invoke()
        }.last()

        assertThat(event)
            .isInstanceOf(BlazeCampaignCreationPreviewViewModel.NavigateToTargetSelectionScreen::class.java)
    }

    @Test
    fun `when devices are changed, then update campaign details`() = testBlocking {
        setup()

        val newDevice = devices.last()
        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onTargetSelectionUpdated(BlazeTargetType.DEVICE, listOf(newDevice.id))
        }.last()

        val deviceTargetUi = state.campaignDetails.targetDetails.first {
            it.displayTitle == resourceProvider.getString(R.string.blaze_campaign_preview_details_devices)
        }
        assertThat(deviceTargetUi.displayValue).isEqualTo(newDevice.name)
    }

    @Test
    fun `when tapping on location, then open location selection screen`() = testBlocking {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()
        val event = viewModel.event.runAndCaptureValues {
            val locationTargetUi = state.campaignDetails.targetDetails.first {
                it.displayTitle == resourceProvider.getString(R.string.blaze_campaign_preview_details_location)
            }
            locationTargetUi.onItemSelected.invoke()
        }.last()

        assertThat(event)
            .isInstanceOf(BlazeCampaignCreationPreviewViewModel.NavigateToTargetLocationSelectionScreen::class.java)
    }

    @Test
    fun `when locations are changed, then update campaign details`() = testBlocking {
        setup()

        val newLocation = locations.first()
        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onTargetLocationsUpdated(listOf(newLocation))
        }.last()

        val locationTargetUi = state.campaignDetails.targetDetails.first {
            it.displayTitle == resourceProvider.getString(R.string.blaze_campaign_preview_details_location)
        }
        assertThat(locationTargetUi.displayValue).isEqualTo(newLocation.name)
    }

    @Test
    fun `when tapping on budget, then open budget selection screen`() = testBlocking {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()
        val event = viewModel.event.runAndCaptureValues {
            state.campaignDetails.budget.onItemSelected.invoke()
        }.last()

        assertThat(event).isInstanceOf(BlazeCampaignCreationPreviewViewModel.NavigateToBudgetScreen::class.java)
    }

    @Test
    fun `when budget changes, then update campaign details`() = testBlocking {
        setup()

        val newBudget = BlazeRepository.Budget(
            totalBudget = 20f,
            spentBudget = 0f,
            currencyCode = "$",
            durationInDays = 14,
            startDate = Date(),
            isEndlessCampaign = false
        )
        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onBudgetAndDurationUpdated(newBudget)
        }.last()

        assertThat(state.campaignDetails.budget.displayValue).isEqualTo(
            "${
                currencyFormatter.formatCurrency(
                    amount = newBudget.totalBudget.toBigDecimal(),
                    currencyCode = newBudget.currencyCode
                )
            }, ${
                resourceProvider.getString(
                    R.string.blaze_campaign_preview_days_duration,
                    newBudget.durationInDays,
                    newBudget.startDate.formatToMMMdd()
                )
            }"
        )
    }

    @Test
    fun `when tapping on destination url, then open destination url screen`() = testBlocking {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()
        val event = viewModel.event.runAndCaptureValues {
            state.campaignDetails.destinationUrl.onItemSelected.invoke()
        }.last()

        assertThat(event).isInstanceOf(BlazeCampaignCreationPreviewViewModel.NavigateToAdDestinationScreen::class.java)
    }

    @Test
    fun `when destination changes, then update campaign details`() = testBlocking {
        setup()

        val newDestination = BlazeRepository.DestinationParameters(
            targetUrl = "http://new_url",
            parameters = mapOf("key" to "value")
        )
        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onDestinationUpdated(newDestination)
        }.last()

        assertThat(state.campaignDetails.destinationUrl.displayValue).isEqualTo(newDestination.fullUrl)
    }

    @Test
    fun `given image is missing, when tapping on confirm, then show a dialog`() = testBlocking {
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onConfirmClicked()
        }.last()

        assertThat(state.dialogState).isNotNull
        assertThat(state.dialogState!!.message).isEqualTo(
            UiString.UiStringRes(R.string.blaze_campaign_preview_missing_image_dialog_text)
        )
        assertThat(state.dialogState!!.positiveButton!!.text).isEqualTo(
            UiString.UiStringRes(R.string.blaze_campaign_preview_missing_image_dialog_positive_button)
        )
        assertThat(state.dialogState!!.negativeButton!!.text).isEqualTo(
            UiString.UiStringRes(R.string.cancel)
        )
    }

    @Test
    fun `given image is missing, when tapping on add image of confirm dialog, then edit ad screen`() = testBlocking {
        setup()

        val dialogState = viewModel.viewState.runAndCaptureValues {
            viewModel.onConfirmClicked()
        }.last().dialogState!!
        val event = viewModel.event.runAndCaptureValues {
            dialogState.positiveButton!!.onClick.invoke()
        }.last()
        val viewState = viewModel.viewState.getOrAwaitValue()

        assertThat(viewState.dialogState).isNull()
        assertThat(event).isInstanceOf(BlazeCampaignCreationPreviewViewModel.NavigateToEditAdScreen::class.java)
    }

    @Test
    fun `given ad details missing, when tapping on confirm, then show a dialog`() = testBlocking {
        setup {
            whenever(blazeRepository.generateDefaultCampaignDetails(PRODUCT_ID)).doReturn(
                defaultCampaignDetails.copy(
                    campaignImage = BlazeCampaignImage.LocalImage("image")
                )
            )
        }

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onConfirmClicked()
        }.last()

        assertThat(state.dialogState).isNotNull
        assertThat(state.dialogState!!.message).isEqualTo(
            UiString.UiStringRes(R.string.blaze_campaign_preview_missing_content_dialog_text)
        )
        assertThat(state.dialogState!!.positiveButton!!.text).isEqualTo(
            UiString.UiStringRes(R.string.blaze_campaign_preview_missing_content_dialog_positive_button)
        )
        assertThat(state.dialogState!!.negativeButton!!.text).isEqualTo(
            UiString.UiStringRes(R.string.cancel)
        )
    }

    @Test
    fun `given ad details missing, when tapping on add content of confirm dialog, then edit ad screen`() =
        testBlocking {
            setup {
                whenever(blazeRepository.generateDefaultCampaignDetails(PRODUCT_ID)).doReturn(
                    defaultCampaignDetails.copy(
                        campaignImage = BlazeCampaignImage.LocalImage("image")
                    )
                )
            }

            val dialogState = viewModel.viewState.runAndCaptureValues {
                viewModel.onConfirmClicked()
            }.last().dialogState!!
            val event = viewModel.event.runAndCaptureValues {
                dialogState.positiveButton!!.onClick.invoke()
            }.last()
            val viewState = viewModel.viewState.getOrAwaitValue()

            assertThat(viewState.dialogState).isNull()
            assertThat(event).isInstanceOf(BlazeCampaignCreationPreviewViewModel.NavigateToEditAdScreen::class.java)
        }

    @Test
    fun `given campaign requirements met, when tapping on confirm, then open payment summary`() = testBlocking {
        setup {
            whenever(blazeRepository.generateDefaultCampaignDetails(PRODUCT_ID)).doReturn(
                defaultCampaignDetails.copy(
                    campaignImage = BlazeCampaignImage.LocalImage("image"),
                    tagLine = "tagline",
                    description = "description"
                )
            )
        }

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onConfirmClicked()
        }.last()

        assertThat(event).isInstanceOf(BlazeCampaignCreationPreviewViewModel.NavigateToPaymentSummary::class.java)
    }
}

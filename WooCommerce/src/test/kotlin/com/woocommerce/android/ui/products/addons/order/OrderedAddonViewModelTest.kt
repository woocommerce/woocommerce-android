package com.woocommerce.android.ui.products.addons.order

import android.content.Context
import android.content.SharedPreferences
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultOrderAttributes
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultOrderedAddonList
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultProductAddonList
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.emptyProductAddon
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.listWithSingleAddonAndTwoValidOptions
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.orderAttributesWithPercentageBasedItem
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.orderedAddonWithPercentageBasedDeliveryOptionList
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.PriceType
import kotlin.test.fail

@ExperimentalCoroutinesApi
class OrderedAddonViewModelTest : BaseUnitTest() {
    private lateinit var viewModelUnderTest: OrderedAddonViewModel
    private var addonRepositoryMock: AddonRepository = mock()

    @Before
    fun setUp() {
        val storeParametersMock = mock<SiteParameters> {
            on { currencyCode }.doReturn("currency-code")
        }
        val savedState = OrderedAddonFragmentArgs(
            orderId = 321,
            orderItemId = 999,
            addonsProductId = 123
        ).initSavedStateHandle()

        val parameterRepositoryMock = mock<ParameterRepository> {
            on { getParameters("key_product_parameters", savedState) }
                .doReturn(storeParametersMock)
        }

        val editor = mock<SharedPreferences.Editor>()
        val preferences = mock<SharedPreferences> { whenever(it.edit()).thenReturn(editor) }
        mock<Context> {
            whenever(it.applicationContext).thenReturn(it)
            whenever(it.getSharedPreferences(any(), any())).thenReturn(preferences)
            FeedbackPrefs.init(it)
        }

        viewModelUnderTest = OrderedAddonViewModel(
            savedState,
            coroutinesTestRule.testDispatchers,
            addonRepositoryMock,
            parameterRepositoryMock
        )
    }

    @Test
    fun `should trigger a successful parse to all data at once`() =
        testBlocking {
            whenever(addonRepositoryMock.updateGlobalAddonsSuccessfully()).thenReturn(true)
            whenever(addonRepositoryMock.getOrderAddonsData(321, 999, 123))
                .thenReturn(Pair(defaultProductAddonList, defaultOrderAttributes))

            val expectedResult = defaultOrderedAddonList

            var actualResult: List<Addon>? = null
            viewModelUnderTest.orderedAddonsData.observeForever {
                actualResult = it
            }

            viewModelUnderTest.start(321, 999, 123)

            assertThat(actualResult).isEqualTo(expectedResult)
        }

    @Test
    fun `should parse Addons with parsed option as FlatFee when matching PercentageBased option is found`() =
        testBlocking {
            whenever(addonRepositoryMock.getOrderAddonsData(321, 999, 123))
                .thenReturn(Pair(defaultProductAddonList, orderAttributesWithPercentageBasedItem))
            whenever(addonRepositoryMock.updateGlobalAddonsSuccessfully()).thenReturn(false)

            val expectedResult = orderedAddonWithPercentageBasedDeliveryOptionList

            var actualResult: List<Addon>? = null
            viewModelUnderTest.orderedAddonsData.observeForever {
                actualResult = it
            }

            viewModelUnderTest.start(321, 999, 123)

            assertThat(actualResult).isEqualTo(expectedResult)
        }

    @Test
    fun `should request data even if fetchGlobalAddons returns an error`() =
        testBlocking {
            whenever(addonRepositoryMock.getOrderAddonsData(321, 999, 123))
                .thenReturn(Pair(defaultProductAddonList, defaultOrderAttributes))
            whenever(addonRepositoryMock.updateGlobalAddonsSuccessfully()).thenReturn(false)

            val expectedResult = defaultOrderedAddonList

            var actualResult: List<Addon>? = null
            viewModelUnderTest.orderedAddonsData.observeForever {
                actualResult = it
            }

            viewModelUnderTest.start(321, 999, 123)

            assertThat(actualResult).isEqualTo(expectedResult)
        }

    @Test
    fun `should inject Attribute data when matching option is NOT found`() =
        testBlocking {
            val mockResponse = Pair(
                listOf(
                    emptyProductAddon.copy(
                        name = "Delivery",
                    )
                ),
                defaultOrderAttributes
            )

            whenever(addonRepositoryMock.getOrderAddonsData(321, 999, 123))
                .doReturn(mockResponse)
            whenever(addonRepositoryMock.updateGlobalAddonsSuccessfully()).thenReturn(true)

            val expectedResult = emptyProductAddon.copy(
                name = "Delivery",
                options = listOf(
                    Addon.HasOptions.Option(
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "$5,00",
                        ),
                        label = "Yes",
                        image = ""
                    )
                )
            ).let { listOf(it) }

            var actualResult: List<Addon>? = null
            viewModelUnderTest.orderedAddonsData.observeForever {
                actualResult = it
            }

            viewModelUnderTest.start(321, 999, 123)

            assertThat(actualResult).isEqualTo(expectedResult)
        }

    @Test
    fun `should return Addon with single option when matching option is found`() =
        testBlocking {
            whenever(addonRepositoryMock.getOrderAddonsData(321, 999, 123))
                .thenReturn(
                    Pair(
                        listWithSingleAddonAndTwoValidOptions,
                        listOf(
                            Order.Item.Attribute(
                                "test-name (test-price)",
                                "test-label"
                            )
                        )
                    )
                )
            whenever(addonRepositoryMock.updateGlobalAddonsSuccessfully()).thenReturn(true)

            val expectedResult = emptyProductAddon.copy(
                name = "test-name",
                options = listOf(
                    Addon.HasOptions.Option(
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "test-price"
                        ),
                        label = "test-label",
                        image = "test-image"
                    )
                )
            ).let { listOf(it) }

            var actualResult: List<Addon>? = null
            viewModelUnderTest.orderedAddonsData.observeForever {
                actualResult = it
            }

            viewModelUnderTest.start(321, 999, 123)

            assertThat(actualResult).isEqualTo(expectedResult)
        }

    @Test
    fun `should return two Addons with a single option when matching addon is found twice`() =
        testBlocking {
            whenever(addonRepositoryMock.getOrderAddonsData(321, 999, 123))
                .doReturn(
                    Pair(
                        listWithSingleAddonAndTwoValidOptions,
                        listOf(
                            Order.Item.Attribute(
                                "test-name (test-price)",
                                "test-label"
                            ),
                            Order.Item.Attribute(
                                "test-name (test-price-2)",
                                "test-label-2"
                            )
                        )
                    )
                )
            whenever(addonRepositoryMock.updateGlobalAddonsSuccessfully()).thenReturn(true)

            val expectedResult = listOf(
                emptyProductAddon.copy(
                    name = "test-name",
                    options = listOf(
                        Addon.HasOptions.Option(
                            price = Addon.HasAdjustablePrice.Price.Adjusted(
                                priceType = PriceType.FlatFee,
                                value = "test-price"
                            ),
                            label = "test-label",
                            image = "test-image"
                        )
                    )
                ),
                emptyProductAddon.copy(
                    name = "test-name",
                    options = listOf(
                        Addon.HasOptions.Option(
                            price = Addon.HasAdjustablePrice.Price.Adjusted(
                                priceType = PriceType.FlatFee,
                                value = "test-price-2",
                            ),
                            label = "test-label-2",
                            image = "test-image-2"
                        )
                    )
                )
            )

            var actualResult: List<Addon>? = null
            viewModelUnderTest.orderedAddonsData.observeForever {
                actualResult = it
            }

            viewModelUnderTest.start(321, 999, 123)

            assertThat(actualResult).isEqualTo(expectedResult)
        }

    @Test
    fun `should enable and disable skeleton view when loading the view data`() =
        testBlocking {
            whenever(addonRepositoryMock.updateGlobalAddonsSuccessfully()).thenReturn(true)
            whenever(addonRepositoryMock.getOrderAddonsData(321, 999, 123))
                .thenReturn(Pair(defaultProductAddonList, defaultOrderAttributes))

            var timesCalled = 0
            var viewModelStarted = false
            viewModelUnderTest.viewStateLiveData.observeForever { old, new ->
                if (viewModelStarted) {
                    when (timesCalled) {
                        0 -> assertThat(new.isSkeletonShown).isTrue
                        1 -> assertThat(new.isSkeletonShown).isFalse
                        else -> fail("View state is expected to be changed exactly two times")
                    }

                    timesCalled++
                }
            }

            viewModelStarted = true
            viewModelUnderTest.start(321, 999, 123)

            assertThat(timesCalled).isEqualTo(2)
        }

    @Test
    fun `should enable and disable skeleton view when loading the view data fails`() =
        testBlocking {
            whenever(addonRepositoryMock.updateGlobalAddonsSuccessfully()).thenReturn(false)

            var timesCalled = 0
            var viewModelStarted = false
            viewModelUnderTest.viewStateLiveData.observeForever { old, new ->
                if (viewModelStarted) {
                    when (timesCalled) {
                        0 -> assertThat(new.isSkeletonShown).isTrue
                        1 -> assertThat(new.isSkeletonShown).isFalse
                        else -> fail("View state is expected to be changed exactly two times")
                    }

                    timesCalled++
                }
            }

            viewModelStarted = true
            viewModelUnderTest.start(321, 999, 123)

            assertThat(timesCalled).isEqualTo(2)
        }

    @Test
    fun `should change isLoadingFailure to true when loading the view data fails`() =
        testBlocking {
            whenever(addonRepositoryMock.updateGlobalAddonsSuccessfully()).thenReturn(false)

            var timesCalled = 0
            var viewModelStarted = false
            viewModelUnderTest.viewStateLiveData.observeForever { old, new ->
                if (viewModelStarted) {
                    when (timesCalled) {
                        0 -> assertThat(new.isLoadingFailure).isFalse
                        1 -> assertThat(new.isLoadingFailure).isTrue
                        else -> fail("View state is expected to be changed exactly two times")
                    }

                    timesCalled++
                }
            }

            viewModelStarted = true
            viewModelUnderTest.start(321, 999, 123)

            assertThat(timesCalled).isEqualTo(2)
        }

    @Test
    fun `should change isLoadingFailure to true when the Ordered Addons data is empty`() =
        testBlocking {
            whenever(addonRepositoryMock.updateGlobalAddonsSuccessfully()).thenReturn(false)
            whenever(addonRepositoryMock.getOrderAddonsData(321, 999, 123))
                .thenReturn(Pair(emptyList(), emptyList()))

            var timesCalled = 0
            var viewModelStarted = false
            viewModelUnderTest.viewStateLiveData.observeForever { old, new ->
                if (viewModelStarted) {
                    when (timesCalled) {
                        0 -> assertThat(new.isLoadingFailure).isFalse
                        1 -> assertThat(new.isLoadingFailure).isTrue
                        else -> fail("View state is expected to be changed exactly two times")
                    }

                    timesCalled++
                }
            }

            viewModelStarted = true
            viewModelUnderTest.start(321, 999, 123)

            assertThat(timesCalled).isEqualTo(2)
        }

    @Test
    fun `should keep isLoadingFailure to false when loading the view data succeeds`() =
        testBlocking {
            whenever(addonRepositoryMock.updateGlobalAddonsSuccessfully()).thenReturn(true)
            whenever(addonRepositoryMock.getOrderAddonsData(321, 999, 123))
                .thenReturn(Pair(defaultProductAddonList, defaultOrderAttributes))

            var timesCalled = 0
            var viewModelStarted = false
            viewModelUnderTest.viewStateLiveData.observeForever { old, new ->
                if (viewModelStarted) {
                    when (timesCalled) {
                        0 -> assertThat(new.isLoadingFailure).isFalse
                        1 -> assertThat(new.isLoadingFailure).isFalse
                        else -> fail("View state is expected to be changed exactly two times")
                    }

                    timesCalled++
                }
            }

            viewModelStarted = true
            viewModelUnderTest.start(321, 999, 123)

            assertThat(timesCalled).isEqualTo(2)
        }
}

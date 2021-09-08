package com.woocommerce.android.ui.products.addons.order

import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultAddonsList
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultOrderAttributes
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultOrderedAddonList
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultProductAddonList
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.emptyProductAddon
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.listWithSingleAddonAndTwoValidOptions
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.PriceType

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
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

        viewModelUnderTest = OrderedAddonViewModel(
            savedState,
            coroutinesTestRule.testDispatchers,
            addonRepositoryMock,
            parameterRepositoryMock
        )
    }

    @Test
    fun `should trigger a successful parse to all data at once`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `should return null if fetchGlobalAddons returns an error`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(addonRepositoryMock.getOrderAddonsData(321, 999, 123))
                .thenReturn(Pair(defaultAddonsList, defaultOrderAttributes))
            whenever(addonRepositoryMock.updateGlobalAddonsSuccessfully()).thenReturn(false)

            var actualResult: List<Addon>? = null
            viewModelUnderTest.orderedAddonsData.observeForever {
                actualResult = it
            }

            viewModelUnderTest.start(321, 999, 123)

            assertThat(actualResult).isNull()
        }

    @Test
    fun `should inject Attribute data when matching option is NOT found`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
}

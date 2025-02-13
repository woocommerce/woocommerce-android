package org.wordpress.android.fluxc.store

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.Addon.CustomText.Restrictions
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.PriceType
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.NotAdjusted
import org.wordpress.android.fluxc.domain.Addon.MultipleChoice.Display
import org.wordpress.android.fluxc.domain.Addon.TitleFormat
import org.wordpress.android.fluxc.domain.GlobalAddonGroup
import org.wordpress.android.fluxc.domain.GlobalAddonGroup.CategoriesRestriction.AllProductsCategories
import org.wordpress.android.fluxc.domain.GlobalAddonGroup.CategoriesRestriction.SpecifiedProductCategories
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteDisplay.RadioButton
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemotePriceType.FlatFee
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemotePriceType.PercentageBased
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteRestrictionsType.Email
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteTitleFormat.Heading
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteTitleFormat.Hide
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteTitleFormat.Label
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.Checkbox
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.CustomPrice
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.CustomText
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.CustomTextArea
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.FileUpload
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.InputMultiplier
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.MultipleChoice
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.AddOnsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.dto.AddOnGroupDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.mappers.RemoteGlobalAddonGroupMapper
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.mappers.FromDatabaseAddonGroupMapper
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T.API

@RunWith(RobolectricTestRunner::class)
class ProvideAddonsIntegrationTests {
    private lateinit var sut: WCAddonsStore

    private lateinit var database: WCAndroidDatabase
    private lateinit var dao: AddonsDao
    private lateinit var restClient: AddOnsRestClient
    private lateinit var coroutineEngine: CoroutineEngine
    private lateinit var logger: AppLogWrapper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        logger = mock()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.addonsDao
        restClient = mock()
        coroutineEngine = CoroutineEngine(
                context = Default,
                appLog = logger
        )

        sut = WCAddonsStore(
                restClient = restClient,
                coroutineEngine = coroutineEngine,
                dao = dao,
                remoteGlobalAddonGroupMapper = RemoteGlobalAddonGroupMapper(logger),
                fromDatabaseAddonGroupMapper = FromDatabaseAddonGroupMapper(logger),
                logger = logger
        )
    }

    @Test
    fun `should map CustomText add-on`() {
        runBlocking {
            whenever(restClient.fetchGlobalAddOnGroups(any())).thenReturn(
                    WooPayload(
                            result = groupWith(
                                    RemoteAddonDto(
                                            titleFormat = Heading,
                                            descriptionEnabled = 0,
                                            restrictionsType = Email,
                                            adjustPrice = 1,
                                            priceType = FlatFee,
                                            type = CustomText,
                                            name = "CustomText",
                                            description = "",
                                            required = 1,
                                            position = 5,
                                            min = 2,
                                            max = 5,
                                            price = "100$"
                                    )
                            )
                    )
            )

            sut.fetchAllGlobalAddonsGroups(siteModel)

            assertThat(sut.observeAllAddonsForProduct(siteModel, product).first().first()).isEqualTo(
                    Addon.CustomText(
                            name = "CustomText",
                            titleFormat = TitleFormat.Heading,
                            description = null,
                            required = true,
                            position = 5,
                            price = Addon.HasAdjustablePrice.Price.Adjusted(
                                    priceType = PriceType.FlatFee,
                                    value = "100$"
                            ),
                            restrictions = Restrictions.Email,
                            characterLength = 2L..5L
                    )
            )
        }
    }

    @Test
    fun `should map CustomTextArea add-on`() {
        runBlocking {
            whenever(restClient.fetchGlobalAddOnGroups(any())).thenReturn(
                    WooPayload(
                            result = groupWith(
                                    RemoteAddonDto(
                                            titleFormat = Hide,
                                            descriptionEnabled = 1,
                                            adjustPrice = 1,
                                            priceType = PercentageBased,
                                            type = CustomTextArea,
                                            name = "CustomTextArea",
                                            description = "Description",
                                            required = 0,
                                            position = 3,
                                            min = 0,
                                            max = 0,
                                            price = "5"
                                    )
                            )
                    )
            )

            sut.fetchAllGlobalAddonsGroups(siteModel)

            assertThat(sut.observeAllAddonsForProduct(siteModel, product).first().first()).isEqualTo(
                    Addon.CustomTextArea(
                            name = "CustomTextArea",
                            titleFormat = TitleFormat.Hide,
                            description = "Description",
                            required = false,
                            position = 3,
                            price = Addon.HasAdjustablePrice.Price.Adjusted(
                                    priceType = PriceType.PercentageBased,
                                    value = "5"
                            ),
                            characterLength = null
                    )
            )
        }
    }

    @Test
    fun `should map FileUpload add-on`() {
        runBlocking {
            whenever(restClient.fetchGlobalAddOnGroups(any())).thenReturn(
                    WooPayload(
                            result = groupWith(
                                    RemoteAddonDto(
                                            titleFormat = Heading,
                                            descriptionEnabled = 0,
                                            adjustPrice = 0,
                                            type = FileUpload,
                                            name = "FileUpload",
                                            description = "",
                                            required = 1,
                                            position = 1,
                                            min = 0,
                                            max = 0,
                                            price = "100$"
                                    )
                            )
                    )
            )

            sut.fetchAllGlobalAddonsGroups(siteModel)

            assertThat(sut.observeAllAddonsForProduct(siteModel, product).first().first()).isEqualTo(
                    Addon.FileUpload(
                            name = "FileUpload",
                            titleFormat = TitleFormat.Heading,
                            description = null,
                            required = true,
                            position = 1,
                            price = NotAdjusted
                    )
            )
        }
    }

    @Test
    fun `should map InputMultiplier add-on`() {
        runBlocking {
            whenever(restClient.fetchGlobalAddOnGroups(any())).thenReturn(
                    WooPayload(
                            result = groupWith(
                                    RemoteAddonDto(
                                            titleFormat = Label,
                                            descriptionEnabled = 0,
                                            adjustPrice = 0,
                                            type = InputMultiplier,
                                            name = "InputMultiplier",
                                            description = "",
                                            required = 1,
                                            position = 1,
                                            min = 0,
                                            max = 10,
                                            price = "100$"
                                    )
                            )
                    )
            )

            sut.fetchAllGlobalAddonsGroups(siteModel)

            assertThat(sut.observeAllAddonsForProduct(siteModel, product).first().first()).isEqualTo(
                    Addon.InputMultiplier(
                            name = "InputMultiplier",
                            titleFormat = TitleFormat.Label,
                            description = null,
                            required = true,
                            position = 1,
                            price = NotAdjusted,
                            quantityRange = 0L..10L
                    )
            )
        }
    }

    @Test
    fun `should map CustomPrice add-on`() {
        runBlocking {
            whenever(restClient.fetchGlobalAddOnGroups(any())).thenReturn(
                    WooPayload(
                            result = groupWith(
                                    RemoteAddonDto(
                                            titleFormat = Label,
                                            descriptionEnabled = 1,
                                            adjustPrice = 0,
                                            type = CustomPrice,
                                            name = "CustomPrice",
                                            description = "Description",
                                            required = 1,
                                            position = 0,
                                            min = 0,
                                            max = 10,
                                            price = "100$"
                                    )
                            )
                    )
            )

            sut.fetchAllGlobalAddonsGroups(siteModel)

            assertThat(sut.observeAllAddonsForProduct(siteModel, product).first().first()).isEqualTo(
                    Addon.CustomPrice(
                            name = "CustomPrice",
                            titleFormat = TitleFormat.Label,
                            description = "Description",
                            required = true,
                            position = 0,
                            priceRange = 0L..10L
                    )
            )
        }
    }

    @Test
    fun `should map MultipleChoice add-on`() {
        runBlocking {
            whenever(restClient.fetchGlobalAddOnGroups(any())).thenReturn(
                    WooPayload(
                            result = groupWith(
                                    RemoteAddonDto(
                                            titleFormat = Label,
                                            descriptionEnabled = 1,
                                            adjustPrice = 0,
                                            type = MultipleChoice,
                                            display = RadioButton,
                                            name = "MultipleChoice",
                                            description = "Description",
                                            required = 1,
                                            position = 0,
                                            min = 0,
                                            max = 10,
                                            price = "100$",
                                            options = listOf(
                                                    RemoteAddonDto.RemoteOption(
                                                            priceType = FlatFee,
                                                            label = "Test option",
                                                            price = "10"
                                                    )
                                            )
                                    )
                            )
                    )
            )

            sut.fetchAllGlobalAddonsGroups(siteModel)

            assertThat(sut.observeAllAddonsForProduct(siteModel, product).first().first()).isEqualTo(
                    Addon.MultipleChoice(
                            name = "MultipleChoice",
                            titleFormat = TitleFormat.Label,
                            display = Display.RadioButton,
                            description = "Description",
                            required = true,
                            position = 0,
                            options = listOf(
                                    Addon.HasOptions.Option(
                                            price = Adjusted(
                                                    priceType = PriceType.FlatFee,
                                                    value = "10"
                                            ),
                                            label = "Test option",
                                            image = null
                                    )
                            )
                    )
            )
        }
    }

    @Test
    @Suppress("LongMethod")
    fun `should map Checkbox add-on`() {
        runBlocking {
            whenever(restClient.fetchGlobalAddOnGroups(any())).thenReturn(
                    WooPayload(
                            result = groupWith(
                                    RemoteAddonDto(
                                            titleFormat = Label,
                                            descriptionEnabled = 1,
                                            adjustPrice = 0,
                                            type = Checkbox,
                                            name = "MultipleChoice",
                                            description = "Description",
                                            required = 1,
                                            position = 0,
                                            min = 0,
                                            max = 10,
                                            price = "100$",
                                            options = listOf(
                                                    RemoteAddonDto.RemoteOption(
                                                            priceType = FlatFee,
                                                            label = "First checkbox option",
                                                            price = "10"
                                                    ),
                                                    RemoteAddonDto.RemoteOption(
                                                            priceType = FlatFee,
                                                            label = "Second checkbox option",
                                                            price = "5"
                                                    )
                                            )
                                    )
                            )
                    )
            )

            sut.fetchAllGlobalAddonsGroups(siteModel)

            assertThat(sut.observeAllAddonsForProduct(siteModel, product).first().first()).isEqualTo(
                    Addon.Checkbox(
                            name = "MultipleChoice",
                            titleFormat = TitleFormat.Label,
                            description = "Description",
                            required = true,
                            position = 0,
                            options = listOf(
                                    Addon.HasOptions.Option(
                                            price = Adjusted(
                                                    priceType = PriceType.FlatFee,
                                                    value = "10"
                                            ),
                                            label = "First checkbox option",
                                            image = null
                                    ),
                                    Addon.HasOptions.Option(
                                            price = Adjusted(
                                                    priceType = PriceType.FlatFee,
                                                    value = "5"
                                            ),
                                            label = "Second checkbox option",
                                            image = null
                                    )
                            )
                    )
            )
        }
    }

    @Test
    fun `should map Heading add-on`() {
        runBlocking {
            whenever(restClient.fetchGlobalAddOnGroups(any())).thenReturn(
                    WooPayload(
                            result = groupWith(
                                    RemoteAddonDto(
                                            titleFormat = Hide,
                                            descriptionEnabled = 1,
                                            adjustPrice = 0,
                                            type = RemoteType.Heading,
                                            name = "Heading",
                                            description = "Heading description",
                                            required = 1,
                                            position = 1,
                                            min = 0,
                                            max = 0
                                    )
                            )
                    )
            )

            sut.fetchAllGlobalAddonsGroups(siteModel)

            assertThat(sut.observeAllAddonsForProduct(siteModel, product).first().first()).isEqualTo(
                    Addon.Heading(
                            name = "Heading",
                            titleFormat = TitleFormat.Hide,
                            description = "Heading description",
                            required = true,
                            position = 1
                    )
            )
        }
    }

    @Test
    fun `should not allow to map MultipleChoice Add-on without display property`() {
        runBlocking {
            val testedDto = RemoteAddonDto(
                titleFormat = Label,
                descriptionEnabled = 1,
                adjustPrice = 0,
                type = MultipleChoice,
                name = "sample",
                description = "Description",
                required = 1,
                position = 0,
                min = 0,
                max = 10,
                price = "100$",
                options = listOf(
                    RemoteAddonDto.RemoteOption(
                        priceType = FlatFee,
                        label = "Test option",
                        price = "10"
                    )
                )
            )
            whenever(restClient.fetchGlobalAddOnGroups(any())).thenReturn(
                    WooPayload(
                            result = groupWith(
                                testedDto
                            )
                    )
            )

            sut.fetchAllGlobalAddonsGroups(siteModel)

            assertThat(sut.observeAllAddonsForProduct(siteModel, product).first()).isEmpty()
            verify(logger).e(
                API,
                "Exception while parsing $testedDto: MultipleChoice add-on type has to have `display` defined."
            )
        }
    }

    @Test
    fun `should not allow to map CustomText Add-on without restrictions property`() {
        runBlocking {
            val testedDto = RemoteAddonDto(
                titleFormat = Heading,
                descriptionEnabled = 0,
                restrictionsType = null,
                adjustPrice = 1,
                priceType = FlatFee,
                type = CustomText,
                name = "sample",
                description = "",
                required = 1,
                position = 5,
                min = 2,
                max = 5,
                price = "100$"
            )
            whenever(restClient.fetchGlobalAddOnGroups(any())).thenReturn(
                    WooPayload(
                            result = groupWith(
                                testedDto
                            )
                    )
            )

            sut.fetchAllGlobalAddonsGroups(siteModel)

            assertThat(sut.observeAllAddonsForProduct(siteModel, product).first()).isEmpty()
            verify(logger).e(
                API,
                "Exception while parsing $testedDto: CustomText Add-on has to have restrictions defined."
            )
        }
    }

    @Test
    fun `should provide only applicable add-on for a given product`() {
        runBlocking {
            // given
            val basicAddon = Addon.Heading(
                    name = "test",
                    titleFormat = TitleFormat.Label,
                    description = null,
                    required = false,
                    position = 1
            )

            val productSpecificAddon = basicAddon.copy(name = "Product-specific add-on")
            val allCategoriesAddon = basicAddon.copy(name = "Add-on for all categories")
            val addonForTestProductCategory = basicAddon.copy(name = "Add-on applied to category of the test product")
            val addonNotForTestProductCategory =
                    basicAddon.copy(name = "Add-on NOT applied to category of the test product")

            dao.cacheProductAddons(
                    productRemoteId = product.remoteId,
                    localSiteId = siteModel.localId(),
                    addons = listOf(productSpecificAddon)
            )

            dao.cacheGroups(
                    localSiteId = siteModel.localId(),
                    globalAddonGroups = listOf(
                            GlobalAddonGroup(
                                    name = "Group for all categories",
                                    restrictedCategoriesIds = AllProductsCategories,
                                    addons = listOf(allCategoriesAddon)
                            ),
                            GlobalAddonGroup(
                                    name = "Group for category of the test product",
                                    restrictedCategoriesIds = SpecifiedProductCategories(
                                            listOf(testProductCategoryId)
                                    ),
                                    addons = listOf(addonForTestProductCategory)
                            ),
                            GlobalAddonGroup(
                                    name = "Group for category of NOT the test product",
                                    restrictedCategoriesIds = SpecifiedProductCategories(
                                            listOf(10)
                                    ),
                                    addons = listOf(addonNotForTestProductCategory)
                            )
                    )
            )

            // when
            val addonsForTestProduct = sut.observeAllAddonsForProduct(siteModel, product).first()

            // then
            assertThat(addonsForTestProduct).containsExactlyInAnyOrder(
                    productSpecificAddon, allCategoriesAddon, addonForTestProductCategory
            ).doesNotContain(addonNotForTestProductCategory)
        }
    }

    private fun groupWith(vararg remoteAddons: RemoteAddonDto): List<AddOnGroupDto> {
        return listOf(
                AddOnGroupDto(
                        id = 0,
                        name = "test",
                        categoryIds = null,
                        addons = remoteAddons.toList()
                )
        )
    }

    companion object {
        const val testProductCategoryId = 9L

        val siteModel = SiteModel().apply {
            siteId = 1
        }

        val product = WCProductModel().apply {
            // language=JSON
            categories = """
                        [
                          {
                            "id": $testProductCategoryId
                          }
                        ]
                    """.trimIndent()
        }
    }
}

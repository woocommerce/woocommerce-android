package com.woocommerce.android.ui.products.ai.preview

import com.google.gson.Gson
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ai.AiTone
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GenerateProductWithAITest : BaseUnitTest() {
    private companion object {
        private val TEST_SITE_PARAMETERS = SiteParameters(
            currencyCode = "USD",
            currencySymbol = "$",
            weightUnit = "kg",
            dimensionUnit = "cm",
            gmtOffset = 0f,
            currencyFormattingParameters = null,
        )
        private val TEST_PRODUCT_JSON = """
            {
                "names": ["Product Name", "Product Name 2"],
                "descriptions": ["Product Description", "Product Description 2"],
                "short_descriptions": ["Short Description", "Short Description 2"],
                "categories": ["Category 1", "Category 2"],
                "tags": ["Tag 1", "Tag 2"],
                "price": 100.0,
                "virtual": false,
                "shipping": {
                    "weight": 1.0,
                    "height": 1.0,
                    "length": 1.0,
                    "width": 1.0
                }
            }
        """.trimIndent()
    }

    private val aiRepository: AIRepository = mock {
        onBlocking { identifyISOLanguageCode(any(), any()) } doReturn Result.success("en")
        onBlocking { generateProduct(any(), any(), any(), any(), any(), any(), any(), any()) } doReturn
            Result.success(TEST_PRODUCT_JSON)
    }
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val categoriesRepository: ProductCategoriesRepository = mock {
        onBlocking { fetchProductCategories() }.thenReturn(Result.success(emptyList()))
    }
    private val tagsRepository: ProductTagsRepository = mock {
        onBlocking { fetchProductTags() }.thenReturn(Result.success(emptyList()))
    }
    private val parametersRepository: ParameterRepository = mock {
        on { getParameters() }.thenReturn(TEST_SITE_PARAMETERS)
    }
    private val appPrefs: AppPrefsWrapper = mock {
        on { aiContentGenerationTone }.thenReturn(AiTone.Casual)
    }

    private lateinit var generateProductWithAI: GenerateProductWithAI

    suspend fun setup(setupMocks: suspend () -> Unit = {}) {
        setupMocks()
        generateProductWithAI = GenerateProductWithAI(
            aiRepository = aiRepository,
            analyticsTracker = analyticsTracker,
            categoriesRepository = categoriesRepository,
            tagsRepository = tagsRepository,
            parametersRepository = parametersRepository,
            appPrefs = appPrefs,
            gson = Gson()
        )
    }

    @Test
    fun `when product categories fetch fails, then return error`() = testBlocking {
        setup {
            whenever(categoriesRepository.fetchProductCategories()).thenReturn(Result.failure(Exception()))
        }

        val result = generateProductWithAI.invoke("Product Features")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `when categories are already fetched, then ignore fetching them again`() = testBlocking {
        setup {
            whenever(categoriesRepository.fetchProductCategories()).thenReturn(Result.success(emptyList()))
        }

        generateProductWithAI.invoke("Product Features")
        generateProductWithAI.invoke("Product Features")

        verify(categoriesRepository, times(1)).fetchProductCategories()
    }

    @Test
    fun `given successful product categories fetch, when generating product, then pass existing categories`() =
        testBlocking {
            val existingCategories = listOf(
                ProductCategory(name = "Category 1"),
                ProductCategory(name = "Category 2")
            )
            setup {
                whenever(categoriesRepository.fetchProductCategories()).thenReturn(Result.success(existingCategories))
                whenever(tagsRepository.fetchProductTags()).thenReturn(Result.success(emptyList()))
            }

            generateProductWithAI.invoke("Product Features")

            verify(aiRepository).generateProduct(
                productKeyWords = any(),
                tone = any(),
                weightUnit = any(),
                dimensionUnit = any(),
                currency = any(),
                existingCategories = eq(existingCategories),
                existingTags = any(),
                languageISOCode = any()
            )
        }

    @Test
    fun `when product tags fetch fails, then return error`() = testBlocking {
        setup {
            whenever(categoriesRepository.fetchProductCategories()).thenReturn(Result.success(emptyList()))
            whenever(tagsRepository.fetchProductTags()).thenReturn(Result.failure(Exception()))
        }

        val result = generateProductWithAI.invoke("Product Features")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `when tags are already fetched, then ignore fetching them again`() = testBlocking {
        setup {
            whenever(categoriesRepository.fetchProductCategories()).thenReturn(Result.success(emptyList()))
            whenever(tagsRepository.fetchProductTags()).thenReturn(Result.success(emptyList()))
        }

        generateProductWithAI.invoke("Product Features")
        generateProductWithAI.invoke("Product Features")

        verify(tagsRepository, times(1)).fetchProductTags()
    }

    @Test
    fun `given successful product tags fetch, when generating product, then pass existing tags`() =
        testBlocking {
            val existingTags = listOf(
                ProductTag(name = "Tag 1"),
                ProductTag(name = "Tag 2")
            )
            setup {
                whenever(categoriesRepository.fetchProductCategories()).thenReturn(Result.success(emptyList()))
                whenever(tagsRepository.fetchProductTags()).thenReturn(Result.success(existingTags))
            }

            generateProductWithAI.invoke("Product Features")

            verify(aiRepository).generateProduct(
                productKeyWords = any(),
                tone = any(),
                weightUnit = any(),
                dimensionUnit = any(),
                currency = any(),
                existingCategories = any(),
                existingTags = eq(existingTags),
                languageISOCode = any()
            )
        }

    @Test
    fun `when product parameters fetch fails, then return error`() = testBlocking {
        setup {
            whenever(categoriesRepository.fetchProductCategories()).thenReturn(Result.success(emptyList()))
            whenever(tagsRepository.fetchProductTags()).thenReturn(Result.success(emptyList()))
            whenever(parametersRepository.getParameters()).thenReturn(
                TEST_SITE_PARAMETERS.copy(currencyCode = null)
            )
            whenever(parametersRepository.fetchParameters()).thenReturn(Result.failure(Exception()))
        }

        val result = generateProductWithAI.invoke("Product Features")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `when language is not initialized, then identify language`() = testBlocking {
        setup()

        generateProductWithAI.invoke("Product Features")

        verify(aiRepository).identifyISOLanguageCode(eq("Product Features"), any())
    }

    @Test
    fun `given successful product generation, when invoking generateProductWithAI, then return AIProductModel`() =
        testBlocking {
            setup()

            val result = generateProductWithAI.invoke("Product Features")

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `given failed product generation, when invoking generateProductWithAI, then return error`() = testBlocking {
        setup {
            whenever(aiRepository.generateProduct(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(
                Result.failure(Exception())
            )
        }

        val result = generateProductWithAI.invoke("Product Features")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given non valid product json, when invoking generateProductWithAI, then return error`() = testBlocking {
        setup {
            whenever(aiRepository.generateProduct(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(
                Result.success("invalid json")
            )
        }

        val result = generateProductWithAI.invoke("Product Features")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `when generating product with AI, then read AI tone from app prefs`() = testBlocking {
        val aiTone = AiTone.Formal
        setup {
            whenever(appPrefs.aiContentGenerationTone).thenReturn(aiTone)
        }

        generateProductWithAI.invoke("Product Features")

        verify(aiRepository).generateProduct(
            any(),
            eq(aiTone.name),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        )
    }
}

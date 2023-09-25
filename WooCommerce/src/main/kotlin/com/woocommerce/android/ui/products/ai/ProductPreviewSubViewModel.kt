package com.woocommerce.android.ui.products.ai

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ai.AboutProductSubViewModel.AiTone
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ProductPreviewSubViewModel(
    private val aiRepository: AIRepository,
    private val buildProductPreviewProperties: BuildProductPreviewProperties,
    private val categoriesRepository: ProductCategoriesRepository,
    private val tagsRepository: ProductTagsRepository,
    private val parametersRepository: ParameterRepository,
    override val onDone: (Product) -> Unit,
) : AddProductWithAISubViewModel<Product> {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val siteParameters by lazy { parametersRepository.getParameters() }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asLiveData()

    private lateinit var productName: String
    private lateinit var productKeywords: String
    private lateinit var tone: AiTone

    private var generationJob: Job? = null

    override fun onStart() {
        generationJob = viewModelScope.launch {
            _state.value = State.Loading

            val categories = withContext(Dispatchers.IO) {
                categoriesRepository.getProductCategoriesList().ifEmpty {
                    categoriesRepository.fetchProductCategories()
                    categoriesRepository.getProductCategoriesList()
                }
            }

            val tags = withContext(Dispatchers.IO) {
                tagsRepository.getProductTags().ifEmpty {
                    tagsRepository.fetchProductTags()
                    tagsRepository.getProductTags()
                }
            }

            aiRepository.generateProduct(
                productName = productName,
                productKeyWords = productKeywords,
                tone = tone.slug,
                weightUnit = siteParameters.weightUnit ?: "kg",
                dimensionUnit = siteParameters.dimensionUnit ?: "cm",
                currency = siteParameters.currencyCode ?: "USD",
                existingCategories = categories,
                existingTags = tags,
                languageISOCode = Locale.getDefault().language
            ).fold(
                onSuccess = { product ->
                    _state.value = State.Success(
                        product = product,
                        propertyGroups = buildProductPreviewProperties(product)
                    )
                    onDone(product)
                },
                onFailure = {
                    // TODO
                    it.printStackTrace()
                }
            )
        }
    }

    override fun onStop() {
        generationJob?.cancel()
    }

    fun updateName(name: String) {
        this.productName = name
    }

    fun updateKeywords(keywords: String) {
        this.productKeywords = keywords
    }

    fun updateTone(tone: AiTone) {
        this.tone = tone
    }

    override fun close() {
        viewModelScope.cancel()
    }

    sealed interface State {
        object Loading : State
        data class Success(
            private val product: Product,
            val propertyGroups: List<List<ProductPropertyCard>>
        ) : State {
            val title: String
                get() = product.name
            val description: String
                get() = product.description
        }
    }

    data class ProductPropertyCard(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        val content: String
    )
}

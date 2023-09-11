package com.woocommerce.android.ui.products.variations.attributes

import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class AttributeTermsListHandlerTest: BaseUnitTest() {
    private lateinit var sut: AttributeTermsListHandler
    private lateinit var repository: ProductDetailRepository
}

package com.woocommerce.android.ui.products.models

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class ProductPropertyExtTest : BaseUnitTest() {
    @Test
    fun `given all quantity rules null, then generateQuantityRulesProductProperty returns null`() = testBlocking {
        val resourceProvider: ResourceProvider = mock {}
        val result = generateQuantityRulesProductProperty(QuantityRules(), resourceProvider) {}

        assertNull(result)
    }

    @Test
    fun `given all quantity rules with 0 value, then generateQuantityRulesProductProperty returns a ComplexProperty`() = testBlocking {
        val value = "value"
        val resourceProvider: ResourceProvider = mock {
            on { getString(R.string.no_quantity_rules) } doAnswer { value }
        }

        val result = generateQuantityRulesProductProperty(QuantityRules(0, 0, 0), resourceProvider) {}

        assertTrue(result is ProductProperty.ComplexProperty)

        result as ProductProperty.ComplexProperty

        assertEquals(result.value, value)
        assertEquals(result.title, R.string.product_quantity_rules_title)
    }

    @Test
    fun `given quantity rules with all valid values, then generateQuantityRulesProductProperty returns a PropertyGroup with two properties`() =
        testBlocking {
            val minQuantityPropertyValue = "min"
            val maxQuantityPropertyValue = "max"
            val rules = QuantityRules(2, 12, 2)

            val resourceProvider: ResourceProvider = mock {
                on { getString(R.string.min_quantity) } doAnswer { minQuantityPropertyValue }
                on { getString(R.string.max_quantity) } doAnswer { maxQuantityPropertyValue }
            }

            val result = generateQuantityRulesProductProperty(rules, resourceProvider) {}

            assertTrue(result is ProductProperty)

            result as ProductProperty.PropertyGroup

            assertTrue(result.showTitle)
            assertEquals(result.title, R.string.product_quantity_rules_title)

            val expectedProperties = buildMap<String, String> {
                put(minQuantityPropertyValue, rules.min.toString())
                put(maxQuantityPropertyValue, rules.max.toString())
            }

            assertEquals(result.properties, expectedProperties)
        }
}

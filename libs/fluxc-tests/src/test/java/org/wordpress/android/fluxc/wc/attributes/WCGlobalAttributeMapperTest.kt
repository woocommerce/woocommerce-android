package org.wordpress.android.fluxc.wc.attributes

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeMapper
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.attributeCreateResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.attributesFullListResponse
import org.wordpress.android.fluxc.wc.attributes.WCProductAttributesTestFixtures.stubSite
import kotlin.test.fail

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCGlobalAttributeMapperTest {
    private lateinit var mapperUnderTest: WCGlobalAttributeMapper

    @Before
    fun setUp() {
        mapperUnderTest = WCGlobalAttributeMapper()
    }

    @Test
    fun `mapToAttributeModel should never allow null values`() = test {
        attributeCreateResponse
                ?.let {
                    mapperUnderTest.responseToAttributeModel(it, stubSite)
                }?.let { result ->
                    assertThat(result).isNotNull
                    assertThat(result.remoteId).isNotNull
                    assertThat(result.name).isNotNull
                    assertThat(result.slug).isNotNull
                    assertThat(result.type).isNotNull
                    assertThat(result.orderBy).isNotNull
                    assertThat(result.hasArchives).isNotNull
                } ?: fail("Result shouldn't be null")
    }

    @Test
    fun `mapToAttributeModelList should parse list correctly`() = test {
        mapperUnderTest.responseToAttributeModelList(attributesFullListResponse!!, stubSite)
                .let { result ->
                    assertThat(result).isNotNull
                    assertThat(result.size).isEqualTo(2)
                }
    }

    @Test
    fun `mapToAttributeModel should parse correctly`() = test {
        mapperUnderTest.responseToAttributeModel(attributeCreateResponse!!, stubSite)
                ?.let { result ->
                    assertThat(result).isNotNull
                    assertThat(result.remoteId).isEqualTo(1)
                    assertThat(result.name).isEqualTo("Color")
                    assertThat(result.slug).isEqualTo("pa_color")
                    assertThat(result.type).isEqualTo("select")
                    assertThat(result.orderBy).isEqualTo("menu_order")
                    assertThat(result.hasArchives).isEqualTo(true)
                } ?: fail("Result shouldn't be null")
    }
}

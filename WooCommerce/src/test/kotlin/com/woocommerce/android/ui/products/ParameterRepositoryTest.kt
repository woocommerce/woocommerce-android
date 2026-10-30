package com.woocommerce.android.ui.products

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

class ParameterRepositoryTest {
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val site = SiteModel().apply { id = 1 }

    private lateinit var repository: ParameterRepository

    @Before
    fun setUp() {
        whenever(selectedSite.get()).thenReturn(site)
        repository = ParameterRepository(wooCommerceStore, selectedSite)
    }

    @Test
    fun `given site timezone is a number, when getParameters, then gmt offset is parsed`() = runTest {
        site.timezone = "5.5"

        val result = repository.getParameters()

        assertThat(result.gmtOffset).isEqualTo(5.5f)
    }

    @Test
    fun `given site timezone is not a number, when getParameters, then gmt offset is zero`() = runTest {
        site.timezone = "false"

        val result = repository.getParameters()

        assertThat(result.gmtOffset).isEqualTo(0f)
    }

    @Test
    fun `given site timezone is missing, when getParameters, then gmt offset is zero`() = runTest {
        site.timezone = null

        val result = repository.getParameters()

        assertThat(result.gmtOffset).isEqualTo(0f)
    }
}

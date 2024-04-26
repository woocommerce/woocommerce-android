package com.woocommerce.android.ui.mystore

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.ui.login.LoginRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class MyStoreViewModelTest: BaseUnitTest() {

    private lateinit var sut: MyStoreViewModel
    private val loginRepository: LoginRepository = mock()
    private val navController: NavHostController = mock()

    @Test
    fun `when login changes, site data is updated`() = testBlocking {
        // Given
        var expectedSiteId: String? = null
        var expectedSiteName: String? = null
        val site = SiteModel().apply {
            this.siteId = 1
            this.name = "Test Site"
        }
        whenever(loginRepository.currentSite).thenReturn(flowOf(site))
        createSut()
        sut.viewState.observeForever {
            expectedSiteId = it.currentSiteId
            expectedSiteName = it.currentSiteName
        }

        // Then
        assertThat(expectedSiteId).isEqualTo("1")
        assertThat(expectedSiteName).isEqualTo("Test Site")
    }

    @Test
    fun `when login changes with no site, view state is not updated`() = testBlocking {
        // Given
        whenever(loginRepository.currentSite).thenReturn(flowOf())

        // When
        createSut()

        // Then
        assertThat(sut.viewState.value?.currentSiteId).isNull()
        assertThat(sut.viewState.value?.currentSiteName).isNull()
    }

    private fun createSut() {
        sut = MyStoreViewModel(
            loginRepository,
            navController,
            SavedStateHandle()
        )
    }
}

package com.woocommerce.android.wear.ui.stats

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.wear.ui.login.LoginRepository
import com.woocommerce.android.wear.ui.stats.datasource.FetchStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import java.util.Locale

@ExperimentalCoroutinesApi
class StoreStatsViewModelTest : BaseUnitTest() {

    private lateinit var sut: StoreStatsViewModel
    private val loginRepository: LoginRepository = mock()
    private val fetchStats: FetchStats = mock()

    @Test
    fun `when login changes, site data is updated`() = testBlocking {
        // Given
        var expectedSiteName: String? = null
        val site = SiteModel().apply {
            this.siteId = 1
            this.name = "Test Site"
        }
        whenever(loginRepository.selectedSiteFlow).thenReturn(MutableStateFlow<SiteModel?>(site))
        createSut()
        sut.viewState.observeForever {
            expectedSiteName = it.currentSiteName
        }

        // Then
        assertThat(expectedSiteName).isEqualTo("Test Site")
    }

    @Test
    fun `when login changes with no site, view state is not updated`() = testBlocking {
        // Given
        whenever(loginRepository.selectedSiteFlow).thenReturn(MutableStateFlow<SiteModel?>(null))

        // When
        createSut()

        // Then
        assertThat(sut.viewState.value?.currentSiteName).isNull()
    }

    private fun createSut() {
        sut = StoreStatsViewModel(
            fetchStats,
            Locale.getDefault(),
            loginRepository,
            mock(),
            SavedStateHandle()
        )
    }
}

package com.woocommerce.android.ui.woopos.settings.categories

import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosSettingsCategoriesViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val productDataSource: WooPosProductsDataSource = mock()

    private val featureFlagRepository: FeatureFlagRepository = mock()

    private lateinit var sut: WooPosSettingsCategoriesViewModel

    @Test
    fun `given local catalog not supported, when viewmodel is initialized, then LOCAL_CATALOG is hidden`() = runTest {
        // GIVEN
        whenever(productDataSource.getCurrentSyncStrategy())
            .thenReturn(WooPosProductsDataSource.SyncStrategy.REMOTE)

        // WHEN
        sut = createViewModel()

        // THEN
        assertThat(sut.state.value.categories).doesNotContain(WooPosSettingsCategory.LOCAL_CATALOG)
    }

    @Test
    fun `given local catalog supported, when viewmodel is initialized, then LOCAL_CATALOG is visible`() = runTest {
        // GIVEN
        whenever(productDataSource.getCurrentSyncStrategy())
            .thenReturn(WooPosProductsDataSource.SyncStrategy.LOCAL_CATALOG_FILE)

        // WHEN
        sut = createViewModel()

        // THEN
        assertThat(sut.state.value.categories).contains(WooPosSettingsCategory.LOCAL_CATALOG)
    }

    @Test
    fun `given local catalog not supported, when viewmodel is initialized, then other categories are visible`() =
        runTest {
            // GIVEN
            whenever(productDataSource.getCurrentSyncStrategy())
                .thenReturn(WooPosProductsDataSource.SyncStrategy.REMOTE)

            // WHEN
            sut = createViewModel()

            // THEN
            assertThat(sut.state.value.categories).contains(
                WooPosSettingsCategory.STORE,
                WooPosSettingsCategory.HARDWARE,
                WooPosSettingsCategory.PAYMENTS,
                WooPosSettingsCategory.HELP
            )
        }

    @Test
    fun `given local catalog supported, when viewmodel is initialized, then all categories are visible`() = runTest {
        // GIVEN
        whenever(productDataSource.getCurrentSyncStrategy())
            .thenReturn(WooPosProductsDataSource.SyncStrategy.LOCAL_CATALOG_FILE)

        // WHEN
        sut = createViewModel()

        // THEN
        assertThat(sut.state.value.categories).containsExactlyInAnyOrder(
            WooPosSettingsCategory.STORE,
            WooPosSettingsCategory.HARDWARE,
            WooPosSettingsCategory.PAYMENTS,
            WooPosSettingsCategory.LOCAL_CATALOG,
            WooPosSettingsCategory.HELP
        )
    }

    @Test
    fun `when default state is created, then LOCAL_CATALOG is initially hidden`() {
        // GIVEN & WHEN
        val initialState = WooPosSettingsCategoriesState(
            categories = WooPosSettingsCategory.entries.filter { it != WooPosSettingsCategory.LOCAL_CATALOG }
        )

        // THEN
        assertThat(initialState.categories).doesNotContain(WooPosSettingsCategory.LOCAL_CATALOG)
    }

    @Test
    fun `given Roles off, when categories load, then Security is hidden`() {
        // WHEN
        val state = createViewModel().state.value

        // THEN
        assertThat(state.categories).doesNotContain(WooPosSettingsCategory.SECURITY)
    }

    @Test
    fun `given Roles on, when categories load, then Security is visible`() {
        // GIVEN
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ROLES)).thenReturn(true)

        // WHEN
        val state = createViewModel().state.value

        // THEN
        assertThat(state.categories).contains(WooPosSettingsCategory.SECURITY)
    }

    private fun createViewModel() = WooPosSettingsCategoriesViewModel(
        productsDataSource = productDataSource,
        featureFlagRepository = featureFlagRepository,
    )
}

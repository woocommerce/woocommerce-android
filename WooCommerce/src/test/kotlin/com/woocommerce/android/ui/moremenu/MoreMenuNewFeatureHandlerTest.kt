package com.woocommerce.android.ui.moremenu

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class MoreMenuNewFeatureHandlerTest : BaseUnitTest() {
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    @Test
    fun `given new feature is not seen, when checking state, then returns not empty list`() = testBlocking {
        // GIVEN
        whenever(appPrefsWrapper.observePrefs()).thenReturn(MutableStateFlow(Unit))
        whenever(appPrefsWrapper.isUserSeenNewFeatureOnMoreScreen()).thenReturn(false)
        val moreMenuNewFeatureHandler = MoreMenuNewFeatureHandler(appPrefsWrapper)

        // WHEN && THEN
        assertThat(moreMenuNewFeatureHandler.moreMenuNewFeaturesAvailable.first()).isNotEmpty
    }

    @Test
    fun `given new feature is seen, when checking state, then returns empty list`() = testBlocking {
        // GIVEN
        whenever(appPrefsWrapper.observePrefs()).thenReturn(MutableStateFlow(Unit))
        whenever(appPrefsWrapper.isUserSeenNewFeatureOnMoreScreen()).thenReturn(true)
        val moreMenuNewFeatureHandler = MoreMenuNewFeatureHandler(appPrefsWrapper)

        // WHEN && THEN
        assertThat(moreMenuNewFeatureHandler.moreMenuNewFeaturesAvailable.first()).isEmpty()
    }
}

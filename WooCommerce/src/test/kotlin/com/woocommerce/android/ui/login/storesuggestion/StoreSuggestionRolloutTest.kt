package com.woocommerce.android.ui.login.storesuggestion

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StoreSuggestionRolloutTest : BaseUnitTest() {
    private val buildConfigWrapper: BuildConfigWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private val sut = StoreSuggestionRollout(buildConfigWrapper, appPrefsWrapper)

    @Before
    fun setUp() {
        whenever(buildConfigWrapper.debug).thenReturn(false)
    }

    @Test
    fun `given saved bucket 50, when isInRollout, then returns true`() {
        whenever(appPrefsWrapper.storeSuggestionRolloutBucket).thenReturn(50)

        assertThat(sut.isInRollout()).isTrue()
    }

    @Test
    fun `given saved bucket 51, when isInRollout, then returns false`() {
        whenever(appPrefsWrapper.storeSuggestionRolloutBucket).thenReturn(51)

        assertThat(sut.isInRollout()).isFalse()
    }

    @Test
    fun `given saved bucket, when isInRollout, then no new bucket is saved`() {
        whenever(appPrefsWrapper.storeSuggestionRolloutBucket).thenReturn(10)

        sut.isInRollout()

        verify(appPrefsWrapper, never()).storeSuggestionRolloutBucket = any()
    }

    @Test
    fun `given no saved bucket, when isInRollout, then a bucket in 1 to 100 is saved`() {
        whenever(appPrefsWrapper.storeSuggestionRolloutBucket).thenReturn(null)

        sut.isInRollout()

        verify(appPrefsWrapper).storeSuggestionRolloutBucket = argThat<Int> { this in 1..100 }
    }

    @Test
    fun `given debug build, when isInRollout, then returns true without touching prefs`() {
        whenever(buildConfigWrapper.debug).thenReturn(true)

        assertThat(sut.isInRollout()).isTrue()
        verifyNoInteractions(appPrefsWrapper)
    }
}

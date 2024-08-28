package com.woocommerce.android.ui.appwidgets

import android.content.Context
import android.os.PowerManager
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class IsDeviceBatterySaverActiveTest : BaseUnitTest() {
    private lateinit var sut: IsDeviceBatterySaverActive
    private val appContext: Context = mock()
    private val powerManager: PowerManager = mock()

    @Before
    fun setUp() {
        whenever(appContext.getSystemService(Context.POWER_SERVICE)).thenReturn(powerManager)
        sut = IsDeviceBatterySaverActive(appContext)
    }

    @Test
    fun `returns true when power save mode is active`() = testBlocking {
        whenever(powerManager.isPowerSaveMode).thenReturn(true)

        val result = sut.invoke()

        assertThat(result).isTrue
    }

    @Test
    fun `returns false when power save mode is not active`() = testBlocking {
        whenever(powerManager.isPowerSaveMode).thenReturn(false)

        val result = sut.invoke()

        assertThat(result).isFalse
    }

    @Test
    fun `returns false when power manager is null`() = testBlocking {
        whenever(appContext.getSystemService(Context.POWER_SERVICE)).thenReturn(null)

        val result = sut.invoke()

        assertThat(result).isFalse
    }
}

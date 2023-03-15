package com.woocommerce.android.ui.upgrades

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

internal class UpgradesViewModelTest {
    lateinit var sut: UpgradesViewModel
    lateinit var selectedSite: SelectedSite

    @Before
    fun setup() {
        selectedSite = mock()
        sut = UpgradesViewModel(
            savedState = SavedStateHandle(),
            selectedSite = selectedSite
        )
    }

    @Test
    fun `initial empty test`() {
        assertThat(true).isTrue
    }
}

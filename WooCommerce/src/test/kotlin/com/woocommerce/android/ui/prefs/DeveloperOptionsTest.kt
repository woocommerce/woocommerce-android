package com.woocommerce.android.ui.prefs

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.assertj.core.api.Assertions.assertThat

@OptIn(ExperimentalCoroutinesApi::class)
class DeveloperOptionsTest: BaseUnitTest() {
    private lateinit var viewModel: DeveloperOptionsViewModel

    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val developerOptionsRepository: DeveloperOptionsRepository = mock()

    @Before
    fun setup() {
        viewModel = DeveloperOptionsViewModel(savedStateHandle,developerOptionsRepository)
    }

    @Test
    fun `when dev options screen accessed, then enable simulate reader row is displayed`() {

        val simulatedReaderRow = viewModel.viewStateData.value?.rows?.find {
            it.label == UiString.UiStringRes(R.string.enable_card_reader)
        }

        assertThat(simulatedReaderRow).isNotNull
    }


}

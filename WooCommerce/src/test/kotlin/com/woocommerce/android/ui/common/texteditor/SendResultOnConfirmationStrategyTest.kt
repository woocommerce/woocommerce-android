package com.woocommerce.android.ui.common.texteditor

import com.woocommerce.android.ui.common.texteditor.SimpleTextEditorStrategy.SEND_RESULT_ON_CONFIRMATION
import com.woocommerce.android.ui.common.texteditor.SimpleTextEditorViewModel.SimpleTextEditorResult
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@ExperimentalCoroutinesApi
class SendResultOnConfirmationStrategyTest : SimpleTextEditorViewModelTe() {

    override val defaultArgs = super.defaultArgs.copy(strategy = SEND_RESULT_ON_CONFIRMATION)

    @Test
    fun `given there are changes, when back is pressed, then exit without any result`() {
        setup(defaultArgs.copy(strategy = SEND_RESULT_ON_CONFIRMATION))

        viewModel.viewState.observeForever { }
        viewModel.onTextChanged("new text")
        viewModel.onBackPressed()
        val event = viewModel.event.captureValues().last()

        assertThat(event).isEqualTo(Exit)
    }

    @Test
    fun `given a request code was passed, when done is pressed, then exit with result`() {
        setup(defaultArgs.copy(requestCode = 1, strategy = SEND_RESULT_ON_CONFIRMATION))

        viewModel.viewState.observeForever { }
        viewModel.onTextChanged("new text")
        viewModel.onDonePressed()
        val event = viewModel.event.captureValues().last()

        @Suppress("UNCHECKED_CAST")
        assertThat((event as ExitWithResult<SimpleTextEditorResult>).data.requestCode).isEqualTo(1)
    }
}

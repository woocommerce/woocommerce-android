package com.woocommerce.android.ui.common.texteditor

import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.ui.common.texteditor.SimpleTextEditorViewModel.SimpleTextEditorResult
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SimpleTextEditorViewModelTests : BaseUnitTest() {
    private lateinit var viewModel: SimpleTextEditorViewModel

    private val defaultArgs = SimpleTextEditorFragmentArgs(
        currentText = "text",
        screenTitle = "title",
        hint = "hint"
    )

    fun setup(args: SimpleTextEditorFragmentArgs = defaultArgs) {
        viewModel = SimpleTextEditorViewModel(args.initSavedStateHandle())
    }

    @Test
    fun `when the screen is loaded, then init viewstate correctly`() {
        setup()

        val state = viewModel.viewState.captureValues().last()

        assertThat(state.text).isEqualTo(defaultArgs.currentText)
        assertThat(state.hint).isEqualTo(defaultArgs.hint)
        assertThat(state.hasChanges).isFalse
    }

    @Test
    fun `when the text is edited, then update viewstate`() {
        setup()

        viewModel.onTextChanged("new text")
        val state = viewModel.viewState.captureValues().last()

        assertThat(state.text).isEqualTo("new text")
        assertThat(state.hasChanges).isTrue
    }

    @Test
    fun `given there are no changes, when back is pressed, then exit without any result`() {
        setup()

        viewModel.onBackPressed()
        val event = viewModel.event.captureValues().last()

        assertThat(event).isEqualTo(Exit)
    }

    @Test
    fun `given there are text changes, when back is pressed, then exit with result`() {
        setup()

        viewModel.viewState.observeForever { }
        viewModel.onTextChanged("new text")
        viewModel.onBackPressed()
        val event = viewModel.event.captureValues().last()

        assertThat(event).isEqualTo(ExitWithResult(SimpleTextEditorResult(text = "new text", requestCode = null)))
    }

    @Test
    fun `given a request code was passed, when back is pressed, then exit with result`() {
        setup(defaultArgs.copy(requestCode = 1))

        viewModel.viewState.observeForever { }
        viewModel.onTextChanged("new text")
        viewModel.onBackPressed()
        val event = viewModel.event.captureValues().last()

        @Suppress("UNCHECKED_CAST")
        assertThat((event as ExitWithResult<SimpleTextEditorResult>).data.requestCode).isEqualTo(1)
    }
}

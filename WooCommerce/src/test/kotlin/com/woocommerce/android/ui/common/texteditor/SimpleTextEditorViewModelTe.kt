package com.woocommerce.android.ui.common.texteditor

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
abstract class SimpleTextEditorViewModelTe : BaseUnitTest() {
    protected lateinit var viewModel: SimpleTextEditorViewModel

    protected open val defaultArgs = SimpleTextEditorFragmentArgs(
        currentText = "text",
        screenTitle = "title",
        hint = "hint",
        strategy = SimpleTextEditorStrategy.SEND_RESULT_ON_NAVIGATE_BACK
    )

    protected fun setup(args: SimpleTextEditorFragmentArgs = defaultArgs) {
        viewModel = SimpleTextEditorViewModel(args.toSavedStateHandle())
    }
}

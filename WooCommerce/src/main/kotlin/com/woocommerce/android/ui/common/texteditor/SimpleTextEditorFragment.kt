package com.woocommerce.android.ui.common.texteditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.common.texteditor.SimpleTextEditorViewModel.SimpleTextEditorResult
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint

/**
 * A screen that shows a TextField in full-screen, to allow editing and returning a simple text.
 *
 * To configure the screen, pass an instance of [SimpleTextEditorFragmentArgs] during the navigation.
 *
 * The result is returned using the key:
 *   "{[SIMPLE_TEXT_EDITOR_RESULT]}{[SimpleTextEditorFragmentArgs.requestCode] if present}".
 */
@AndroidEntryPoint
class SimpleTextEditorFragment : BaseFragment(), BackPressListener {
    companion object {
        const val SIMPLE_TEXT_EDITOR_RESULT = "text-editor-result"
    }

    private val viewModel: SimpleTextEditorViewModel by viewModels()
    private val navArgs: SimpleTextEditorFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    SimpleTextEditorScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitWithResult<*> -> {
                    val data = event.data as SimpleTextEditorResult
                    val key = "$SIMPLE_TEXT_EDITOR_RESULT${data.requestCode?.toString().orEmpty()}"
                    navigateBackWithResult(key, data.text)
                }
                is Exit -> findNavController().navigateUp()
            }
        }
    }

    override fun getFragmentTitle(): String = navArgs.screenTitle

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackPressed()
        return false
    }
}

package com.woocommerce.android.ui.common.texteditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.common.texteditor.SimpleTextEditorFragment.Companion.SIMPLE_TEXT_EDITOR_RESULT
import com.woocommerce.android.ui.common.texteditor.SimpleTextEditorStrategy.SEND_RESULT_ON_CONFIRMATION
import com.woocommerce.android.ui.common.texteditor.SimpleTextEditorStrategy.SEND_RESULT_ON_NAVIGATE_BACK
import com.woocommerce.android.ui.common.texteditor.SimpleTextEditorViewModel.SimpleTextEditorResult
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
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
class SimpleTextEditorFragment : BaseFragment(), BackPressListener, MenuProvider {
    companion object {
        const val SIMPLE_TEXT_EDITOR_RESULT = "text-editor-result"
    }

    private val viewModel: SimpleTextEditorViewModel by viewModels()
    private val navArgs: SimpleTextEditorFragmentArgs by navArgs()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            when (navArgs.strategy) {
                SEND_RESULT_ON_NAVIGATE_BACK -> R.drawable.ic_back_24dp
                SEND_RESULT_ON_CONFIRMATION -> R.drawable.ic_gridicons_cross_24dp
            }
        )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
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

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (navArgs.strategy == SEND_RESULT_ON_CONFIRMATION) {
            menuInflater.inflate(R.menu.menu_done, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return if (menuItem.itemId == R.id.menu_done) {
            viewModel.onDonePressed()
            true
        } else {
            false
        }
    }
}

package com.woocommerce.android.ui.dashboard.widgeteditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardWidgetEditorFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    val viewModel: DashboardWidgetEditorViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            DashboardWidgetEditorScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner) { event -> handleEvent(event) }
    }

    private fun handleEvent(event: MultiLiveEvent.Event) {
        when (event) {
            is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
        }
    }
}

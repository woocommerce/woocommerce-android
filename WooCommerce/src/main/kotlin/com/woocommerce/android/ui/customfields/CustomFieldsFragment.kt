package com.woocommerce.android.ui.customfields

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
class CustomFieldsFragment : BaseFragment() {
    private val viewModel: CustomFieldsViewModel by viewModels()

    override val activityAppBarStatus = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return composeView {
            CustomFieldsScreen(
                viewModel = viewModel
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleEvents()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> {
                    findNavController().navigateUp()
                }
            }
        }
    }
}

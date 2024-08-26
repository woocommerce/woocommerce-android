package com.woocommerce.android.ui.customfields.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomFieldsEditorFragment : BaseFragment() {
    private val viewModel: CustomFieldsEditorViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            CustomFieldsEditorScreen(viewModel)
        }
    }
}
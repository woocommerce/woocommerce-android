package com.woocommerce.android.ui.reviews.reviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.push.NotificationMessageHandler
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReviewListFragment : BaseFragment() {
    private val viewModel: ReviewListViewModel by viewModels()

    @Inject lateinit var productImageMap: ProductImageMap
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var notificationMessageHandler: NotificationMessageHandler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ReviewListScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                    is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                }
            }
        )
    }
}

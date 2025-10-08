package com.woocommerce.android.ui.bookings.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.fragment.app.viewModels
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.bookings.list.BookingListFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookingFilterListFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: BookingFilterListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return composeView {
            val options by viewModel.options.observeAsState()
            BookingFilterListScreen(
                state = BookingFilterListViewModel.BookingFilterListUiState(
                    items = options.orEmpty(),
                    onClose = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                    onShowBookings = {
                        // TODO Pass new filter payload instead of `true`
                        navigateBackWithResult(BookingListFragment.BOOKINGS_FILTER_RESULT, true)
                    }
                )
            )
        }
    }
}

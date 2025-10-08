package com.woocommerce.android.ui.bookings.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BookingListFragment : TopLevelFragment() {
    companion object {
        const val BOOKINGS_FILTER_RESULT = "bookings_filter_result"
    }

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: BookingListViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override fun getFragmentTitle() = getString(R.string.bookings_tab_title)
    override fun shouldExpandToolbar(): Boolean = false
    override fun scrollToTop() {
        return
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            BookingListScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleEvents()
        handleBottomNavigationVisibility()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is BookingListViewModel.NavigateToFilters -> findNavController().navigate(
                    BookingListFragmentDirections.actionBookingListFragmentToBookingFilterList()
                )

                is BookingListViewModel.NavigateToBookingDetails -> findNavController().navigate(
                    BookingListFragmentDirections.actionBookingListFragmentToBookingDetailsFragment(event.bookingId)
                )

                is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
            }
        }

        // Observe result coming back from filter screen
        handleResult<Boolean>(key = BOOKINGS_FILTER_RESULT) { viewModel.onFiltersApplied() }
    }

    private fun handleBottomNavigationVisibility() {
        viewModel.bottomNavigationVisible.observe(viewLifecycleOwner) { isVisible ->
            if (!isVisible) {
                (activity as? MainActivity)?.hideBottomNav()
            } else {
                (activity as? MainActivity)?.showBottomNav()
            }
        }
    }
}

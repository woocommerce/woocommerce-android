package com.woocommerce.android.ui.bookings.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.BookingsArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentBookingListBinding
import com.woocommerce.android.extensions.isTwoPanesShouldBeUsed
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.bookings.BookingsCommunicationViewModel
import com.woocommerce.android.ui.bookings.details.BookingDetailsFragment
import com.woocommerce.android.ui.bookings.details.BookingDetailsFragmentArgs
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.TabletLayoutSetupHelper
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TWO_PANES_WERE_SHOWN_BEFORE_CONFIG_CHANGE_KEY = "non_root_navigation_in_detail_pane"

@AndroidEntryPoint
class BookingListFragment : TopLevelFragment(), TabletLayoutSetupHelper.Screen {
    @Inject
    lateinit var tabletLayoutSetupHelper: TabletLayoutSetupHelper

    private var _binding: FragmentBookingListBinding? = null
    private val binding get() = _binding!!

    override val twoPaneLayoutGuideline
        get() = binding.twoPaneLayoutGuideline
    override val listPaneContainer: View
        get() = binding.listPaneContainer
    override val detailPaneContainer: View
        get() = binding.detailNavContainer
    override var twoPanesWereShownBeforeConfigChange: Boolean = false
    override val automaticallyAdjustLayoutAfterConfigChange: Boolean = false
    override val listFragment: Fragment
        get() = this
    override val navigation
        get() = TabletLayoutSetupHelper.Screen.Navigation(
            detailsNavGraphId = R.navigation.nav_graph_bookings_details,
            detailsInitialBundle = BookingDetailsFragmentArgs(
                mode = BookingDetailsFragment.Mode.Empty
            ).toBundle()
        )

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override val shouldShowBottomNavigation: Boolean
        get() = navArgs.showBottomNavigation

    private val viewModel: BookingListViewModel by viewModels()
    private val navArgs: BookingsArgs by navArgs()
    private val bookingsCommunicationViewModel: BookingsCommunicationViewModel by activityViewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override fun getFragmentTitle() = getString(R.string.bookings_tab_title)
    override fun shouldExpandToolbar(): Boolean = false
    override fun scrollToTop() {
        return
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookingListBinding.inflate(inflater, container, false)
        binding.bookingListCompose.setContent {
            WooTheme {
                BookingListScreen(viewModel)
            }
        }
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        twoPanesWereShownBeforeConfigChange = savedInstanceState?.getBoolean(
            TWO_PANES_WERE_SHOWN_BEFORE_CONFIG_CHANGE_KEY,
            false
        ) ?: false
        tabletLayoutSetupHelper.onRootFragmentCreated(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleEvents()
        handleBottomNavigationVisibility()
        handleTwoPaneToOnePaneConversionIfNeeded()
    }

    override fun onStart() {
        super.onStart()
        viewModel.trackBookingListView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is BookingListViewModel.NavigateToFilters -> findNavController().navigate(
                    BookingListFragmentDirections.actionBookingListFragmentToBookingFilterList()
                )
                is BookingListViewModel.NavigateToBookingDetails -> {
                    tabletLayoutSetupHelper.openItemDetails(
                        tabletNavigateTo = {
                            R.id.nav_graph_bookings_details to BookingDetailsFragmentArgs(
                                mode = BookingDetailsFragment.Mode.ShowBooking(event.bookingId)
                            ).toBundle()
                        },
                        navigateWithPhoneNavigation = {
                            findNavController().navigate(
                                BookingListFragmentDirections
                                    .actionBookingListFragmentToBookingDetailsFragment(
                                        BookingDetailsFragment.Mode.ShowBooking(event.bookingId)
                                    )
                            )
                        }
                    )
                }
                is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
            }
        }

        bookingsCommunicationViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is BookingsCommunicationViewModel.CommunicationEvent.BookingSelected -> {
                    tabletLayoutSetupHelper.openItemDetails(
                        tabletNavigateTo = {
                            R.id.nav_graph_bookings_details to BookingDetailsFragmentArgs(
                                mode = BookingDetailsFragment.Mode.ShowBooking(event.bookingId)
                            ).toBundle()
                        },
                        navigateWithPhoneNavigation = { /* No-op, won't happen */ }
                    )
                }

                else -> event.isHandled = false
            }
        }
    }

    private fun handleBottomNavigationVisibility() {
        viewModel.bottomNavigationVisible.observe(viewLifecycleOwner) { isVisible ->
            if (!navArgs.showBottomNavigation || !isVisible) {
                (activity as? MainActivity)?.hideBottomNav()
            } else {
                (activity as? MainActivity)?.showBottomNav()
            }
        }
    }

    private fun handleTwoPaneToOnePaneConversionIfNeeded() {
        val isNowSinglePane = !requireContext().isTwoPanesShouldBeUsed
        if (isNowSinglePane && twoPanesWereShownBeforeConfigChange) {
            // Reset the flag to avoid re-triggering on future recreations
            twoPanesWereShownBeforeConfigChange = false

            // Show the list pane only in this fragment
            tabletLayoutSetupHelper.displayListPaneOnly(this)

            // Read the current state of the details NavHost to get the booking being displayed (if any)
            val navHost = childFragmentManager.findFragmentById(R.id.detail_nav_container) as? NavHostFragment
            val argsBundle = navHost?.navController?.currentBackStackEntry?.arguments
            if (argsBundle != null) {
                val args = BookingDetailsFragmentArgs.fromBundle(argsBundle)
                when (val mode = args.mode) {
                    is BookingDetailsFragment.Mode.ShowBooking -> {
                        // Re-open details with phone navigation to ensure full-screen and proper back behavior
                        findNavController().navigate(
                            BookingListFragmentDirections
                                .actionBookingListFragmentToBookingDetailsFragment(
                                    BookingDetailsFragment.Mode.ShowBooking(mode.bookingId)
                                )
                        )
                    }

                    is BookingDetailsFragment.Mode.Empty -> {
                        // No booking was selected; nothing to re-open
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(
            TWO_PANES_WERE_SHOWN_BEFORE_CONFIG_CHANGE_KEY,
            _binding?.detailNavContainer?.isVisible == true && _binding?.listPaneContainer?.isVisible == true
        )
        super.onSaveInstanceState(outState)
    }
}

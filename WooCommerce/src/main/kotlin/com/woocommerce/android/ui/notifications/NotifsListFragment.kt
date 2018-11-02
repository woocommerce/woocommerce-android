package com.woocommerce.android.ui.notifications

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.OrderListFragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_notifs_list.*
import kotlinx.android.synthetic.main.fragment_notifs_list.view.*
import javax.inject.Inject

class NotifsListFragment : TopLevelFragment(), NotifsListContract.View {
    companion object {
        val TAG: String = NotifsListFragment::class.java.simpleName
        const val STATE_KEY_LIST = "list-state"
        const val STATE_KEY_REFRESH_PENDING = "is-refresh-pending"

        fun newInstance() = NotifsListFragment()
    }

    @Inject lateinit var presenter: NotifsListContract.Presenter
    @Inject lateinit var notifsAdapter: NotifsListAdapter
    @Inject lateinit var  uiMessageResolver: UIMessageResolver
    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var dividerDecoration: DividerItemDecoration

    override var isActive: Boolean = false
        get() = childFragmentManager.backStackEntryCount == 0 && !isHidden

    override var isRefreshPending = true
    private var listState: Parcelable? = null // Save the state of the recycler view

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { bundle ->
            listState = bundle.getParcelable(OrderListFragment.STATE_KEY_LIST)
            isRefreshPending = bundle.getBoolean(OrderListFragment.STATE_KEY_REFRESH_PENDING, false)
        }
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateFragmentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifs_list, container, false)
        with(view) {
            notifsRefreshLayout?.apply {
                activity?.let { activity ->
                    setColorSchemeColors(
                            ContextCompat.getColor(activity, R.color.colorPrimary),
                            ContextCompat.getColor(activity, R.color.colorAccent),
                            ContextCompat.getColor(activity, R.color.colorPrimaryDark)
                    )
                }
                // Set the scrolling view in the custom SwipeRefreshLayout
                scrollUpChild = notifsList
                setOnRefreshListener {
                    // TODO track analytics

                    notifsRefreshLayout.isRefreshing = false

                    if (!isRefreshPending) {
                        isRefreshPending = true
                        presenter.loadNotifs(forceRefresh = true)
                    }
                }
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Set the divider decoration for the list
        dividerDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)

        notifsList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(false)
            addItemDecoration(dividerDecoration)
            adapter = notifsAdapter
        }

        presenter.takeView(this)

        if (isActive && !deferInit) {
            presenter.loadNotifs(forceRefresh = this.isRefreshPending)
        }

        listState?.let {
            notifsList.layoutManager.onRestoreInstanceState(listState)
            listState = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val listState = notifsList.layoutManager.onSaveInstanceState()

        outState.putParcelable(STATE_KEY_LIST, listState)
        outState.putBoolean(STATE_KEY_REFRESH_PENDING, isRefreshPending)
        super.onSaveInstanceState(outState)
    }

    override fun onBackStackChanged() {
        super.onBackStackChanged()

        // If this fragment is now visible and we've deferred loading orders due to it not
        // being visible - go ahead and load the orders.
        if (isActive) {
            presenter.loadNotifs(forceRefresh = this.isRefreshPending)
        }
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun showNotifications(notifs: List<WCNotificationModel>, isFreshData: Boolean) {
        if (!notifsAdapter.isSameList(notifs)) {
            notifsList?.let { _ ->
                if (isFreshData) {
                    notifsList.scrollToPosition(0)
                }
                notifsAdapter.setNotifications(notifs)
            }
        }
        if (isFreshData) {
            isRefreshPending = false
        }
    }

    override fun getFragmentTitle() = getString(R.string.notifications)

    override fun refreshFragmentState() {
        // todo reset any scrolling
    }

    override fun setLoadingMoreIndicator(active: Boolean) {
        notifsLoadMoreProgress.visibility = if (active) View.VISIBLE else View.GONE
    }

    override fun showSkeleton(show: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

package com.woocommerce.android.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemReselectedListener
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.woocommerce.android.R
import com.woocommerce.android.extensions.active
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.main.BottomNavigationPosition.MY_STORE
import org.wordpress.android.util.DisplayUtils
import kotlin.math.min

class MainBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BottomNavigationView(context, attrs),
        OnNavigationItemSelectedListener, OnNavigationItemReselectedListener {
    private lateinit var navAdapter: NavAdapter
    private lateinit var fragmentManager: FragmentManager
    private lateinit var listener: MainNavigationListener
    private lateinit var ordersBadge: BadgeDrawable
    private lateinit var reviewsBadge: BadgeDrawable

    companion object {
        private var previousNavPos: BottomNavigationPosition? = null
    }

    interface MainNavigationListener {
        fun onNavItemSelected(navPos: BottomNavigationPosition)
        fun onNavItemReselected(navPos: BottomNavigationPosition)
    }

    var currentPosition: BottomNavigationPosition
        get() = findNavigationPositionById(selectedItemId)
        set(navPos) = updateCurrentPosition(navPos)

    fun init(fm: FragmentManager, listener: MainNavigationListener) {
        this.fragmentManager = fm
        this.listener = listener

        navAdapter = NavAdapter()
        addTopDivider()
        createBadges()

        assignNavigationListeners(true)

        // Default to the dashboard position
        active(MY_STORE.position)
    }

    /**
     * HACK alert! The bottom nav's presenter stores the badges in its saved state and recreates them
     * in onRestoreInstanceState, which should be fine but instead it ends up creating duplicates
     * of our badges. To work around this we remove the badges before state is saved and recreate
     * them ourselves when state is restored.
     */
    override fun onSaveInstanceState(): Parcelable? {
        removeBadge(R.id.orders)
        removeBadge(R.id.reviews)
        return super.onSaveInstanceState()
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        createBadges()
    }

    private fun createBadges() {
        ordersBadge = getOrCreateBadge(R.id.orders)
        ordersBadge.isVisible = false
        ordersBadge.backgroundColor = ContextCompat.getColor(context, R.color.color_primary)
        ordersBadge.maxCharacterCount = 3 // this includes the plus sign

        reviewsBadge = getOrCreateBadge(R.id.reviews)
        reviewsBadge.isVisible = false
        reviewsBadge.backgroundColor = ContextCompat.getColor(context, R.color.color_primary)
    }

    /**
     * When we changed the background to white, the top shadow provided by BottomNavigationView wasn't
     * dark enough to provide enough separation between the bar and the content above it. For this
     * reason we add a darker top divider here.
     */
    private fun addTopDivider() {
        val divider = View(context)
        val dividerColor = ContextCompat.getColor(context, R.color.divider_color)
        divider.setBackgroundColor(dividerColor)

        val dividerHeight = resources.getDimensionPixelSize(R.dimen.minor_10)
        val dividerParams = LayoutParams(LayoutParams.MATCH_PARENT, dividerHeight)
        divider.layoutParams = dividerParams

        addView(divider)
    }

    /**
     * We want to override the bottom nav's default behavior of only showing labels for the active tab when
     * more than three tabs are showing, but we only want to do this if we know it won't cause any of the
     * tabs to wrap to more than one line.
     */
    @SuppressLint("PrivateResource")
    private fun detectLabelVisibilityMode() {
        // default to showing labels for all tabs
        labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED

        var numVisibleItems = 0
        for (index in 0 until menu.size()) {
            if (menu.getItem(index).isVisible) {
                numVisibleItems++
            }
        }

        // determine the width of a navbar item
        val displayWidth = DisplayUtils.getDisplayPixelWidth(context)
        val itemMargin = resources.getDimensionPixelSize(R.dimen.design_bottom_navigation_margin)
        val itemMaxWidth = resources.getDimensionPixelSize(R.dimen.design_bottom_navigation_item_max_width)
        val itemWidth = min(itemMaxWidth, (displayWidth / numVisibleItems) - (itemMargin * 3))

        // create a paint object whose text size matches the bottom navigation active text size - note that
        // we have to use the active size since it's 2sp larger than inactive
        val textPaint = Paint().also {
            it.textSize = resources.getDimension(R.dimen.design_bottom_navigation_active_text_size)
        }

        // iterate through the menu items and determine whether they can all fit their space - if any of them
        // can't, we revert to LABEL_VISIBILITY_AUTO
        val bounds = Rect()
        for (index in 0 until menu.size()) {
            val title = menu.getItem(index).title.toString()
            textPaint.getTextBounds(title, 0, title.length, bounds)
            if (bounds.width() > itemWidth) {
                labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_AUTO
                break
            }
        }
    }

    fun getFragment(navPos: BottomNavigationPosition): TopLevelFragment = navAdapter.getFragment(navPos)

    fun updatePositionAndDeferInit(navPos: BottomNavigationPosition) {
        updateCurrentPosition(navPos, true)
    }

    /**
     * For use when restoring the navigation bar after the host activity
     * state has been restored.
     */
    fun restoreSelectedItemState(itemId: Int) {
        assignNavigationListeners(false)
        selectedItemId = itemId
        assignNavigationListeners(true)
    }

    fun showReviewsBadge(show: Boolean) {
        reviewsBadge.isVisible = show
    }

    fun setOrderBadgeCount(count: Int) {
        if (count > 0) {
            ordersBadge.number = count
            ordersBadge.isVisible = true
        } else {
            ordersBadge.isVisible = false
        }
    }

    fun clearOrderBadgeCount() {
        ordersBadge.clearNumber()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navPos = findNavigationPositionById(item.itemId)
        currentPosition = navPos

        listener.onNavItemSelected(navPos)
        return true
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        val navPos = findNavigationPositionById(item.itemId)
        listener.onNavItemReselected(navPos)
    }

    /**
     * Replaces the fragment in [MY_STORE] based on whether the revenue stats is available
     */
    fun replaceStatsFragment() {
        val fragment = fragmentManager.findFragment(currentPosition)
        val tag = currentPosition.getTag()

        // replace the fragment
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment, tag)
                .show(fragment)
                .commitAllowingStateLoss()

        // update the correct fragment in the navigation adapter
        navAdapter.replaceFragment(currentPosition, fragment)
    }

    private fun updateCurrentPosition(navPos: BottomNavigationPosition, deferInit: Boolean = false) {
        assignNavigationListeners(false)
        try {
            selectedItemId = navPos.id
        } finally {
            assignNavigationListeners(true)
        }

        val fragment = navAdapter.getFragment(navPos)
        fragment.deferInit = deferInit

        // hide previous fragment if it exists
        val fragmentTransaction = fragmentManager.beginTransaction()
        previousNavPos?.let {
            val previousFragment = navAdapter.getFragment(it)
            fragmentTransaction.hide(previousFragment)
        }

        // add the fragment if it hasn't been added yet
        val tag = navPos.getTag()
        if (fragmentManager.findFragmentByTag(tag) == null) {
            fragmentTransaction.add(R.id.container, fragment, tag)
        }

        // show the new fragment
        fragmentTransaction.show(fragment)
        fragmentTransaction.commitAllowingStateLoss()

        previousNavPos = navPos
    }

    private fun assignNavigationListeners(assign: Boolean) {
        setOnNavigationItemSelectedListener(if (assign) this else null)
        setOnNavigationItemReselectedListener(if (assign) this else null)
    }

    /**
     * Extension function for retrieving an existing fragment from the [FragmentManager]
     * if one exists, if not, create a new instance of the requested fragment.
     */
    private fun FragmentManager.findFragment(position: BottomNavigationPosition): TopLevelFragment {
        return (findFragmentByTag(position.getTag()) ?: position.createFragment()) as TopLevelFragment
    }

    // region Private Classes
    private inner class NavAdapter {
        private val fragments = SparseArray<TopLevelFragment>(BottomNavigationPosition.values().size)

        internal fun getFragment(navPos: BottomNavigationPosition): TopLevelFragment {
            fragments[navPos.position]?.let {
                return it
            }

            val fragment = fragmentManager.findFragment(navPos)
            fragments.put(navPos.position, fragment)
            return fragment
        }

        internal fun replaceFragment(navPos: BottomNavigationPosition, fragment: TopLevelFragment) =
                fragments.put(navPos.position, fragment)
    }
    // endregion
}

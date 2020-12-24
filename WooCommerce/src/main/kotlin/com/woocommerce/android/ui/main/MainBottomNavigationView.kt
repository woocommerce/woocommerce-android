package com.woocommerce.android.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.NavOptions.Builder
import androidx.navigation.ui.NavigationUI
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemReselectedListener
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.woocommerce.android.R
import com.woocommerce.android.R.anim
import org.wordpress.android.util.DisplayUtils
import kotlin.math.min

class MainBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BottomNavigationView(context, attrs),
    OnNavigationItemSelectedListener, OnNavigationItemReselectedListener {
    private lateinit var navController: NavController
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

    val currentPosition: BottomNavigationPosition
        get() = findNavigationPositionById(selectedItemId)

    fun init(navController: NavController, listener: MainNavigationListener) {
        this.listener = listener
        this.navController = navController

        addTopDivider()
        createBadges()

        assignNavigationListeners(true)
        navController.addOnDestinationChangedListener(
            object : OnDestinationChangedListener {
                override fun onDestinationChanged(
                    controller: NavController,
                    destination: NavDestination, arguments: Bundle?
                ) {
                    val menu = menu
                    var h = 0
                    val size = menu.size()
                    while (h < size) {
                        val item = menu.getItem(h)
                        if (destination.id ==  item.itemId) {
                            item.isChecked = true
                        }
                        h++
                    }
                }
            })
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
        listener.onNavItemSelected(navPos)
        val builder = Builder()
            .setLaunchSingleTop(true)
            .setEnterAnim(anim.nav_default_enter_anim)
            .setExitAnim(anim.nav_default_exit_anim)
            .setPopEnterAnim(anim.nav_default_pop_enter_anim)
            .setPopExitAnim(anim.nav_default_pop_exit_anim)

        val options: NavOptions = builder.build()
        return try {
            navController.navigate(item.itemId, null, options)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
        return true
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        val navPos = findNavigationPositionById(item.itemId)
        listener.onNavItemReselected(navPos)
    }

    private fun assignNavigationListeners(assign: Boolean) {
        setOnNavigationItemSelectedListener(if (assign) this else null)
        setOnNavigationItemReselectedListener(if (assign) this else null)
    }
}

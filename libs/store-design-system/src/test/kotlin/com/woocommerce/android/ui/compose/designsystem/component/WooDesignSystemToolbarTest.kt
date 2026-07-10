package com.woocommerce.android.ui.compose.designsystem.component

import android.graphics.Rect
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SearchView
import androidx.core.view.children
import androidx.test.core.app.ApplicationProvider
import com.woocommerce.android.ui.compose.designsystem.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class WooDesignSystemToolbarTest {
    @Test
    fun `when creating design system toolbar, then chrome is applied automatically`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())

        assertThat(toolbar.isTitleCentered).isTrue()
        assertThat(toolbar.minimumHeight)
            .isEqualTo(toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_height))
        assertThat(toolbar.background).isNotNull()
    }

    @Test
    fun `when creating design system toolbar, then figma edge insets are applied`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        val edgeInset = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_edge_padding)

        assertThat(toolbar.contentInsetStart).isEqualTo(edgeInset)
        assertThat(toolbar.contentInsetEnd).isEqualTo(edgeInset)
        assertThat(toolbar.contentInsetStartWithNavigation).isEqualTo(edgeInset)
        assertThat(toolbar.contentInsetEndWithActions).isEqualTo(edgeInset)
    }

    @Test
    fun `given navigation and actions, when laid out, then controls honor edge insets`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        val edgeInset = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_edge_padding)
        toolbar.navigationIcon = AppCompatResources.getDrawable(
            toolbar.context,
            R.drawable.woo_ds_ic_regular_angle_left_24dp,
        )
        toolbar.navigationContentDescription = "Back"
        toolbar.addIconAction()

        toolbar.layoutToolbar()
        val navigationButton = toolbar.navigationButton("Back")
        val actionMenuView = toolbar.actionMenuView()

        assertThat(navigationButton.left).isEqualTo(edgeInset)
        assertThat(actionMenuView.right).isEqualTo(toolbar.width - edgeInset)
    }

    @Test
    fun `when title is set after creation, then title remains visible`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())

        toolbar.title = "Products"
        toolbar.layoutToolbar()

        val titleView = toolbar.titleTextView("Products")
        assertThat(titleView.width).isGreaterThan(0)
        assertThat(titleView.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun `given toolbar xml attributes, when inflated, then title navigation and menu are applied`() {
        val toolbar = LayoutInflater.from(toolbarContext())
            .inflate(R.layout.woo_design_system_toolbar_test, null) as WooDesignSystemToolbar

        toolbar.layoutToolbar()
        val action = toolbar.actionChild(R.id.woo_ds_toolbar_test_action)

        assertThat(toolbar.title).isEqualTo("XML Products")
        assertThat(toolbar.navigationIcon).isNotNull()
        assertThat(toolbar.navigationContentDescription).isEqualTo("Back")
        assertThat(action.getTag(R.id.woo_ds_toolbar_action_view)).isEqualTo(true)
    }

    @Test
    fun `given navigation is set normally, when clicked, then navigation remains clickable`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        var navigationClicked = false
        toolbar.navigationIcon = AppCompatResources.getDrawable(
            toolbar.context,
            R.drawable.woo_ds_ic_regular_angle_left_24dp,
        )
        toolbar.navigationContentDescription = "Back"
        toolbar.setNavigationOnClickListener {
            navigationClicked = true
        }

        toolbar.layoutToolbar()
        val navigationButton = toolbar.navigationButton("Back")
        val touchTarget = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_icon_touch_target)
        val iconPadding = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_icon_padding)
        navigationButton.performClick()

        assertThat(navigationButton.measuredWidth).isEqualTo(touchTarget)
        assertThat(navigationButton.measuredHeight).isEqualTo(touchTarget)
        assertThat(navigationButton.paddingLeft).isEqualTo(iconPadding)
        assertThat(navigationButton.paddingTop).isEqualTo(iconPadding)
        assertThat(navigationButton.background).isNotNull()
        assertThat(navigationButton.scaleType).isEqualTo(ImageView.ScaleType.FIT_CENTER)
        assertThat(navigationClicked).isTrue()
    }

    @Test
    fun `given inflated icon item, when shown in toolbar, then item is outlined by default`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        toolbar.addIconAction()

        toolbar.layoutToolbar()
        val action = toolbar.actionChild(ACTION_ID) as TextView
        val touchTarget = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_icon_touch_target)
        val backgroundPadding = Rect().also { action.background.getPadding(it) }

        assertThat(action.measuredWidth).isEqualTo(touchTarget)
        assertThat(action.measuredHeight).isEqualTo(touchTarget)
        assertThat(action.background).isNotNull()
        assertThat(action.getTag(R.id.woo_ds_toolbar_action_view)).isEqualTo(true)
        assertThat(action.gravity).isEqualTo(android.view.Gravity.CENTER)
        assertThat(backgroundPadding).isEqualTo(Rect())
        assertThat(action.paddingTop).isEqualTo(action.paddingBottom)
        assertThat(action.compoundDrawables.filterNotNull()).isEmpty()
    }

    @Test
    fun `given outlined icon item, when laid out again, then decoration background is reused`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        toolbar.addIconAction()

        toolbar.layoutToolbar()
        val firstAction = toolbar.actionChild(ACTION_ID)
        val firstBackground = firstAction.background

        toolbar.layoutToolbar()
        val secondAction = toolbar.actionChild(ACTION_ID)

        assertThat(secondAction).isSameAs(firstAction)
        assertThat(secondAction.background).isSameAs(firstBackground)
    }

    @Test
    fun `given outlined icon item, when icon is removed, then default action styling is restored`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        val item = toolbar.addIconAction()

        toolbar.layoutToolbar()
        val outlinedAction = toolbar.actionChild(ACTION_ID)
        val outlinedBackground = outlinedAction.background

        item.icon = null
        toolbar.layoutToolbar()
        val textAction = toolbar.actionChild(ACTION_ID) as TextView

        assertThat(textAction).isSameAs(outlinedAction)
        assertThat(textAction.text).isEqualTo("Open")
        assertThat(textAction.background).isNotNull().isNotSameAs(outlinedBackground)
        assertThat(textAction.layoutParams.width).isEqualTo(ViewGroup.LayoutParams.WRAP_CONTENT)
        assertThat(textAction.getTag(R.id.woo_ds_toolbar_action_view)).isNull()
        assertThat(textAction.getTag(R.id.woo_ds_toolbar_action_icon)).isNull()
        assertThat(textAction.compoundDrawables.filterNotNull()).isEmpty()
    }

    @Test
    fun `given text item, when shown in toolbar, then item is not outlined`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        toolbar.menu.add(0, TEXT_ACTION_ID, 0, "Done").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        toolbar.layoutToolbar()
        val action = toolbar.actionChild(TEXT_ACTION_ID) as TextView

        assertThat(action.text).isEqualTo("Done")
        assertThat(action.getTag(R.id.woo_ds_toolbar_action_view)).isNull()
        assertThat(action.compoundDrawablesRelative.filterNotNull()).isEmpty()
    }

    @Test
    @Config(qualifiers = "w600dp")
    fun `given icon with text item on wide screen, when shown in toolbar, then item keeps natural width`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        toolbar.addIconAction(
            showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_WITH_TEXT,
            title = "Open product",
        )

        toolbar.layoutToolbar(widthDp = 600)
        val action = toolbar.actionChild(ACTION_ID) as TextView
        val touchTarget = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_icon_touch_target)

        assertThat(action.text).isEqualTo("Open product")
        assertThat(action.measuredWidth).isGreaterThan(touchTarget)
        assertThat(action.getTag(R.id.woo_ds_toolbar_action_view)).isNull()
        assertThat(action.compoundDrawables.filterNotNull()).hasSize(1)
    }

    @Test
    fun `given disabled icon item, when shown in toolbar, then rendered child remains disabled`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        toolbar.addIconAction(enabled = false)

        toolbar.layoutToolbar()
        val action = toolbar.actionChild(ACTION_ID)

        assertThat(action.isEnabled).isFalse()
        assertThat(action.getTag(R.id.woo_ds_toolbar_action_view)).isEqualTo(true)
    }

    @Test
    fun `given never action item, when menu is rendered, then item is not decorated as toolbar action`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        toolbar.addIconAction(showAsAction = MenuItem.SHOW_AS_ACTION_NEVER)

        toolbar.layoutToolbar()

        assertThat(toolbar.actionChildOrNull(ACTION_ID)).isNull()
    }

    @Test
    fun `given collapsed search item, when shown in toolbar, then collapsed trigger is outlined`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        val searchView = SearchView(toolbar.context)
        val searchItem = toolbar.menu.add(0, SEARCH_ACTION_ID, 0, "Search").apply {
            icon = AppCompatResources.getDrawable(
                toolbar.context,
                R.drawable.woo_ds_ic_regular_magnifying_glass_24dp,
            )
            actionView = searchView
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
        }

        toolbar.layoutToolbar()
        val collapsedAction = toolbar.actionChild(SEARCH_ACTION_ID)

        assertThat(searchItem.actionView).isSameAs(searchView)
        assertThat(searchView.getTag(R.id.woo_ds_toolbar_action_view)).isNull()
        assertThat(collapsedAction).isNotSameAs(searchView)
        assertThat(collapsedAction.background).isNotNull()
        assertThat(collapsedAction.getTag(R.id.woo_ds_toolbar_action_view)).isEqualTo(true)

        assertThat(searchItem.expandActionView()).isTrue()
        assertThat(searchItem.actionView).isSameAs(searchView)
    }

    @Test
    fun `given custom action view, when shown in toolbar, then action view is preserved`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        val customActionView = AppCompatImageButton(toolbar.context).apply {
            id = ACTION_ID
            setImageDrawable(
                AppCompatResources.getDrawable(
                    toolbar.context,
                    R.drawable.woo_ds_ic_regular_arrow_up_right_24dp,
                )
            )
        }
        val item = toolbar.menu.add(0, ACTION_ID, 0, "Custom").apply {
            icon = AppCompatResources.getDrawable(
                toolbar.context,
                R.drawable.woo_ds_ic_regular_arrow_up_right_24dp,
            )
            actionView = customActionView
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        toolbar.layoutToolbar()

        assertThat(item.actionView).isSameAs(customActionView)
        assertThat(customActionView.getTag(R.id.woo_ds_toolbar_action_view)).isNull()
    }

    private fun toolbarContext() = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar,
    )

    private fun WooDesignSystemToolbar.addIconAction(
        showAsAction: Int = MenuItem.SHOW_AS_ACTION_ALWAYS,
        enabled: Boolean = true,
        title: String = "Open",
    ): MenuItem = menu.add(0, ACTION_ID, 0, title).apply {
        icon = AppCompatResources.getDrawable(context, R.drawable.woo_ds_ic_regular_arrow_up_right_24dp)
        isEnabled = enabled
        setShowAsAction(showAsAction)
    }

    private fun WooDesignSystemToolbar.layoutToolbar(widthDp: Int = 360) {
        measure(
            exactMeasureSpec(dp(widthDp)),
            exactMeasureSpec(resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_height)),
        )
        layout(0, 0, measuredWidth, measuredHeight)
    }

    private fun WooDesignSystemToolbar.actionChild(itemId: Int): View =
        checkNotNull(actionChildOrNull(itemId)) {
            "No rendered toolbar action child for item id $itemId"
        }

    private fun WooDesignSystemToolbar.actionChildOrNull(itemId: Int): View? =
        actionMenuView()
            .children
            .firstOrNull { child -> child.id == itemId }

    private fun WooDesignSystemToolbar.actionMenuView(): ActionMenuView =
        children.filterIsInstance<ActionMenuView>().first()

    private fun WooDesignSystemToolbar.navigationButton(contentDescription: String): AppCompatImageButton =
        children
            .filterIsInstance<AppCompatImageButton>()
            .first { it.contentDescription == contentDescription }

    private fun WooDesignSystemToolbar.titleTextView(title: String): TextView =
        children.filterIsInstance<TextView>().first { it.text == title }

    private fun exactMeasureSpec(size: Int): Int =
        View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    private fun View.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val ACTION_ID = 1
        const val TEXT_ACTION_ID = 2
        const val SEARCH_ACTION_ID = 3
    }
}

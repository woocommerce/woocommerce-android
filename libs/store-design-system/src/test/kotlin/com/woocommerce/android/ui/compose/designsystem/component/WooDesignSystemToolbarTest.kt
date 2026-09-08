package com.woocommerce.android.ui.compose.designsystem.component

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Rect
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.children
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.woocommerce.android.ui.compose.designsystem.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
class WooDesignSystemToolbarTest {
    @Test
    fun `when toolbar XML is inflated, then background is flat without a baked in divider`() {
        val toolbar = inflateToolbar()

        assertThat(toolbar.background).isInstanceOf(LayerDrawable::class.java)
        assertThat((toolbar.background as LayerDrawable).numberOfLayers).isEqualTo(1)
        assertThat(toolbar.elevation).isZero()
        assertThat(toolbar.isDividerVisible).isFalse()
    }

    @Test
    fun `given scrollable content, when scrolled and returned to rest, then divider follows the content`() {
        val scenario = layoutScrollScenario()

        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.toolbar.isDividerVisible).isFalse()
        scenario.scrollView.scrollTo(0, scenario.toolbar.dp(SCROLL_OFFSET_DP))

        scenario.toolbar.viewTreeObserver.dispatchOnScrollChanged()

        assertThat(scenario.toolbar.isDividerVisible).isTrue()

        scenario.scrollView.scrollTo(0, 0)
        scenario.toolbar.viewTreeObserver.dispatchOnScrollChanged()

        assertThat(scenario.toolbar.isDividerVisible).isFalse()
    }

    @Test
    fun `given scrollable content, when divider mode changes, then explicit modes override scroll state`() {
        val scenario = layoutScrollScenario()
        scenario.toolbar.dividerMode = WooDesignSystemToolbar.DividerMode.ALWAYS
        assertThat(scenario.toolbar.isDividerVisible).isTrue()

        scenario.toolbar.dividerMode = WooDesignSystemToolbar.DividerMode.NEVER
        scenario.scrollView.scrollTo(0, scenario.toolbar.dp(SCROLL_OFFSET_DP))

        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.toolbar.isDividerVisible).isFalse()
    }

    @Test
    fun `given two panes, when a toolbar declares its scroll target, then it ignores the other pane`() {
        val scenario = layoutTwoPaneScenario()
        scenario.secondToolbar.setScrollTarget(scenario.secondScrollView)
        scenario.scrollView.scrollTo(0, scenario.toolbar.dp(SCROLL_OFFSET_DP))

        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.toolbar.isDividerVisible).isTrue()
        assertThat(scenario.secondToolbar.isDividerVisible).isFalse()
    }

    @Test
    fun `given scroll target has no id, when it is hidden then shown and scrolled, then divider still tracks it`() {
        val scenario = layoutNoIdScrollTargetScenario()
        scenario.toolbar.setScrollTarget(scenario.targetScrollView)

        scenario.targetScrollView.visibility = View.INVISIBLE
        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()
        scenario.targetScrollView.visibility = View.VISIBLE
        scenario.targetScrollView.scrollTo(0, scenario.toolbar.dp(SCROLL_OFFSET_DP))

        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.toolbar.isDividerVisible).isTrue()
    }

    @Test
    fun `given scroll target is set via xml, when sibling and target scroll, then divider tracks only the target`() {
        val scenario = inflateScrollTargetAttributeScenario()
        scenario.decoy.scrollTo(0, scenario.toolbar.dp(SCROLL_OFFSET_DP))

        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.toolbar.isDividerVisible).isFalse()

        scenario.target.scrollTo(0, scenario.toolbar.dp(SCROLL_OFFSET_DP))
        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.toolbar.isDividerVisible).isTrue()
    }

    @Test
    fun `given explicit divider modes in xml, when content scrolls, then always and never are respected`() {
        val scenario = inflateDividerModeAttributeScenario()

        assertThat(scenario.alwaysToolbar.isDividerVisible).isTrue()
        scenario.neverScrollView.scrollTo(0, scenario.neverToolbar.dp(SCROLL_OFFSET_DP))

        scenario.neverToolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.neverToolbar.isDividerVisible).isFalse()
    }

    @Test
    fun `given recycler view with zero height first item and content that fits, when at rest, then divider stays hidden`() {
        val scenario = layoutRecyclerScrollScenario(
            listOf(0) + List(ZERO_HEIGHT_SIBLING_COUNT) { ZERO_HEIGHT_ITEM_HEIGHT_DP },
        )

        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.toolbar.isDividerVisible).isFalse()
    }

    @Test
    fun `given recycler children without adapter positions, when at rest, then divider stays hidden`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context = themedContext(activity)
        val toolbar = WooDesignSystemToolbar(context)
        val recyclerView = NoPositionRecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = FixedSizeItemAdapter(
                List(RECYCLER_ITEM_COUNT) { toolbar.dp(RECYCLER_ITEM_HEIGHT_DP) },
                RecyclerView.VERTICAL,
            )
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, toolbar.minimumHeight))
            addView(recyclerView, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        activity.setContentView(root)
        root.layoutAsScreen()

        toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(toolbar.isDividerVisible).isFalse()
    }

    @Test
    fun `given recycler view scrolled so the first item is recycled away, when layout is dispatched, then divider is shown`() {
        val scenario = layoutRecyclerScrollScenario(List(RECYCLER_ITEM_COUNT) { RECYCLER_ITEM_HEIGHT_DP })
        scenario.recyclerView.scrollBy(0, scenario.toolbar.dp(RECYCLER_SCROLL_OFFSET_DP))

        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.toolbar.isDividerVisible).isTrue()
    }

    @Test
    fun `given horizontal recycler view ahead of a scrollable pane, when the pane is scrolled, then divider is shown`() {
        val scenario = layoutHorizontalRecyclerScenario()
        scenario.scrollView.scrollTo(0, scenario.toolbar.dp(SCROLL_OFFSET_DP))

        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.toolbar.isDividerVisible).isTrue()
    }

    @Test
    fun `given a fitting recycler view ahead of a scrolled pane, when layout is dispatched, then the scrolled pane wins`() {
        val scenario = layoutFittingRecyclerScenario()
        scenario.scrollView.scrollTo(0, scenario.toolbar.dp(SCROLL_OFFSET_DP))

        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.toolbar.isDividerVisible).isTrue()
    }

    @Test
    fun `given nothing currently scrolls, when the type-eligible recycler view later overflows and is scrolled, then divider is shown`() {
        val scenario = layoutRecyclerScrollScenario(List(FITTING_ITEM_COUNT) { FITTING_ITEM_HEIGHT_DP })
        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()
        assertThat(scenario.toolbar.isDividerVisible).isFalse()

        scenario.recyclerView.layoutParams = scenario.recyclerView.layoutParams.apply {
            height = scenario.toolbar.dp(SHRUNK_RECYCLER_HEIGHT_DP)
        }
        (scenario.recyclerView.parent as ViewGroup).layoutAsScreen()
        scenario.recyclerView.scrollBy(0, scenario.toolbar.dp(RECYCLER_SCROLL_OFFSET_DP))
        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.toolbar.isDividerVisible).isTrue()
    }

    @Test
    fun `given a previously inert scroll host, when another host becomes scrollable, then auto discovery switches hosts`() {
        val scenario = layoutTimeShiftedScrollScenario()
        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        scenario.lateContent.minimumHeight = scenario.toolbar.dp(SCROLL_CONTENT_HEIGHT_DP)
        (scenario.lateScrollView.parent as ViewGroup).layoutAsScreen()
        scenario.lateScrollView.scrollTo(0, scenario.toolbar.dp(SCROLL_OFFSET_DP))
        scenario.toolbar.viewTreeObserver.dispatchOnScrollChanged()

        assertThat(scenario.toolbar.isDividerVisible).isTrue()
    }

    @Test
    fun `given a recycler view with a top item decoration inset, when scrolled less than the inset, then divider is shown`() {
        val scenario = layoutDecoratedRecyclerScenario()
        scenario.recyclerView.scrollBy(0, scenario.toolbar.dp(DECORATION_SCROLL_OFFSET_DP))

        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.toolbar.isDividerVisible).isTrue()
    }

    @Test
    fun `given a reverse layout recycler view whose content fits, when layout is dispatched, then divider stays hidden`() {
        val scenario = layoutReverseLayoutRecyclerScenario()

        scenario.toolbar.viewTreeObserver.dispatchOnGlobalLayout()

        assertThat(scenario.toolbar.isDividerVisible).isFalse()
    }

    @Test
    fun `when creating design system toolbar, then chrome is applied automatically`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())

        assertThat(toolbar.isTitleCentered).isFalse()
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
    @Config(qualifiers = "420dpi")
    fun `given navigation and actions, when laid out, then controls honor edge insets`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        val controlEdgeInset = toolbar.resources.getDimensionPixelSize(
            R.dimen.woo_ds_toolbar_control_edge_padding,
        )
        toolbar.navigationIcon = AppCompatResources.getDrawable(
            toolbar.context,
            R.drawable.woo_ds_ic_regular_angle_left_24dp,
        )
        toolbar.navigationContentDescription = "Back"
        toolbar.addIconAction()

        toolbar.layoutToolbar()
        val navigationButton = toolbar.navigationButton("Back")
        val actionMenuView = toolbar.actionMenuView()
        val action = toolbar.actionChild(ACTION_ID)

        assertThat(navigationButton.left).isEqualTo(controlEdgeInset)
        assertThat(actionMenuView.right).isEqualTo(toolbar.width - controlEdgeInset)
        assertThat(navigationButton.top)
            .isEqualTo(((toolbar.height - navigationButton.height) / 2f).roundToInt())
        assertThat(actionMenuView.top)
            .isEqualTo(((toolbar.height - actionMenuView.height) / 2f).roundToInt())
        assertThat(actionMenuView.top + action.top)
            .isEqualTo(((toolbar.height - action.height) / 2f).roundToInt())
    }

    @Test
    @Config(qualifiers = "420dpi")
    fun `given LTR navigation, when laid out, then title starts four dp after the leading target`() {
        // GIVEN
        val toolbar = WooDesignSystemToolbar(toolbarContext()).apply {
            title = "Products"
            navigationIcon = AppCompatResources.getDrawable(context, R.drawable.woo_ds_ic_regular_angle_left_24dp)
            navigationContentDescription = "Back"
        }

        // WHEN
        toolbar.layoutToolbar()

        // THEN
        val navigationButton = toolbar.navigationButton("Back")
        val titleView = toolbar.titleTextView("Products")
        val controlEdgeInset = toolbar.resources.getDimensionPixelSize(
            R.dimen.woo_ds_toolbar_control_edge_padding,
        )
        val touchTarget = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_icon_touch_target)
        val titleGap = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_navigation_title_spacing)

        assertThat(navigationButton.left).isEqualTo(controlEdgeInset)
        assertThat(navigationButton.right).isEqualTo(controlEdgeInset + touchTarget)
        assertThat(titleView.left).isEqualTo(navigationButton.right + titleGap)
    }

    @Test
    @Config(qualifiers = "420dpi")
    fun `given RTL navigation, when laid out, then title ends four dp before the leading target`() {
        // GIVEN
        val toolbar = WooDesignSystemToolbar(rtlToolbarContext()).apply {
            title = "Products"
            navigationIcon = AppCompatResources.getDrawable(context, R.drawable.woo_ds_ic_regular_angle_left_24dp)
            navigationContentDescription = "Back"
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // WHEN
        toolbar.layoutToolbar()

        // THEN
        val navigationButton = toolbar.navigationButton("Back")
        val titleView = toolbar.titleTextView("Products")
        val controlEdgeInset = toolbar.resources.getDimensionPixelSize(
            R.dimen.woo_ds_toolbar_control_edge_padding,
        )
        val touchTarget = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_icon_touch_target)
        val titleGap = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_navigation_title_spacing)

        assertThat(navigationButton.right).isEqualTo(toolbar.width - controlEdgeInset)
        assertThat(navigationButton.left).isEqualTo(navigationButton.right - touchTarget)
        assertThat(titleView.right).isEqualTo(navigationButton.left - titleGap)
    }

    @Test
    fun `when title is set after creation, then title remains visible`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())

        toolbar.title = "Products"
        toolbar.layoutToolbar()

        val titleView = toolbar.titleTextView("Products")
        assertThat(titleView.width).isGreaterThan(0)
        assertThat(titleView.visibility).isEqualTo(View.VISIBLE)
        assertThat(titleView.includeFontPadding).isFalse()
    }

    @Test
    fun `given medium centered toolbar with supporting text, when laid out, then the configured title content is shown`() {
        val toolbar = LayoutInflater.from(toolbarContext())
            .inflate(R.layout.woo_design_system_toolbar_configuration_test, null) as WooDesignSystemToolbar

        toolbar.layoutToolbar()

        val titleView = toolbar.titleTextView("XML Products")
        val subtitleView = toolbar.subtitleTextView("All products")

        assertThat(toolbar.configuration).isEqualTo(WooDesignSystemToolbar.Configuration.MEDIUM)
        assertThat(toolbar.titleAlignment).isEqualTo(WooDesignSystemToolbar.TitleAlignment.CENTER)
        assertThat(toolbar.minimumHeight)
            .isEqualTo(toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_medium_height))
        assertThat(toolbar.isTitleCentered).isTrue()
        assertThat(toolbar.height)
            .isEqualTo(toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_medium_height))
        assertThat(titleView.gravity and android.view.Gravity.HORIZONTAL_GRAVITY_MASK)
            .isEqualTo(android.view.Gravity.CENTER_HORIZONTAL)
        assertThat(subtitleView.visibility).isEqualTo(View.VISIBLE)
        assertThat(subtitleView.top).isGreaterThan(titleView.top)
    }

    @Test
    fun `given medium toolbar, when its scroll target moves away from top, then it collapses to small height and restores`() {
        val scenario = layoutScrollScenario(WooDesignSystemToolbar.Configuration.MEDIUM)
        val titleView = scenario.toolbar.titleTextView("Products")
        val subtitleView = scenario.toolbar.subtitleTextView("All products")

        assertThat(scenario.toolbar.height)
            .isEqualTo(scenario.toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_medium_height))
        assertThat(titleView.textSize).isEqualTo(scenario.toolbar.sp(MEDIUM_TITLE_TEXT_SIZE_SP))
        assertThat(titleView.lineHeight).isEqualTo(scenario.toolbar.sp(MEDIUM_TITLE_LINE_HEIGHT_SP).roundToInt())
        assertThat(subtitleView.visibility).isEqualTo(View.VISIBLE)

        scenario.scrollView.scrollTo(0, scenario.toolbar.dp(SCROLL_OFFSET_DP))
        scenario.toolbar.viewTreeObserver.dispatchOnScrollChanged()
        scenario.root.layoutAsScreen()

        assertThat(scenario.toolbar.isMediumCollapsed).isTrue()
        assertThat(scenario.toolbar.height)
            .isEqualTo(scenario.toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_height))
        assertThat(scenario.toolbar.layoutParams.height)
            .isEqualTo(scenario.toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_height))
        assertThat(scenario.toolbar.isDividerVisible).isTrue()
        assertThat(titleView.textSize).isEqualTo(scenario.toolbar.sp(SMALL_TITLE_TEXT_SIZE_SP))
        assertThat(titleView.lineHeight).isEqualTo(scenario.toolbar.sp(SMALL_TITLE_LINE_HEIGHT_SP).roundToInt())
        assertThat(subtitleView.visibility).isEqualTo(View.GONE)

        scenario.scrollView.scrollTo(0, 0)
        scenario.toolbar.viewTreeObserver.dispatchOnScrollChanged()
        scenario.root.layoutAsScreen()

        assertThat(scenario.toolbar.isMediumCollapsed).isFalse()
        assertThat(scenario.toolbar.height)
            .isEqualTo(scenario.toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_medium_height))
        assertThat(scenario.toolbar.layoutParams.height)
            .isEqualTo(scenario.toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_medium_height))
        assertThat(scenario.toolbar.isDividerVisible).isFalse()
        assertThat(titleView.textSize).isEqualTo(scenario.toolbar.sp(MEDIUM_TITLE_TEXT_SIZE_SP))
        assertThat(titleView.lineHeight).isEqualTo(scenario.toolbar.sp(MEDIUM_TITLE_LINE_HEIGHT_SP).roundToInt())
        assertThat(subtitleView.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun `given attached small toolbar, when switched to medium and back, then its measured height updates`() {
        // GIVEN
        val scenario = layoutScrollScenario()

        // WHEN
        scenario.toolbar.configuration = WooDesignSystemToolbar.Configuration.MEDIUM
        scenario.root.layoutAsScreen()

        // THEN
        val mediumHeight = scenario.toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_medium_height)
        assertThat(scenario.toolbar.height).isEqualTo(mediumHeight)
        assertThat(scenario.toolbar.isMediumCollapsed).isFalse()

        // WHEN
        scenario.toolbar.configuration = WooDesignSystemToolbar.Configuration.SMALL
        scenario.root.layoutAsScreen()

        // THEN
        val smallHeight = scenario.toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_height)
        assertThat(scenario.toolbar.height).isEqualTo(smallHeight)
        assertThat(scenario.toolbar.isMediumCollapsed).isFalse()
    }

    @Test
    fun `given long title and text action, when laid out, then title keeps spacing from action`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext()).apply {
            title = "Trailblazer Trek Pants with really long name"
            menu.add(0, TEXT_ACTION_ID, 0, "Save").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        val controlSpacing = toolbar.resources.getDimensionPixelSize(
            R.dimen.woo_ds_toolbar_title_control_spacing,
        )

        toolbar.layoutToolbar()
        val titleView = toolbar.titleTextView(toolbar.title.toString())
        val action = toolbar.actionChild(TEXT_ACTION_ID)
        val actionLeft = toolbar.actionMenuView().left + action.left

        assertThat(titleView.paddingStart).isZero()
        assertThat(titleView.paddingEnd).isZero()
        assertThat(actionLeft - titleView.right).isGreaterThanOrEqualTo(controlSpacing)
    }

    @Test
    fun `given long title with icon and overflow actions, when laid out, then title keeps spacing from actions`() {
        // GIVEN
        val toolbar = WooDesignSystemToolbar(toolbarContext()).apply {
            title = "Beanie with Logo - Enormous Leather Company Limited ".repeat(10).trim()
            navigationIcon = AppCompatResources.getDrawable(
                context,
                R.drawable.woo_ds_ic_regular_angle_left_24dp,
            )
            navigationContentDescription = "Back"
            addIconAction(title = "Share")
            addOverflowAction()
        }
        val controlSpacing = toolbar.resources.getDimensionPixelSize(
            R.dimen.woo_ds_toolbar_title_control_spacing,
        )

        // WHEN
        toolbar.layoutToolbar(widthDp = 480)

        // THEN
        val titleView = toolbar.titleTextView(toolbar.title.toString())
        val action = toolbar.actionChild(ACTION_ID)
        val actionLeft = toolbar.actionMenuView().left + action.left
        assertThat(actionLeft - titleView.right).isGreaterThanOrEqualTo(controlSpacing)
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
    fun `given overflow item, when menu is rendered, then overflow matches flat icon contract`() {
        val toolbar = LayoutInflater.from(toolbarContext())
            .inflate(R.layout.woo_design_system_toolbar_test, null) as WooDesignSystemToolbar

        toolbar.layoutToolbar()
        val overflowButton = toolbar.overflowButton()
        val touchTarget = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_icon_touch_target)
        val iconPadding = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_icon_padding)
        val overflowLayoutParams = overflowButton.layoutParams as ActionMenuView.LayoutParams
        val expectedTint = checkNotNull(
            AppCompatResources.getColorStateList(toolbar.context, R.color.woo_ds_toolbar_icon_button_tint),
        )

        assertThat(overflowLayoutParams.isOverflowButton).isTrue()
        assertThat(overflowLayoutParams.width).isEqualTo(touchTarget)
        assertThat(overflowLayoutParams.height).isEqualTo(touchTarget)
        assertThat(overflowButton.measuredWidth).isEqualTo(touchTarget)
        assertThat(overflowButton.measuredHeight).isEqualTo(touchTarget)
        assertThat(overflowButton.minimumWidth).isEqualTo(touchTarget)
        assertThat(overflowButton.minimumHeight).isEqualTo(touchTarget)
        assertThat(overflowButton.paddingLeft).isEqualTo(iconPadding)
        assertThat(overflowButton.paddingTop).isEqualTo(iconPadding)
        assertThat(overflowButton.paddingRight).isEqualTo(iconPadding)
        assertThat(overflowButton.paddingBottom).isEqualTo(iconPadding)
        assertThat(overflowButton.scaleType).isEqualTo(ImageView.ScaleType.FIT_CENTER)
        assertThat(overflowButton.background).isInstanceOf(RippleDrawable::class.java)
        assertThat(overflowButton.getTag(R.id.woo_ds_toolbar_action_view)).isEqualTo(true)
        assertThat(shadowOf(overflowButton.drawable).createdFromResId)
            .isEqualTo(R.drawable.woo_ds_ic_regular_ellipsis_24dp)
        val actualTint = checkNotNull(ImageViewCompat.getImageTintList(overflowButton))
        val disabledState = intArrayOf(-android.R.attr.state_enabled)
        assertThat(actualTint.isStateful).isEqualTo(expectedTint.isStateful)
        assertThat(actualTint.defaultColor).isEqualTo(expectedTint.defaultColor)
        assertThat(actualTint.getColorForState(disabledState, actualTint.defaultColor))
            .isEqualTo(expectedTint.getColorForState(disabledState, expectedTint.defaultColor))
        assertThat(overflowButton.contentDescription)
            .isEqualTo(toolbar.context.getString(androidx.appcompat.R.string.abc_action_menu_overflow_description))
        assertThat(overflowButton.isClickable).isTrue()
        assertThat(overflowButton.isFocusable).isTrue()
    }

    @Test
    fun `given decorated overflow, when laid out again, then background and overflow layout params are reused`() {
        val toolbar = LayoutInflater.from(toolbarContext())
            .inflate(R.layout.woo_design_system_toolbar_test, null) as WooDesignSystemToolbar

        toolbar.layoutToolbar()
        val firstOverflowButton = toolbar.overflowButton()
        val firstBackground = firstOverflowButton.background
        val firstLayoutParams = firstOverflowButton.layoutParams

        toolbar.layoutToolbar()
        val secondOverflowButton = toolbar.overflowButton()

        assertThat(secondOverflowButton).isSameAs(firstOverflowButton)
        assertThat(secondOverflowButton.background).isSameAs(firstBackground)
        assertThat(secondOverflowButton.layoutParams).isSameAs(firstLayoutParams)
        assertThat((secondOverflowButton.layoutParams as ActionMenuView.LayoutParams).isOverflowButton).isTrue()
    }

    @Test
    fun `given decorated overflow, when menu is cleared and reinflated, then overflow remains styled`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        toolbar.inflateMenu(R.menu.woo_design_system_toolbar_test_menu)
        toolbar.layoutToolbar()
        val overflowButton = toolbar.overflowButton()
        val overflowBackground = overflowButton.background
        val touchTarget = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_icon_touch_target)

        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.woo_design_system_toolbar_test_menu)
        val overflowLayoutParams = overflowButton.layoutParams as ActionMenuView.LayoutParams
        overflowLayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        overflowLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        toolbar.layoutToolbar()
        val redecoratedOverflowButton = toolbar.overflowButton()

        assertThat(redecoratedOverflowButton).isSameAs(overflowButton)
        assertThat(redecoratedOverflowButton.layoutParams).isSameAs(overflowLayoutParams)
        assertThat(overflowLayoutParams.width).isEqualTo(touchTarget)
        assertThat(overflowLayoutParams.height).isEqualTo(touchTarget)
        assertThat(overflowButton.background).isInstanceOf(RippleDrawable::class.java)
        assertThat(overflowButton.background).isSameAs(overflowBackground)
        assertThat(overflowButton.getTag(R.id.woo_ds_toolbar_action_view)).isEqualTo(true)
        assertThat(shadowOf(overflowButton.drawable).createdFromResId)
            .isEqualTo(R.drawable.woo_ds_ic_regular_ellipsis_24dp)
        assertThat(overflowLayoutParams.isOverflowButton).isTrue()
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `given light theme, when icon controls are rendered, then ripple matches compose pressed state`() {
        assertToolbarIconRippleColor()
    }

    @Test
    @Config(qualifiers = "night")
    fun `given dark theme, when icon controls are rendered, then ripple matches compose pressed state`() {
        assertToolbarIconRippleColor()
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `given light theme, when actions are rendered, then icon and text actions use on surface colors`() {
        assertToolbarActionTints()
    }

    @Test
    @Config(qualifiers = "night")
    fun `given dark theme, when actions are rendered, then icon and text actions use on surface colors`() {
        assertToolbarActionTints()
    }

    @Test
    @Config(qualifiers = "ldrtl")
    fun `given rtl layout with overflow, when laid out, then actions honor the logical end inset`() {
        val toolbar = WooDesignSystemToolbar(rtlToolbarContext()).apply {
            addOverflowAction()
        }
        val controlEdgeInset = toolbar.resources.getDimensionPixelSize(
            R.dimen.woo_ds_toolbar_control_edge_padding,
        )
        toolbar.measure(
            exactMeasureSpec(toolbar.dp(360)),
            exactMeasureSpec(toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_height)),
        )
        toolbar.layoutDirection = View.LAYOUT_DIRECTION_RTL
        toolbar.layout(0, 0, toolbar.measuredWidth, toolbar.measuredHeight)

        assertThat(toolbar.layoutDirection).isEqualTo(View.LAYOUT_DIRECTION_RTL)
        assertThat(toolbar.actionMenuView().left).isEqualTo(controlEdgeInset)
        assertThat((toolbar.overflowButton().layoutParams as ActionMenuView.LayoutParams).isOverflowButton).isTrue()
    }

    @Test
    fun `given inflated icon item, when shown in toolbar, then item uses a flat touch target by default`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        toolbar.addIconAction()

        toolbar.layoutToolbar()
        val action = toolbar.actionChild(ACTION_ID) as TextView
        val touchTarget = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_icon_touch_target)
        val iconSize = toolbar.resources.getDimensionPixelSize(R.dimen.woo_ds_toolbar_icon_size)
        val backgroundPadding = Rect().also { action.background.getPadding(it) }
        val icon = action.compoundDrawables.filterNotNull().single()

        assertThat(action.measuredWidth).isEqualTo(touchTarget)
        assertThat(action.measuredHeight).isEqualTo(touchTarget)
        assertThat(action.background).isNotNull()
        assertThat(action.getTag(R.id.woo_ds_toolbar_action_view)).isEqualTo(true)
        assertThat(action.gravity).isEqualTo(android.view.Gravity.CENTER)
        assertThat(backgroundPadding).isEqualTo(Rect())
        assertThat(action.paddingTop).isEqualTo(action.paddingBottom)
        assertThat(action.compoundDrawables.filterNotNull()).hasSize(1)
        assertThat(icon.bounds.width()).isEqualTo(iconSize)
        assertThat(icon.bounds.height()).isEqualTo(iconSize)
    }

    @Test
    fun `given flat icon item, when laid out again, then decoration background is reused`() {
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
    fun `given flat icon item, when icon is removed, then default action styling is restored`() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        val item = toolbar.addIconAction()

        toolbar.layoutToolbar()
        val outlinedAction = toolbar.actionChild(ACTION_ID)
        val outlinedBackground = outlinedAction.background

        item.icon = null
        toolbar.layoutToolbar()
        val actionTextView = toolbar.actionChild(ACTION_ID) as TextView

        assertThat(actionTextView).isSameAs(outlinedAction)
        assertThat(actionTextView.text).isEqualTo("Open")
        assertThat(actionTextView.background).isNotNull().isNotSameAs(outlinedBackground)
        assertThat(actionTextView.layoutParams.width).isEqualTo(ViewGroup.LayoutParams.WRAP_CONTENT)
        assertThat(actionTextView.getTag(R.id.woo_ds_toolbar_action_view)).isNull()
        assertThat(actionTextView.getTag(R.id.woo_ds_toolbar_action_icon)).isNull()
        assertThat(actionTextView.compoundDrawables.filterNotNull()).isEmpty()
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
        assertThat(action.background).isInstanceOf(RippleDrawable::class.java)
        val ripple = action.background as RippleDrawable
        ripple.setBounds(0, 0, action.width, action.height)
        val rippleMask = ripple.findDrawableByLayerId(android.R.id.mask)
        assertThat(rippleMask.bounds).isEqualTo(Rect(0, 0, action.width, action.height))
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
    fun `given collapsed search item, when shown in toolbar, then collapsed trigger is flat`() {
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

    private fun layoutScrollScenario(
        configuration: WooDesignSystemToolbar.Configuration = WooDesignSystemToolbar.Configuration.SMALL,
    ): ScrollScenario {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context = themedContext(activity)
        val toolbar = WooDesignSystemToolbar(context).apply {
            this.configuration = configuration
            title = "Products"
            supportingText = "All products"
        }
        val scrollView = scrollView(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, toolbar.minimumHeight))
            addView(scrollView, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

        activity.setContentView(root)
        root.layoutAsScreen()
        return ScrollScenario(toolbar, scrollView, root)
    }

    private fun layoutTwoPaneScenario(): TwoPaneScenario {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context = themedContext(activity)
        val toolbar = WooDesignSystemToolbar(context)
        val scrollView = scrollView(context)
        val secondToolbar = WooDesignSystemToolbar(context)
        val secondScrollView = scrollView(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, toolbar.minimumHeight))
            addView(scrollView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            addView(secondToolbar, LinearLayout.LayoutParams(MATCH_PARENT, secondToolbar.minimumHeight))
            addView(secondScrollView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        }

        activity.setContentView(root)
        root.layoutAsScreen()
        return TwoPaneScenario(toolbar, scrollView, secondToolbar, secondScrollView)
    }

    private fun layoutNoIdScrollTargetScenario(): NoIdScrollTargetScenario {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context = themedContext(activity)
        val toolbar = WooDesignSystemToolbar(context)
        val decoyScrollView = scrollView(context)
        val targetScrollView = ScrollView(context).apply {
            val content = View(context).apply { minimumHeight = dp(SCROLL_CONTENT_HEIGHT_DP) }
            addView(content, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, toolbar.minimumHeight))
            addView(decoyScrollView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            addView(targetScrollView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        }

        activity.setContentView(root)
        root.layoutAsScreen()
        return NoIdScrollTargetScenario(toolbar, targetScrollView, decoyScrollView)
    }

    private fun inflateScrollTargetAttributeScenario(): ScrollTargetAttributeScenario {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context = themedContext(activity)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.woo_design_system_toolbar_scroll_target_test, null) as ViewGroup

        activity.setContentView(root)
        root.layoutAsScreen()

        return ScrollTargetAttributeScenario(
            toolbar = root.findViewById(R.id.woo_ds_toolbar_scroll_target_test_toolbar),
            target = root.findViewById(R.id.woo_ds_toolbar_scroll_target_test_target),
            decoy = root.findViewById(R.id.woo_ds_toolbar_scroll_target_test_decoy),
        )
    }

    private fun inflateDividerModeAttributeScenario(): DividerModeAttributeScenario {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context = themedContext(activity)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.woo_design_system_toolbar_divider_mode_test, null) as ViewGroup

        activity.setContentView(root)
        root.layoutAsScreen()

        return DividerModeAttributeScenario(
            alwaysToolbar = root.findViewById(R.id.woo_ds_toolbar_divider_mode_test_always_toolbar),
            neverToolbar = root.findViewById(R.id.woo_ds_toolbar_divider_mode_test_never_toolbar),
            neverScrollView = root.findViewById(R.id.woo_ds_toolbar_divider_mode_test_never_scroll_view),
        )
    }

    private fun layoutRecyclerScrollScenario(itemHeightsDp: List<Int>): RecyclerScenario {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context = themedContext(activity)
        val toolbar = WooDesignSystemToolbar(context)
        val recyclerView = verticalRecyclerView(context, itemHeightsDp.map { toolbar.dp(it) })
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, toolbar.minimumHeight))
            addView(recyclerView, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

        activity.setContentView(root)
        root.layoutAsScreen()
        return RecyclerScenario(toolbar, recyclerView)
    }

    private fun layoutTimeShiftedScrollScenario(): TimeShiftedScrollScenario {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context = themedContext(activity)
        val toolbar = WooDesignSystemToolbar(context)
        val initiallyInertScrollView = ScrollView(context).apply {
            addView(View(context), ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        val lateContent = View(context)
        val lateScrollView = ScrollView(context).apply {
            addView(lateContent, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, toolbar.minimumHeight))
            addView(initiallyInertScrollView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            addView(lateScrollView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        }

        activity.setContentView(root)
        root.layoutAsScreen()
        return TimeShiftedScrollScenario(toolbar, lateScrollView, lateContent)
    }

    /**
     * Puts a horizontal recycler view ahead of a real vertical [ScrollView] sibling. If discovery
     * wrongly selected the horizontal recycler view as the scroll target, it would never reach the
     * scroll view, and scrolling it would have no effect on the divider.
     */
    private fun layoutHorizontalRecyclerScenario(): LeadingCandidateScenario {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context = themedContext(activity)
        val toolbar = WooDesignSystemToolbar(context)
        val recyclerView = RecyclerView(context).apply {
            id = View.generateViewId()
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = FixedSizeItemAdapter(
                itemSizesPx = List(HORIZONTAL_ITEM_COUNT) { toolbar.dp(HORIZONTAL_ITEM_SIZE_DP) },
                orientation = RecyclerView.HORIZONTAL,
            )
        }
        val scrollView = scrollView(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, toolbar.minimumHeight))
            addView(recyclerView, LinearLayout.LayoutParams(MATCH_PARENT, toolbar.dp(HORIZONTAL_ITEM_SIZE_DP)))
            addView(scrollView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        }

        activity.setContentView(root)
        root.layoutAsScreen()
        return LeadingCandidateScenario(toolbar, scrollView)
    }

    /**
     * Puts a recycler view whose content fits (nothing to scroll) ahead of a real vertical
     * [ScrollView] sibling. Discovery prefers a host that can currently scroll, so the scroll view
     * is selected over the inert recycler view; scrolling it should show the divider.
     */
    private fun layoutFittingRecyclerScenario(): LeadingCandidateScenario {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context = themedContext(activity)
        val toolbar = WooDesignSystemToolbar(context)
        val recyclerView = verticalRecyclerView(
            context,
            List(FITTING_ITEM_COUNT) { toolbar.dp(FITTING_ITEM_HEIGHT_DP) },
        )
        val scrollView = scrollView(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, toolbar.minimumHeight))
            addView(recyclerView, LinearLayout.LayoutParams(MATCH_PARENT, toolbar.dp(FITTING_RECYCLER_HEIGHT_DP)))
            addView(scrollView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        }

        activity.setContentView(root)
        root.layoutAsScreen()
        return LeadingCandidateScenario(toolbar, scrollView)
    }

    private fun layoutDecoratedRecyclerScenario(): RecyclerScenario {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context = themedContext(activity)
        val toolbar = WooDesignSystemToolbar(context)
        val recyclerView = verticalRecyclerView(
            context,
            List(RECYCLER_ITEM_COUNT) { toolbar.dp(RECYCLER_ITEM_HEIGHT_DP) },
        ).apply {
            setPadding(0, toolbar.dp(DECORATION_PADDING_TOP_DP), 0, 0)
            addItemDecoration(TopInsetItemDecoration(toolbar.dp(DECORATION_INSET_DP)))
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, toolbar.minimumHeight))
            addView(recyclerView, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

        activity.setContentView(root)
        root.layoutAsScreen()
        return RecyclerScenario(toolbar, recyclerView)
    }

    /**
     * Reverse layout pins position 0 to the visual bottom, so overflowing content would
     * legitimately leave rows hidden above the toolbar. Content here fits entirely instead, so
     * nothing scrolls in either direction, isolating the fallback branch from that ambiguity.
     */
    private fun layoutReverseLayoutRecyclerScenario(): RecyclerScenario {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context = themedContext(activity)
        val toolbar = WooDesignSystemToolbar(context)
        val recyclerView = RecyclerView(context).apply {
            id = View.generateViewId()
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
            adapter = FixedSizeItemAdapter(
                itemSizesPx = List(FITTING_ITEM_COUNT) { toolbar.dp(FITTING_ITEM_HEIGHT_DP) },
                orientation = RecyclerView.VERTICAL,
            )
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, toolbar.minimumHeight))
            addView(recyclerView, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

        activity.setContentView(root)
        root.layoutAsScreen()
        return RecyclerScenario(toolbar, recyclerView)
    }

    private fun verticalRecyclerView(context: Context, itemHeightsPx: List<Int>) =
        RecyclerView(context).apply {
            id = View.generateViewId()
            layoutManager = LinearLayoutManager(context)
            adapter = FixedSizeItemAdapter(itemHeightsPx, RecyclerView.VERTICAL)
        }

    private fun themedContext(activity: Activity) = ContextThemeWrapper(
        activity,
        com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar,
    )

    private fun scrollView(context: Context) = ScrollView(context).apply {
        id = View.generateViewId()
        val content = View(context).apply { minimumHeight = dp(SCROLL_CONTENT_HEIGHT_DP) }
        addView(content, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    private fun View.layoutAsScreen() {
        measure(exactMeasureSpec(dp(SCREEN_WIDTH_DP)), exactMeasureSpec(dp(SCREEN_HEIGHT_DP)))
        layout(0, 0, measuredWidth, measuredHeight)
    }

    private class ScrollScenario(
        val toolbar: WooDesignSystemToolbar,
        val scrollView: ScrollView,
        val root: LinearLayout,
    )

    private class TwoPaneScenario(
        val toolbar: WooDesignSystemToolbar,
        val scrollView: ScrollView,
        val secondToolbar: WooDesignSystemToolbar,
        val secondScrollView: ScrollView,
    )

    private class NoIdScrollTargetScenario(
        val toolbar: WooDesignSystemToolbar,
        val targetScrollView: ScrollView,
        val decoyScrollView: ScrollView,
    )

    private class ScrollTargetAttributeScenario(
        val toolbar: WooDesignSystemToolbar,
        val target: ScrollView,
        val decoy: ScrollView,
    )

    private class DividerModeAttributeScenario(
        val alwaysToolbar: WooDesignSystemToolbar,
        val neverToolbar: WooDesignSystemToolbar,
        val neverScrollView: ScrollView,
    )

    private class RecyclerScenario(
        val toolbar: WooDesignSystemToolbar,
        val recyclerView: RecyclerView,
    )

    private class LeadingCandidateScenario(
        val toolbar: WooDesignSystemToolbar,
        val scrollView: ScrollView,
    )

    private class TimeShiftedScrollScenario(
        val toolbar: WooDesignSystemToolbar,
        val lateScrollView: ScrollView,
        val lateContent: View,
    )

    /** Sizes items along the scroll axis; the cross axis always matches the parent. */
    private class FixedSizeItemAdapter(
        private val itemSizesPx: List<Int>,
        private val orientation: Int,
    ) : RecyclerView.Adapter<FixedSizeItemAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(View(parent.context))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.itemView.layoutParams = if (orientation == RecyclerView.HORIZONTAL) {
                ViewGroup.LayoutParams(itemSizesPx[position], ViewGroup.LayoutParams.MATCH_PARENT)
            } else {
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemSizesPx[position])
            }
        }

        override fun getItemCount(): Int = itemSizesPx.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    private class NoPositionRecyclerView(context: Context) : RecyclerView(context) {
        override fun getChildAdapterPosition(child: View): Int = RecyclerView.NO_POSITION
    }

    /** Adds a leading inset before the first item, like a section header would. */
    private class TopInsetItemDecoration(private val insetPx: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = insetPx
            }
        }
    }

    private fun toolbarContext() = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar,
    )

    private fun rtlToolbarContext() = object : ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar,
    ) {
        private val rtlApplicationInfo = ApplicationInfo(super.getApplicationInfo()).apply {
            flags = flags or ApplicationInfo.FLAG_SUPPORTS_RTL
        }

        override fun getApplicationInfo() = rtlApplicationInfo
    }

    private fun inflateToolbar() = LayoutInflater.from(toolbarContext())
        .inflate(R.layout.woo_design_system_toolbar_test, null) as WooDesignSystemToolbar

    private fun WooDesignSystemToolbar.addIconAction(
        showAsAction: Int = MenuItem.SHOW_AS_ACTION_ALWAYS,
        enabled: Boolean = true,
        title: String = "Open",
    ): MenuItem = menu.add(0, ACTION_ID, 0, title).apply {
        icon = AppCompatResources.getDrawable(context, R.drawable.woo_ds_ic_regular_arrow_up_right_24dp)
        isEnabled = enabled
        setShowAsAction(showAsAction)
    }

    private fun WooDesignSystemToolbar.addOverflowAction(): MenuItem =
        menu.add(0, OVERFLOW_ACTION_ID, 0, "Settings").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }

    private fun WooDesignSystemToolbar.layoutToolbar(widthDp: Int = 360) {
        measure(
            exactMeasureSpec(dp(widthDp)),
            exactMeasureSpec(minimumHeight),
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

    private fun WooDesignSystemToolbar.overflowButton(): ImageView =
        actionMenuView()
            .children
            .first { child ->
                (child.layoutParams as? ActionMenuView.LayoutParams)?.isOverflowButton == true
            } as ImageView

    private fun WooDesignSystemToolbar.navigationButton(contentDescription: String): AppCompatImageButton =
        children
            .filterIsInstance<AppCompatImageButton>()
            .first { it.contentDescription == contentDescription }

    private fun WooDesignSystemToolbar.titleTextView(title: String): TextView =
        children.filterIsInstance<TextView>().first { it.text == title }

    private fun WooDesignSystemToolbar.subtitleTextView(subtitle: String): TextView =
        children.filterIsInstance<TextView>().first { it.text == subtitle }

    private fun assertToolbarIconRippleColor() {
        val toolbar = WooDesignSystemToolbar(toolbarContext())
        toolbar.navigationIcon = AppCompatResources.getDrawable(
            toolbar.context,
            R.drawable.woo_ds_ic_regular_angle_left_24dp,
        )
        toolbar.navigationContentDescription = "Back"
        toolbar.addIconAction()
        toolbar.addOverflowAction()

        toolbar.layoutToolbar()
        val expectedColor = ColorUtils.setAlphaComponent(
            ContextCompat.getColor(toolbar.context, R.color.woo_ds_color_surface_on_default),
            (FULL_COLOR_ALPHA * PRESSED_STATE_ALPHA).roundToInt(),
        )
        val rippleColor = checkNotNull(
            AppCompatResources.getColorStateList(toolbar.context, R.color.woo_ds_toolbar_icon_button_ripple),
        )
        val resourceBackground = AppCompatResources.getDrawable(
            toolbar.context,
            R.drawable.woo_ds_toolbar_icon_button_background,
        )

        assertThat(rippleColor.defaultColor).isEqualTo(expectedColor)
        assertThat(toolbar.navigationButton("Back").background).isInstanceOf(RippleDrawable::class.java)
        assertThat(toolbar.actionChild(ACTION_ID).background).isInstanceOf(RippleDrawable::class.java)
        assertThat(toolbar.overflowButton().background).isInstanceOf(RippleDrawable::class.java)
        assertThat(ImageViewCompat.getImageTintList(toolbar.overflowButton())?.defaultColor)
            .isEqualTo(ContextCompat.getColor(toolbar.context, R.color.woo_ds_color_surface_on_default))
        assertThat(resourceBackground).isInstanceOf(RippleDrawable::class.java)
    }

    private fun assertToolbarActionTints() {
        val enabledIconToolbar = WooDesignSystemToolbar(toolbarContext()).apply {
            addIconAction()
        }
        enabledIconToolbar.layoutToolbar()

        val disabledIconToolbar = WooDesignSystemToolbar(toolbarContext()).apply {
            addIconAction(enabled = false)
        }
        disabledIconToolbar.layoutToolbar()

        val textActionToolbar = WooDesignSystemToolbar(toolbarContext()).apply {
            menu.add(0, TEXT_ACTION_ID, 0, "Edit").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        textActionToolbar.layoutToolbar()

        val enabledIconTint = checkNotNull(
            TextViewCompat.getCompoundDrawableTintList(enabledIconToolbar.actionChild(ACTION_ID) as TextView),
        )
        val disabledIconTint = checkNotNull(
            TextViewCompat.getCompoundDrawableTintList(disabledIconToolbar.actionChild(ACTION_ID) as TextView),
        )
        val textActionColors = (textActionToolbar.actionChild(TEXT_ACTION_ID) as TextView).textColors
        val expectedEnabledColor = ContextCompat.getColor(
            enabledIconToolbar.context,
            R.color.woo_ds_color_surface_on_default,
        )
        val expectedDisabledColor = ContextCompat.getColor(
            enabledIconToolbar.context,
            R.color.woo_ds_color_surface_on_variant_lowest,
        )

        assertThat(enabledIconTint.defaultColor).isEqualTo(expectedEnabledColor)
        assertThat(disabledIconTint.getColorForState(DISABLED_STATE, disabledIconTint.defaultColor))
            .isEqualTo(expectedDisabledColor)
        assertThat(textActionColors.defaultColor).isEqualTo(expectedEnabledColor)
        assertThat(textActionColors.getColorForState(DISABLED_STATE, textActionColors.defaultColor))
            .isEqualTo(expectedDisabledColor)
    }

    private fun exactMeasureSpec(size: Int): Int =
        View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    private fun View.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun WooDesignSystemToolbar.sp(value: Int): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value.toFloat(), resources.displayMetrics)

    private companion object {
        const val ACTION_ID = 1
        const val TEXT_ACTION_ID = 2
        const val SEARCH_ACTION_ID = 3
        const val OVERFLOW_ACTION_ID = 4
        const val FULL_COLOR_ALPHA = 255
        const val PRESSED_STATE_ALPHA = 0.1f
        val DISABLED_STATE = intArrayOf(-android.R.attr.state_enabled)
        const val SCROLL_CONTENT_HEIGHT_DP = 2000
        const val SCROLL_OFFSET_DP = 24
        const val SCREEN_WIDTH_DP = 360
        const val SCREEN_HEIGHT_DP = 640
        const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
        const val ZERO_HEIGHT_SIBLING_COUNT = 4
        const val ZERO_HEIGHT_ITEM_HEIGHT_DP = 50
        const val RECYCLER_ITEM_COUNT = 20
        const val RECYCLER_ITEM_HEIGHT_DP = 100
        const val RECYCLER_SCROLL_OFFSET_DP = 250
        const val HORIZONTAL_ITEM_COUNT = 5
        const val HORIZONTAL_ITEM_SIZE_DP = 200
        const val FITTING_ITEM_COUNT = 3
        const val FITTING_ITEM_HEIGHT_DP = 40
        const val FITTING_RECYCLER_HEIGHT_DP = 300
        const val SHRUNK_RECYCLER_HEIGHT_DP = 60
        const val DECORATION_PADDING_TOP_DP = 24
        const val DECORATION_INSET_DP = 16
        const val DECORATION_SCROLL_OFFSET_DP = 8
        const val MEDIUM_TITLE_TEXT_SIZE_SP = 24
        const val MEDIUM_TITLE_LINE_HEIGHT_SP = 32
        const val SMALL_TITLE_TEXT_SIZE_SP = 17
        const val SMALL_TITLE_LINE_HEIGHT_SP = 24
    }
}

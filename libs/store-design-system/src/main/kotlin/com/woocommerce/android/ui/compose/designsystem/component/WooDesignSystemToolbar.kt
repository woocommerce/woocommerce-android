package com.woocommerce.android.ui.compose.designsystem.component

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AbsListView
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.ScrollingView
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.woocommerce.android.ui.compose.designsystem.R
import kotlin.math.roundToInt

class WooDesignSystemToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.toolbarStyle,
) : MaterialToolbar(context.withWooToolbarTheme(), attrs, defStyleAttr) {
    private val dividerPaint = Paint().apply {
        color = context.color(R.color.woo_ds_color_tint_layer_on_surface_opacity16)
    }
    private val dividerHeight = context.dimensionPixelSize(R.dimen.woo_ds_toolbar_divider_height)
    private val scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
        refreshScrollTarget()
        updateDivider()
    }
    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        refreshScrollTarget()
        updateDivider()
    }

    private var scrollTarget: View? = null
    private var explicitScrollTarget: View? = null
    private var scrollTargetId = NO_ID
    @VisibleForTesting
    internal var autoDiscoveryFailureCount = 0
        private set

    @VisibleForTesting
    internal var isDividerVisible = false
        private set

    @VisibleForTesting
    internal var isMediumCollapsed = false
        private set

    var dividerMode: DividerMode = DividerMode.AUTO
        set(value) {
            if (field == value) return
            field = value
            updateDivider()
        }

    /**
     * Changes the toolbar's height and title placement without changing toolbar/menu ownership.
     * Existing XML remains small unless it explicitly opts into [Configuration.MEDIUM].
     */
    var configuration: Configuration = Configuration.SMALL
        set(value) {
            if (field == value) return
            val previousConfiguration = field
            field = value
            isMediumCollapsed = false
            applyConfigurationChrome()
            updateSupportingTextVisibility()
            updateDivider()
            if (previousConfiguration == Configuration.MEDIUM || value == Configuration.MEDIUM) {
                updateToolbarLayoutHeight(force = true)
            }
            requestLayout()
        }

    /** Controls whether the title is positioned from the start edge or centered in the toolbar. */
    var titleAlignment: TitleAlignment = TitleAlignment.START
        set(value) {
            if (field == value) return
            field = value
            setTitleCentered(value == TitleAlignment.CENTER)
            requestLayout()
        }

    /** Optional secondary text shown below the title. */
    var supportingText: CharSequence?
        get() = subtitle
        set(value) {
            if (subtitle == value) return
            subtitle = value
            updateSupportingTextVisibility()
            requestLayout()
        }

    init {
        applyStaticChrome()
        context.withStyledAttributes(attrs, R.styleable.WooDesignSystemToolbar) {
            configuration = Configuration.entries.getOrElse(
                getInt(R.styleable.WooDesignSystemToolbar_wooToolbarConfiguration, 0),
            ) { Configuration.SMALL }
            titleAlignment = TitleAlignment.entries.getOrElse(
                getInt(R.styleable.WooDesignSystemToolbar_wooToolbarTitleAlignment, 0),
            ) { TitleAlignment.START }
            if (hasValue(R.styleable.WooDesignSystemToolbar_wooToolbarSupportingText)) {
                supportingText = getText(R.styleable.WooDesignSystemToolbar_wooToolbarSupportingText)
            }
            dividerMode = DividerMode.entries.getOrElse(
                getInt(R.styleable.WooDesignSystemToolbar_wooToolbarDividerMode, 0),
            ) { DividerMode.AUTO }
            scrollTargetId = getResourceId(R.styleable.WooDesignSystemToolbar_wooToolbarScrollTarget, NO_ID)
        }
    }

    /**
     * Binds the toolbar divider to [view]. Overrides the automatically discovered scroll host, which
     * is ambiguous on screens hosting more than one toolbar or more than one scrollable region.
     *
     * This binding takes precedence over id-based and auto-discovered targets for as long as the
     * toolbar stays attached, even if [view] has no id or is briefly not shown.
     */
    fun setScrollTarget(view: View?) {
        explicitScrollTarget = view
        scrollTargetId = NO_ID
        scrollTarget = view
        autoDiscoveryFailureCount = 0
        updateDivider()
    }

    override fun inflateMenu(resId: Int) {
        super.inflateMenu(resId)
        decorateRenderedMenuActions()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        decorateRenderedMenuActions()
        autoDiscoveryFailureCount = 0
        refreshScrollTarget()
        updateDivider()
        updateToolbarLayoutHeight()
        viewTreeObserver.addOnScrollChangedListener(scrollChangedListener)
        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnScrollChangedListener(scrollChangedListener)
        viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
        scrollTarget = null
        explicitScrollTarget = null
        super.onDetachedFromWindow()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (!isDividerVisible) return
        canvas.drawRect(
            0f,
            (height - dividerHeight).toFloat(),
            width.toFloat(),
            height.toFloat(),
            dividerPaint,
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // ActionMenuItemView centers icon-only items from the current icon bounds during measure.
        decorateNavigationButton()
        decorateRenderedMenuActions()
        applyToolbarControlEdgeMargins()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (decorateTitleAndSubtitle() || decorateNavigationButton() || decorateRenderedMenuActions()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        applyToolbarControlEdgeMargins()
        super.onLayout(changed, left, top, right, bottom)
        decorateNavigationButton()
        decorateRenderedMenuActions()
        centerToolbarControlsVertically()
        layoutTitleAndSubtitle()
    }

    private fun applyStaticChrome() {
        setTitleCentered(false)
        setTitleTextColor(context.color(R.color.woo_ds_color_surface_on_default))
        setSubtitleTextColor(context.color(R.color.woo_ds_color_surface_on_default))
        background = context.drawable(R.drawable.woo_ds_toolbar_background)
        val edgeInset = context.dimensionPixelSize(R.dimen.woo_ds_toolbar_edge_padding)
        setContentInsetsAbsolute(edgeInset, edgeInset)
        setContentInsetStartWithNavigation(edgeInset)
        setContentInsetEndWithActions(edgeInset)
        applyConfigurationChrome()
    }

    private fun applyConfigurationChrome() {
        val titleAppearance = when (displayConfiguration) {
            Configuration.SMALL -> R.style.TextAppearance_Woo_DesignSystem_ToolbarTitle
            Configuration.MEDIUM -> R.style.TextAppearance_Woo_DesignSystem_ToolbarMediumTitle
        }
        setTitleTextAppearance(context, titleAppearance)
        setSubtitleTextAppearance(context, R.style.TextAppearance_Woo_DesignSystem_ToolbarSupportingText)
        minimumHeight = context.dimensionPixelSize(
            when (displayConfiguration) {
                Configuration.SMALL -> R.dimen.woo_ds_toolbar_height
                Configuration.MEDIUM -> R.dimen.woo_ds_toolbar_medium_height
            },
        )
    }

    private fun decorateNavigationButton(): Boolean {
        val navigationButton = children.filterIsInstance<AppCompatImageButton>().firstOrNull() ?: return false
        return navigationButton.applyFlatToolbarImageButtonStyle()
    }

    private fun decorateTitleAndSubtitle(): Boolean {
        val titleView = children.filterIsInstance<TextView>().firstOrNull { it.text == title } ?: return false
        val subtitleView = children.filterIsInstance<TextView>()
            .firstOrNull { it !== titleView && it.text == subtitle }
        val titleLineHeight = if (displayConfiguration == Configuration.MEDIUM) 32f else 24f
        return titleView.applyToolbarTextStyle(titleLineHeight) || (subtitleView?.applyToolbarTextStyle() == true)
    }

    private fun decorateRenderedMenuActions(): Boolean {
        var changed = false
        val iconSize = context.dimensionPixelSize(R.dimen.woo_ds_toolbar_icon_size)
        children
            .filterIsInstance<ActionMenuView>()
            .flatMap { actionMenuView -> actionMenuView.children.asIterable() }
            .forEach { child ->
                val layoutParams = child.layoutParams as? ActionMenuView.LayoutParams
                if (layoutParams?.isOverflowButton == true) {
                    changed = child.applyFlatToolbarImageButtonStyle() || changed
                    return@forEach
                }

                val item = menu.findItem(child.id) ?: return@forEach
                if (item.actionView === child) {
                    return@forEach
                }
                val icon = item.icon
                changed = if (icon != null && child.isIconOnlyAction()) {
                    child.applyFlatToolbarActionStyle(icon, iconSize) || changed
                } else {
                    child.clearFlatToolbarActionStyle(icon, iconSize) || changed
                }
            }
        return changed
    }

    private fun View.applyFlatToolbarImageButtonStyle(): Boolean {
        var changed = applyToolbarIconTouchTarget()
        if (this is ImageView && scaleType != ImageView.ScaleType.FIT_CENTER) {
            scaleType = ImageView.ScaleType.FIT_CENTER
            changed = true
        }
        if (getTag(R.id.woo_ds_toolbar_action_view) != true) {
            background = context.toolbarIconButtonBackground()
            setTag(R.id.woo_ds_toolbar_action_view, true)
            changed = true
        }
        return changed
    }

    private fun View.applyFlatToolbarActionStyle(icon: Drawable?, iconSize: Int): Boolean {
        if (getTag(R.id.woo_ds_toolbar_action_original_state) == null) {
            setTag(R.id.woo_ds_toolbar_action_original_state, captureToolbarActionViewState())
        }
        var changed = applyToolbarIconTouchTarget()
        if (getTag(R.id.woo_ds_toolbar_action_view) != true ||
            getTag(R.id.woo_ds_toolbar_action_icon) !== icon
        ) {
            background = context.toolbarIconButtonBackground()
            setTag(R.id.woo_ds_toolbar_action_view, true)
            setTag(R.id.woo_ds_toolbar_action_icon, icon)
            changed = true
        }

        if (this is TextView) {
            changed = applyTextViewIconStyle(iconSize) || changed
        }

        if (icon != null && (icon.bounds.width() != iconSize || icon.bounds.height() != iconSize)) {
            icon.setBounds(0, 0, iconSize, iconSize)
            changed = true
        }
        return changed
    }

    private fun View.clearFlatToolbarActionStyle(icon: Drawable?, iconSize: Int): Boolean {
        if (getTag(R.id.woo_ds_toolbar_action_view) != true) {
            return false
        }

        (getTag(R.id.woo_ds_toolbar_action_original_state) as? ToolbarActionViewState)?.restoreTo(this)
        if (this is TextView) {
            restoreToolbarActionIcon(icon, iconSize)
        }
        setTag(R.id.woo_ds_toolbar_action_view, null)
        setTag(R.id.woo_ds_toolbar_action_icon, null)
        setTag(R.id.woo_ds_toolbar_action_original_state, null)
        return true
    }

    private fun View.isIconOnlyAction(): Boolean =
        this !is TextView || text.isNullOrEmpty()

    private fun applyToolbarControlEdgeMargins() {
        val controlEdgeInset = context.dimensionPixelSize(R.dimen.woo_ds_toolbar_control_edge_padding)
        val navigationLayoutParams = children.filterIsInstance<AppCompatImageButton>()
            .firstOrNull()?.layoutParams as? ViewGroup.MarginLayoutParams
        val actionMenuLayoutParams = children.filterIsInstance<ActionMenuView>()
            .firstOrNull()?.layoutParams as? ViewGroup.MarginLayoutParams

        navigationLayoutParams?.marginStart = controlEdgeInset
        actionMenuLayoutParams?.marginEnd = controlEdgeInset
    }

    private fun centerToolbarControlsVertically() {
        val touchTarget = context.dimensionPixelSize(R.dimen.woo_ds_toolbar_icon_touch_target)
        val controlTop = when (displayConfiguration) {
            Configuration.SMALL -> ((height - touchTarget) / 2f).roundToInt()
            Configuration.MEDIUM -> context.dimensionPixelSize(R.dimen.woo_ds_toolbar_medium_control_top_padding)
        }
        children.filterIsInstance<AppCompatImageButton>().firstOrNull()?.layoutAt(controlTop)
        children.filterIsInstance<ActionMenuView>().firstOrNull()?.let { actionMenuView ->
            if (displayConfiguration == Configuration.SMALL) {
                actionMenuView.centerVertically(height)
            } else {
                actionMenuView.layoutAt(controlTop)
            }
            actionMenuView.centerFlatActionsVertically(
                toolbarHeight = height.takeIf { displayConfiguration == Configuration.SMALL },
            )
        }
    }

    private fun layoutTitleAndSubtitle() {
        val titleView = children.filterIsInstance<TextView>().firstOrNull { it.text == title } ?: return
        val subtitleView = children.filterIsInstance<TextView>()
            .firstOrNull { it !== titleView && it.text == subtitle && it.visibility != GONE }
        val (contentStart, contentEnd) = titleContentBounds()
        if (contentEnd <= contentStart) return

        val visibleTextViews = listOfNotNull(titleView, subtitleView)
        visibleTextViews.forEach { textView ->
            textView.measure(
                MeasureSpec.makeMeasureSpec(contentEnd - contentStart, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
        }
        val textHeight = visibleTextViews.sumOf { it.measuredHeight }
        val titleTop = when (displayConfiguration) {
            Configuration.SMALL -> ((height - textHeight) / 2f).roundToInt()
            Configuration.MEDIUM -> height - textHeight - context.dimensionPixelSize(
                if (subtitleView == null) {
                    R.dimen.woo_ds_toolbar_medium_title_bottom_padding
                } else {
                    R.dimen.woo_ds_toolbar_medium_supporting_text_bottom_padding
                },
            )
        }
        var top = titleTop.coerceAtLeast(0)
        visibleTextViews.forEach { textView ->
            textView.layout(contentStart, top, contentEnd, top + textView.measuredHeight)
            top += textView.measuredHeight
        }
    }

    private fun titleContentBounds(): Pair<Int, Int> {
        val controlSpacing = context.dimensionPixelSize(R.dimen.woo_ds_toolbar_title_control_spacing)
        val navigationTitleSpacing = context.dimensionPixelSize(
            R.dimen.woo_ds_toolbar_navigation_title_spacing,
        )
        val edge = context.dimensionPixelSize(R.dimen.woo_ds_toolbar_edge_padding)
        val navigationButton = children.filterIsInstance<AppCompatImageButton>().firstOrNull()
        val actionMenuView = children.filterIsInstance<ActionMenuView>().firstOrNull()
        return if (layoutDirection == LAYOUT_DIRECTION_RTL) {
            maxOf(edge, (actionMenuView?.right ?: 0) + controlSpacing) to
                minOf(width - edge, (navigationButton?.left ?: width) - navigationTitleSpacing)
        } else {
            maxOf(edge, (navigationButton?.right ?: 0) + navigationTitleSpacing) to
                minOf(width - edge, (actionMenuView?.left ?: width) - controlSpacing)
        }
    }

    private fun updateDivider() {
        val isScrolled = scrollTarget?.isScrolledFromTop() == true
        updateMediumCollapse(isScrolled)
        val visible = when (dividerMode) {
            DividerMode.ALWAYS -> true
            DividerMode.NEVER -> false
            DividerMode.AUTO -> isScrolled
        }
        if (visible != isDividerVisible) {
            isDividerVisible = visible
            invalidate()
        }
    }

    private fun updateMediumCollapse(isScrolled: Boolean) {
        val shouldCollapse = configuration == Configuration.MEDIUM && isScrolled
        if (isMediumCollapsed == shouldCollapse) return

        isMediumCollapsed = shouldCollapse
        applyConfigurationChrome()
        updateSupportingTextVisibility()
        updateToolbarLayoutHeight()
        requestLayout()
    }

    private fun updateToolbarLayoutHeight(force: Boolean = false) {
        if (!force && configuration != Configuration.MEDIUM) return
        val targetHeight = context.dimensionPixelSize(
            when (displayConfiguration) {
                Configuration.SMALL -> R.dimen.woo_ds_toolbar_height
                Configuration.MEDIUM -> R.dimen.woo_ds_toolbar_medium_height
            },
        )
        layoutParams?.let { params ->
            if (params.height != targetHeight) {
                params.height = targetHeight
                requestLayout()
            }
        }
    }

    private fun updateSupportingTextVisibility() {
        val titleView = children.filterIsInstance<TextView>().firstOrNull { it.text == title } ?: return
        val subtitleView = children.filterIsInstance<TextView>()
            .firstOrNull { it !== titleView && it.text == subtitle } ?: return
        subtitleView.visibility = if (configuration == Configuration.MEDIUM && isMediumCollapsed) GONE else VISIBLE
    }

    private fun refreshScrollTarget() {
        when {
            explicitScrollTarget != null -> scrollTarget = explicitScrollTarget
            scrollTargetId != NO_ID -> {
                if (scrollTarget?.id != scrollTargetId || scrollTarget?.isShown != true) {
                    scrollTarget = rootView.findViewById(scrollTargetId)
                }
            }
            scrollTarget?.isActiveScrollHost() != true -> {
                scrollTarget = resolveAutoDiscoveredScrollTarget()
            }
        }
    }

    // Caps consecutive failed sweeps so a Compose-only screen, which exposes no matching View-based
    // scroll host, stops re-walking the tree on every layout pass for the rest of the attach cycle.
    private fun resolveAutoDiscoveredScrollTarget(): View? {
        if (autoDiscoveryFailureCount >= MAX_AUTO_DISCOVERY_ATTEMPTS) return null
        val found = findScrollHost()
        autoDiscoveryFailureCount = if (found?.isActiveScrollHost() == true) {
            0
        } else {
            autoDiscoveryFailureCount + 1
        }
        return found
    }

    private fun findScrollHost(): View? {
        val activelyScrolling = findScrollHost { it.isActiveScrollHost() }
        return activelyScrolling ?: findScrollHost { it.isVerticalScrollHost() }
    }

    private fun findScrollHost(isEligible: (View) -> Boolean): View? {
        var searched: View = this
        var ancestor = parent as? ViewGroup
        while (ancestor != null) {
            ancestor.findVerticalScrollHost(searched, isEligible)?.let { return it }
            if (ancestor.id == android.R.id.content) return null
            searched = ancestor
            ancestor = ancestor.parent as? ViewGroup
        }
        return null
    }

    private fun ViewGroup.findVerticalScrollHost(skip: View, isEligible: (View) -> Boolean): View? {
        for (child in children) {
            if (child === skip || !child.isShown) continue
            if (isEligible(child)) return child
            (child as? ViewGroup)?.findVerticalScrollHost(skip, isEligible)?.let { return it }
        }
        return null
    }

    private fun View.isVerticalScrollHost(): Boolean = when (this) {
        is RecyclerView -> layoutManager?.canScrollVertically() == true
        is ScrollView, is NestedScrollView, is AbsListView -> true
        is ScrollingView -> canScrollVertically(-1) || canScrollVertically(1)
        else -> false
    }

    private fun View.isActiveScrollHost(): Boolean = when (this) {
        is RecyclerView -> isRecyclerScrolledFromTop() || canScrollVertically(1)
        else -> isVerticalScrollHost() && (canScrollVertically(-1) || canScrollVertically(1))
    }

    private fun View.isScrolledFromTop(): Boolean = when (this) {
        is RecyclerView -> isRecyclerScrolledFromTop()
        is AbsListView ->
            firstVisiblePosition > 0 ||
                (getChildAt(0)?.let { it.top < paddingTop } == true)
        else -> canScrollVertically(-1)
    }

    private fun RecyclerView.isRecyclerScrolledFromTop(): Boolean {
        val manager = layoutManager
        // The scan below assumes the lowest adapter position is the visual top, which does not hold for
        // reversed or non-linear layout managers, so those fall back to a state-based check instead.
        if (manager !is LinearLayoutManager || manager.reverseLayout || manager.stackFromEnd) {
            return canScrollVertically(-1)
        }

        var lowestPosition = Int.MAX_VALUE
        var topOfLowest = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index) ?: continue
            val position = getChildAdapterPosition(child)
            if (position != RecyclerView.NO_POSITION && position < lowestPosition) {
                lowestPosition = position
                topOfLowest = manager.getDecoratedTop(child)
            }
        }
        if (lowestPosition == Int.MAX_VALUE) return false
        return lowestPosition > 0 || topOfLowest < paddingTop
    }

    enum class DividerMode {
        AUTO,
        ALWAYS,
        NEVER,
    }

    enum class Configuration {
        SMALL,
        MEDIUM,
    }

    enum class TitleAlignment {
        START,
        CENTER,
    }

    private val displayConfiguration: Configuration
        get() = if (configuration == Configuration.MEDIUM && isMediumCollapsed) {
            Configuration.SMALL
        } else {
            configuration
        }

    private companion object {
        const val MAX_AUTO_DISCOVERY_ATTEMPTS = 5
    }
}

private fun View.layoutAt(top: Int) {
    layout(left, top, right, top + measuredHeight)
}

private fun View.centerVertically(parentHeight: Int) {
    val centeredTop = ((parentHeight - measuredHeight) / 2f).roundToInt()
    layout(left, centeredTop, right, centeredTop + measuredHeight)
}

private fun ActionMenuView.centerFlatActionsVertically(toolbarHeight: Int? = null) {
    children
        .filter { child -> child.getTag(R.id.woo_ds_toolbar_action_view) == true }
        .forEach { child ->
            val childTop = toolbarHeight?.let {
                ((it - child.measuredHeight) / 2f).roundToInt() - top
            } ?: ((measuredHeight - child.measuredHeight) / 2f).roundToInt()
            child.layout(child.left, childTop, child.left + child.measuredWidth, childTop + child.measuredHeight)
        }
}

private fun View.applyToolbarIconTouchTarget(): Boolean {
    var changed = false
    val touchTarget = context.dimensionPixelSize(R.dimen.woo_ds_toolbar_icon_touch_target)
    val iconPadding = context.dimensionPixelSize(R.dimen.woo_ds_toolbar_icon_padding)

    if (minimumWidth != touchTarget) {
        minimumWidth = touchTarget
        changed = true
    }
    if (minimumHeight != touchTarget) {
        minimumHeight = touchTarget
        changed = true
    }
    val currentLayoutParams = layoutParams
    if (currentLayoutParams == null) {
        layoutParams = ViewGroup.LayoutParams(touchTarget, touchTarget)
        changed = true
    } else if (currentLayoutParams.width != touchTarget || currentLayoutParams.height != touchTarget) {
        currentLayoutParams.width = touchTarget
        currentLayoutParams.height = touchTarget
        requestLayout()
        changed = true
    }
    if (!hasUniformPadding(iconPadding)) {
        setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
        changed = true
    }
    return changed
}

private fun View.hasUniformPadding(padding: Int): Boolean {
    return paddingLeft == padding &&
        paddingTop == padding &&
        paddingRight == padding &&
        paddingBottom == padding
}

private fun View.captureToolbarActionViewState(): ToolbarActionViewState =
    ToolbarActionViewState(
        background = background,
        minimumSize = ToolbarActionViewSize(minimumWidth, minimumHeight),
        layoutSize = layoutParams?.let { ToolbarActionViewSize(it.width, it.height) },
        padding = Rect(paddingLeft, paddingTop, paddingRight, paddingBottom),
        textState = (this as? TextView)?.let {
            ToolbarActionTextViewState(
                gravity = it.gravity,
                compoundDrawablePadding = it.compoundDrawablePadding,
                compoundDrawableTintList = TextViewCompat.getCompoundDrawableTintList(it),
            )
        },
    )

private data class ToolbarActionViewState(
    val background: Drawable?,
    val minimumSize: ToolbarActionViewSize,
    val layoutSize: ToolbarActionViewSize?,
    val padding: Rect,
    val textState: ToolbarActionTextViewState?,
) {
    fun restoreTo(view: View) {
        view.background = background
        view.minimumWidth = minimumSize.width
        view.minimumHeight = minimumSize.height
        view.layoutParams?.let { params ->
            layoutSize?.let {
                params.width = it.width
                params.height = it.height
            }
        }
        view.setPadding(padding.left, padding.top, padding.right, padding.bottom)
        if (view is TextView && textState != null) {
            view.gravity = textState.gravity
            view.compoundDrawablePadding = textState.compoundDrawablePadding
            TextViewCompat.setCompoundDrawableTintList(view, textState.compoundDrawableTintList)
        }
    }
}

private data class ToolbarActionViewSize(
    val width: Int,
    val height: Int,
)

private data class ToolbarActionTextViewState(
    val gravity: Int,
    val compoundDrawablePadding: Int,
    val compoundDrawableTintList: ColorStateList?,
)

private fun Context.withWooToolbarTheme(): Context {
    return ContextThemeWrapper(this, R.style.ThemeOverlay_Woo_DesignSystem_Toolbar)
}

private fun Context.drawable(@DrawableRes drawableRes: Int): Drawable? {
    return AppCompatResources.getDrawable(this, drawableRes)?.mutate()
}

private fun Context.toolbarIconButtonBackground(): Drawable =
    checkNotNull(drawable(R.drawable.woo_ds_toolbar_icon_button_background))

private fun Context.toolbarIconButtonTint(): ColorStateList =
    checkNotNull(AppCompatResources.getColorStateList(this, R.color.woo_ds_toolbar_icon_button_tint))

private fun Context.color(colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

private fun Context.dimensionPixelSize(dimenRes: Int): Int =
    resources.getDimensionPixelSize(dimenRes)

private fun TextView.applyToolbarTextStyle(lineHeightSp: Float? = null): Boolean {
    var changed = false
    if (includeFontPadding) {
        includeFontPadding = false
        changed = true
    }
    val expectedGravity = if ((parent as? WooDesignSystemToolbar)?.titleAlignment ==
        WooDesignSystemToolbar.TitleAlignment.CENTER
    ) {
        Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
    } else {
        Gravity.START or Gravity.CENTER_VERTICAL
    }
    if (gravity != expectedGravity) {
        gravity = expectedGravity
        changed = true
    }
    lineHeightSp?.let { expectedLineHeightSp ->
        val expectedLineHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            expectedLineHeightSp,
            resources.displayMetrics,
        )
        val fontHeight = paint.fontMetricsInt.descent - paint.fontMetricsInt.ascent
        setLineSpacing(expectedLineHeight - fontHeight, 1f)
    }
    return changed
}

private fun TextView.applyTextViewIconStyle(iconSize: Int): Boolean {
    var changed = false
    if (gravity != Gravity.CENTER) {
        gravity = Gravity.CENTER
        changed = true
    }
    if (compoundDrawablePadding != 0) {
        compoundDrawablePadding = 0
        changed = true
    }
    TextViewCompat.setCompoundDrawableTintList(this, context.toolbarIconButtonTint())
    return resizeCompoundDrawables(iconSize) || changed
}

private fun TextView.resizeCompoundDrawables(iconSize: Int): Boolean {
    var changed = false
    compoundDrawables
        .filterNotNull()
        .forEach { drawable ->
            if (drawable.bounds.width() != iconSize || drawable.bounds.height() != iconSize) {
                drawable.setBounds(0, 0, iconSize, iconSize)
                changed = true
            }
        }
    return changed
}

private fun TextView.restoreToolbarActionIcon(icon: Drawable?, iconSize: Int) {
    icon?.setBounds(0, 0, iconSize, iconSize)
    setCompoundDrawables(icon, null, null, null)
}

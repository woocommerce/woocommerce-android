package com.woocommerce.android.ui.compose.designsystem.component

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ActionMenuView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.google.android.material.appbar.MaterialToolbar
import com.woocommerce.android.ui.compose.designsystem.R

class WooDesignSystemToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.toolbarStyle,
) : MaterialToolbar(context.withWooToolbarTheme(), attrs, defStyleAttr) {
    init {
        applyStaticChrome()
    }

    override fun inflateMenu(resId: Int) {
        super.inflateMenu(resId)
        decorateRenderedMenuActions()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        decorateRenderedMenuActions()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // ActionMenuItemView centers icon-only items from the current icon bounds during measure.
        decorateRenderedMenuActions()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (decorateRenderedMenuActions()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        decorateRenderedMenuActions()
    }

    private fun applyStaticChrome() {
        setTitleCentered(true)
        setTitleTextAppearance(context, R.style.TextAppearance_Woo_DesignSystem_ToolbarTitle)
        setTitleTextColor(context.color(R.color.woo_ds_color_surface_on_default))
        setSubtitleTextColor(context.color(R.color.woo_ds_color_surface_on_default))
        background = context.drawable(R.drawable.woo_ds_toolbar_background)
        val edgeInset = context.dimensionPixelSize(R.dimen.woo_ds_toolbar_edge_padding)
        setContentInsetsAbsolute(edgeInset, edgeInset)
        setContentInsetStartWithNavigation(edgeInset)
        setContentInsetEndWithActions(edgeInset)
        minimumHeight = context.dimensionPixelSize(R.dimen.woo_ds_toolbar_height)
    }

    private fun decorateRenderedMenuActions(): Boolean {
        var changed = false
        val iconSize = context.dimensionPixelSize(R.dimen.woo_ds_toolbar_icon_size)
        children
            .filterIsInstance<ActionMenuView>()
            .flatMap { actionMenuView -> actionMenuView.children.asIterable() }
            .forEach { child ->
                val item = menu.findItem(child.id) ?: return@forEach
                if (item.actionView === child) {
                    return@forEach
                }
                if (item.icon != null) {
                    changed = child.applyOutlinedToolbarActionStyle(item.icon, iconSize) || changed
                }
            }
        return changed
    }

    private fun android.view.View.applyOutlinedToolbarActionStyle(icon: Drawable?, iconSize: Int): Boolean {
        var changed = false
        val backgroundIcon = icon.takeIf { shouldDrawIconInBackground() }
        if (getTag(R.id.woo_ds_toolbar_action_view) != true ||
            getTag(R.id.woo_ds_toolbar_action_icon) !== backgroundIcon
        ) {
            background = context.toolbarIconButtonBackground(backgroundIcon)
            setTag(R.id.woo_ds_toolbar_action_view, true)
            setTag(R.id.woo_ds_toolbar_action_icon, backgroundIcon)
            changed = true
        }

        if (this is TextView) {
            changed = applyTextViewIconStyle(backgroundIcon, iconSize) || changed
        }

        if (icon != null && (icon.bounds.width() != iconSize || icon.bounds.height() != iconSize)) {
            icon.setBounds(0, 0, iconSize, iconSize)
            changed = true
        }
        return changed
    }

    private fun android.view.View.shouldDrawIconInBackground(): Boolean =
        this !is TextView || text.isNullOrEmpty()
}

private class CenteredToolbarIconButtonDrawable(
    icon: Drawable?,
    private val spec: CenteredToolbarIconButtonSpec,
) : Drawable() {
    private val icon = icon?.newMutableDrawable()?.apply {
        setTintList(spec.iconTint)
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = spec.strokeWidth
        color = spec.color
    }
    private val rect = RectF()

    override fun draw(canvas: Canvas) {
        centeredSquareIn(bounds, spec.boxSize, rect)
        rect.inset(spec.strokeWidth / 2, spec.strokeWidth / 2)
        canvas.drawRoundRect(rect, spec.cornerRadius, spec.cornerRadius, paint)

        icon?.let { drawable ->
            val iconLeft = bounds.left + (bounds.width() - spec.iconSize) / 2
            val iconTop = bounds.top + (bounds.height() - spec.iconSize) / 2
            drawable.setBounds(
                iconLeft.toInt(),
                iconTop.toInt(),
                (iconLeft + spec.iconSize).toInt(),
                (iconTop + spec.iconSize).toInt(),
            )
            drawable.draw(canvas)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun onStateChange(state: IntArray): Boolean {
        icon?.state = state
        icon?.setTint(spec.iconTint.getColorForState(state, spec.iconTint.defaultColor))
        invalidateSelf()
        return true
    }

    override fun isStateful(): Boolean =
        spec.iconTint.isStateful || icon?.isStateful == true || super.isStateful()

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

private data class CenteredToolbarIconButtonSpec(
    val color: Int,
    val iconTint: ColorStateList,
    val iconSize: Float,
    val boxSize: Float,
    val cornerRadius: Float,
    val strokeWidth: Float,
)

private class CenteredToolbarIconButtonMaskDrawable(
    private val boxSize: Float,
    private val cornerRadius: Float,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }
    private val rect = RectF()

    override fun draw(canvas: Canvas) {
        centeredSquareIn(bounds, boxSize, rect)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

private fun centeredSquareIn(bounds: Rect, boxSize: Float, out: RectF) {
    val size = boxSize.coerceAtMost(bounds.width().toFloat()).coerceAtMost(bounds.height().toFloat())
    val left = bounds.left + (bounds.width() - size) / 2
    val top = bounds.top + (bounds.height() - size) / 2
    out.set(left, top, left + size, top + size)
}

private fun Context.withWooToolbarTheme(): Context {
    return ContextThemeWrapper(this, R.style.ThemeOverlay_Woo_DesignSystem_Toolbar)
}

private fun Context.drawable(@DrawableRes drawableRes: Int): Drawable? {
    return AppCompatResources.getDrawable(this, drawableRes)?.mutate()
}

private fun Context.toolbarIconButtonBackground(icon: Drawable?): Drawable {
    val boxSize = resources.getDimension(R.dimen.woo_ds_toolbar_icon_button_size)
    val cornerRadius = resources.getDimension(R.dimen.woo_ds_toolbar_icon_corner_radius)
    return RippleDrawable(
        ColorStateList.valueOf(color(R.color.woo_ds_color_overlay_overlay20)),
        CenteredToolbarIconButtonDrawable(
            icon = icon,
            spec = CenteredToolbarIconButtonSpec(
                color = color(R.color.woo_ds_color_outline_variant),
                iconTint = AppCompatResources.getColorStateList(this, R.color.woo_ds_toolbar_icon_button_tint),
                iconSize = resources.getDimension(R.dimen.woo_ds_toolbar_icon_size),
                boxSize = boxSize,
                cornerRadius = cornerRadius,
                strokeWidth = resources.getDimension(R.dimen.woo_ds_toolbar_icon_border_width),
            ),
        ),
        CenteredToolbarIconButtonMaskDrawable(
            boxSize = boxSize,
            cornerRadius = cornerRadius,
        ),
    )
}

private fun Context.color(colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

private fun Context.dimensionPixelSize(dimenRes: Int): Int =
    resources.getDimensionPixelSize(dimenRes)

private fun Drawable.newMutableDrawable(): Drawable =
    constantState?.newDrawable()?.mutate() ?: mutate()

private fun TextView.applyTextViewIconStyle(backgroundIcon: Drawable?, iconSize: Int): Boolean {
    var changed = false
    if (gravity != Gravity.CENTER) {
        gravity = Gravity.CENTER
        changed = true
    }
    if (compoundDrawablePadding != 0) {
        compoundDrawablePadding = 0
        changed = true
    }
    return if (backgroundIcon != null) {
        clearCompoundDrawables() || changed
    } else {
        resizeCompoundDrawables(iconSize) || changed
    }
}

private fun TextView.clearCompoundDrawables(): Boolean {
    if (compoundDrawables.none { drawable -> drawable != null }) {
        return false
    }
    setCompoundDrawables(null, null, null, null)
    return true
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

package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * Special RecyclerView ItemDecorator to draw a divider that aligns to the beginning, end, or both of a child element
 * inside a list view element.
 *
 * Define an element to align the begging of the divider with:
 *
 * +-------------------------+
 * |ImageView TextView Button|
 * |          ---------------|<-- Divider aligns with second element (R.id.text2)
 * |ImageView TextView Button|
 * |          ---------------|
 * |ImageView TextView Button|
 * |          ---------------|
 * |ImageView TextView Button|
 * +_________________________+
 *
 * You would create an instance of this class by feeding the [alignStartToStartOf] with the id of this view:
 * `AlignedDividerDecoration(context, DividerItemDecoration.VERTICAL, R.id.text2))`
 *
 * Define an element to align the start and end of the divider and respect the margins:
 *
 * +-------------------------+
 * |ImageView TextView Button|
 * |          --------       |<-- Divider aligns with second element (R.id.text2)
 * |ImageView TextView Button|
 * |          --------       |
 * |ImageView TextView Button|
 * |          --------       |
 * |ImageView TextView Button|
 * +_________________________+
 *
 * You would create an instance of this class by feeding the [alignStartToStartOf] and [alignEndToEndOf] with the id
 * of this view:
 * `AlignedDividerDecoration(context, DividerItemDecoration.VERTICAL, R.id.text2, R.id.text2, true))`
 *
 * For Horizontal lists, the start = top, and end = bottom.
 *
 * @param [ctx] The active context
 * @param [orientation] The orientation of the list. Either [AlignedDividerDecoration.HORIZONTAL]
 * or [AlignedDividerDecoration.VERTICAL]
 * @param [alignStartToStartOf] Optional. The resource ID of the component in the list view to align the start of the
 * divider with. If not provided, the start will be aligned with the start of the parent.
 * @param [alignEndToEndOf] Optional. The resource ID of the component in the list item view to align the end of the
 * divider with. If not provided, the end will be aligned with the end of the parent.
 * @param [clipToMargin] True if the divider should also clip itself to match the margins of the provided components.
 * Default is false.
 */
class AlignedDividerDecoration @JvmOverloads constructor(
    ctx: Context,
    private val orientation: Int,
    private val alignStartToStartOf: Int = 0,
    private val alignEndToEndOf: Int = 0,
    private val clipToMargin: Boolean = false,
    private val padding: Int = 0
)
    : RecyclerView.ItemDecoration() {
    companion object {
        const val HORIZONTAL = 0
        const val VERTICAL = 1
    }
    init {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw IllegalArgumentException("Invalid orientation. It should either be HORIZONTAL or VERTICAL.")
        }
        val attrs = intArrayOf(android.R.attr.listDivider)
        val a = ctx.obtainStyledAttributes(attrs)
        a.getDrawable(0)?.let { setDrawable(it) }
        a.recycle()
    }
    lateinit var divider: Drawable

    private val bounds = Rect()

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null) {
            return
        }

        if (orientation == VERTICAL) {
            drawForVertical(canvas, parent)
        } else {
            drawForHorizontal(canvas, parent)
        }
    }

    private fun drawForVertical(canvas: Canvas, parent: RecyclerView) {
        val adjustedChildCount = parent.childCount - 2
        val isRtl = ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_RTL
        (0..adjustedChildCount)
                .map { parent.getChildAt(it) }
                .forEach {
                    val left = it.findViewById<View>(if (isRtl) alignEndToEndOf else alignStartToStartOf)
                    val right = it.findViewById<View>(if (isRtl) alignStartToStartOf else alignEndToEndOf)

                    var dividerStart = left?.left ?: 0
                    var dividerEnd = right?.right ?: parent.width
                    if (clipToMargin) {
                        (left?.layoutParams as ConstraintLayout.LayoutParams?)?.let {
                            dividerStart += it.marginStart
                        }
                        (right?.layoutParams as ConstraintLayout.LayoutParams?)?.let {
                            dividerEnd -= it.marginEnd
                        }
                    }

                    parent.getDecoratedBoundsWithMargins(it, bounds)
                    val bottom = bounds.bottom + Math.round(it.translationY)
                    val top = bottom - divider.intrinsicHeight
                    divider.setBounds(dividerStart, top, dividerEnd, bottom)
                    divider.draw(canvas)
                }
    }

    private fun drawForHorizontal(canvas: Canvas, parent: RecyclerView) {
        val adjustedChildCount = parent.childCount - 2
        (0..adjustedChildCount)
                .map { parent.getChildAt(it) }
                .forEach {
                    val clipStartView = it.findViewById<View>(alignStartToStartOf)
                    val clipEndView = it.findViewById<View>(alignEndToEndOf)

                    var top = clipStartView?.top ?: 0
                    var bottom = clipEndView?.bottom ?: parent.height

                    // Calculate margins if enabled
                    if (clipToMargin) {
                        (clipStartView?.layoutParams as ConstraintLayout.LayoutParams?)?.let {
                            top += it.marginStart
                        }
                        (clipEndView?.layoutParams as ConstraintLayout.LayoutParams?)?.let {
                            bottom -= it.marginEnd
                        }
                    }

                    parent.layoutManager?.getDecoratedBoundsWithMargins(it, bounds)
                    val right = bounds.right + Math.round(it.translationX)
                    val left = right - divider.intrinsicWidth
                    divider.setBounds(left, top, right, bottom)
                    divider.draw(canvas)
                }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (padding > 0) {
            if (orientation == VERTICAL) {
                outRect.set(0, 0, 0, padding)
            } else {
                outRect.set(0, 0, padding, 0)
            }
        } else {
            if (orientation == VERTICAL) {
                outRect.set(0, 0, 0, divider.intrinsicHeight)
            } else {
                outRect.set(0, 0, divider.intrinsicWidth, 0)
            }
        }
    }

    fun setDrawable(drawable: Drawable) {
        divider = drawable
    }
}

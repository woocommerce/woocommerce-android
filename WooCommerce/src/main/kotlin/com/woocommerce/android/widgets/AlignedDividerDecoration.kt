package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.View

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
 * Define an element to align the end of the divider with:
 *
 * +-------------------------+
 * |ImageView TextView Button|
 * |          --------        <-- Divider aligns with second element (R.id.text2)
 * |ImageView TextView Button|
 * |          --------       |
 * |ImageView TextView Button|
 * |          --------       |
 * |ImageView TextView Button|
 * +_________________________+
 *
 * You would create an instance of this class by feeding the [alignEndToEndOf] with the id of this view:
 * `AlignedDividerDecoration(context, DividerItemDecoration.VERTICAL, R.id.text2))`
 *
 * For Horizontal lists, the start = top, and end = bottom.
 */
class AlignedDividerDecoration @JvmOverloads constructor(
        ctx: Context, val orientation: Int, val alignStartToStartOf: Int = 0, val alignEndToEndOf: Int = 0)
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
        setDrawable(a.getDrawable(0))
        a.recycle()
    }
    lateinit var divider: Drawable

    private val bounds = Rect()

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
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
        (0..adjustedChildCount)
                .map { parent.getChildAt(it) }
                .forEach {
                    val clipStartView = it.findViewById<View>(alignStartToStartOf)
                    val clipEndView = it.findViewById<View>(alignEndToEndOf)

                    val left = clipStartView?.left ?: 0
                    val right = clipEndView?.right ?: parent.width

                    parent.getDecoratedBoundsWithMargins(it, bounds)
                    val bottom = bounds.bottom + Math.round(it.translationY)
                    val top = bottom - divider.intrinsicHeight
                    divider.setBounds(left, top, right, bottom)
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

                    val top = clipStartView?.top ?: 0
                    val bottom = clipEndView?.bottom ?: parent.height

                    parent.layoutManager.getDecoratedBoundsWithMargins(it, bounds)
                    val right = bounds.right + Math.round(it.translationX)
                    val left = right - divider.intrinsicWidth
                    divider.setBounds(left, top, right, bottom)
                    divider.draw(canvas)
                }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView?, state: RecyclerView.State?) {
        if (orientation == VERTICAL) {
            outRect.set(0, 0, 0, divider.intrinsicHeight)
        } else {
            outRect.set(0, 0, divider.intrinsicWidth, 0)
        }
    }

    fun setDrawable(drawable: Drawable) {
        if (drawable == null) {
            throw IllegalArgumentException("Drawable cannot be null.")
        }
        divider = drawable
    }
}

package com.woocommerce.android.ui.orders.list

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.OrderStatusUpdateSource
import kotlin.math.roundToInt

class SwipeToComplete(
    private val context: Context,
    private val listener: OnSwipeListener
) : ItemTouchHelper.SimpleCallback(
    0,
    ItemTouchHelper.LEFT
) {
    companion object {
        private const val NO_SWIPE_ABLE_SCREEN_PERCENT = 0.10
        const val OLD_STATUS = "old_status"
        const val SWIPE_THRESHOLD = .5F
        const val SWIPE_VELOCITY_THRESHOLD = 0F
    }

    private val displayMetrics = context.resources.displayMetrics
    private val swipeAbleColor = ContextCompat.getColor(context, R.color.color_primary)
    private val noSwipeAbleColor = ContextCompat.getColor(context, R.color.color_on_surface_disabled)
    private val width = (displayMetrics.widthPixels / displayMetrics.density).toInt().dp
    private val completeIcon = ContextCompat.getDrawable(context, R.drawable.ic_checkmark_white_24dp)?.apply {
        setTint(Color.WHITE)
    }
    private val messageSize = context.resources.getDimension(R.dimen.text_minor_125)
    private val message = context.resources.getString(R.string.orderlist_mark_completed)
    private val messagePaint: TextPaint = TextPaint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = Color.WHITE
        textSize = messageSize
    }
    private val nonSwipeAbleBackgroundPaint = Paint().apply { color = noSwipeAbleColor }
    private val swipeAbleBackgroundPaint = Paint().apply { color = swipeAbleColor }

    private val margin = context.resources.getDimension(R.dimen.major_100).toInt()

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = SWIPE_THRESHOLD
    override fun getSwipeEscapeVelocity(defaultValue: Float) = SWIPE_VELOCITY_THRESHOLD
    override fun getSwipeVelocityThreshold(defaultValue: Float) = SWIPE_VELOCITY_THRESHOLD

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.absoluteAdapterPosition
        val isSwipeAble = (viewHolder as SwipeAbleViewHolder).isSwipeAble()
        if (isSwipeAble) {
            val orderId = viewHolder.getSwipedItemId()
            val oldStatus = viewHolder.getSwipedExtras()[OLD_STATUS] ?: run {
                // don't perform swipe if old status is null
                viewHolder.bindingAdapter?.notifyItemChanged(pos)
                return
            }
            val gestureSource = OrderStatusUpdateSource.SwipeToCompleteGesture(
                orderId = orderId,
                oldStatus = oldStatus,
                orderPosition = pos
            )
            listener.onSwiped(gestureSource)
        } else {
            viewHolder.bindingAdapter?.notifyItemChanged(pos)
        }
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return if (viewHolder is SwipeAbleViewHolder) {
            super.getMovementFlags(recyclerView, viewHolder)
        } else {
            makeMovementFlags(0, 0)
        }
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val isSwipeAble = (viewHolder as SwipeAbleViewHolder).isSwipeAble()
        if (isSwipeAble) {
            swipeAbleChildrenDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        } else {
            noSwipeAbleChildrenDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    @Suppress("LongParameterList")
    private fun noSwipeAbleChildrenDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val maxDX = (width * NO_SWIPE_ABLE_SCREEN_PERCENT).toFloat()
        val shrinkDX = if (dX > 0) dX.coerceAtMost(maxDX) else dX.coerceAtLeast(-maxDX)
        val background = Rect(
            viewHolder.itemView.left,
            viewHolder.itemView.top,
            viewHolder.itemView.right,
            viewHolder.itemView.bottom
        )
        canvas.drawRect(background, nonSwipeAbleBackgroundPaint)
        super.onChildDraw(canvas, recyclerView, viewHolder, shrinkDX, dY, actionState, isCurrentlyActive)
    }

    @Suppress("LongParameterList")
    private fun swipeAbleChildrenDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        // Draw background
        val background = Rect(
            viewHolder.itemView.left,
            viewHolder.itemView.top,
            viewHolder.itemView.right,
            viewHolder.itemView.bottom
        )
        canvas.drawRect(background, swipeAbleBackgroundPaint)
        val isSwipingRight = dX > 0

        // Select the longest line to measure the text width
        val longLine = message.split("\n").maxOfWith(compareBy { it.length }) { it }

        // Measure text width to place icon aligned to message center
        val messageWidth = messagePaint.measureText(longLine).toInt()
        var iconBottom = 0

        // Draw icon
        completeIcon?.let { icon ->
            val xCenteredInMessage = (messageWidth / 2f) - (completeIcon.intrinsicWidth / 2f)

            // Small adjustment for checkmark icon center alignment
            val iconAdjustment = 3.dp

            val iconRect = if (isSwipingRight) {
                Rect(
                    (xCenteredInMessage + margin + iconAdjustment).toInt(),
                    viewHolder.itemView.top + margin,
                    (completeIcon.intrinsicWidth + xCenteredInMessage + margin + iconAdjustment).toInt(),
                    viewHolder.itemView.top + completeIcon.intrinsicHeight + margin
                )
            } else {
                Rect(
                    (width - completeIcon.intrinsicWidth - xCenteredInMessage - margin + iconAdjustment).toInt(),
                    viewHolder.itemView.top + margin,
                    (width - xCenteredInMessage - margin + iconAdjustment).toInt(),
                    viewHolder.itemView.top + completeIcon.intrinsicHeight + margin
                )
            }
            iconBottom = iconRect.height()
            icon.run {
                bounds = iconRect
                draw(canvas)
            }
        }

        val textLayout = getTextStaticLayout(messageWidth)

        canvas.save()

        val textX = if (isSwipingRight) margin.toFloat() else (width - messageWidth - margin).toFloat()
        val textY = (viewHolder.itemView.top + margin + iconBottom + 4.dp).toFloat()

        canvas.translate(textX, textY)
        textLayout.draw(canvas)

        canvas.restore()

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun getTextStaticLayout(width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(message, 0, message.length, messagePaint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
    }

    interface SwipeAbleViewHolder {
        companion object {
            const val EMPTY_SWIPED_ID = -1L
        }

        fun isSwipeAble(): Boolean
        fun getSwipedItemId(): Long
        fun getSwipedExtras(): Map<String, String>
    }

    interface OnSwipeListener {
        fun onSwiped(gestureSource: OrderStatusUpdateSource.SwipeToCompleteGesture)
    }

    private val Int.dp
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            toFloat(),
            context.resources.displayMetrics
        ).roundToInt()
}

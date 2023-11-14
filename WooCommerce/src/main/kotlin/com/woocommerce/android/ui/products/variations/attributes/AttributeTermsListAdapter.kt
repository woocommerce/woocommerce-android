package com.woocommerce.android.ui.products.variations.attributes

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AttributeTermListItemBinding
import com.woocommerce.android.ui.products.variations.attributes.AttributeTermsListAdapter.TermViewHolder

typealias OnLoadMore = () -> Unit

/**
 * Adapter which shows a simple list of attribute term names
 */
class AttributeTermsListAdapter(
    private val enableDragAndDrop: Boolean,
    private val enableDeleting: Boolean,
    private val defaultItemBackground: TypedValue,
    private val dragHelper: ItemTouchHelper? = null,
    private val loadMoreListener: OnLoadMore? = null
) : RecyclerView.Adapter<TermViewHolder>() {
    interface OnTermListener {
        fun onTermClick(termName: String)
        fun onTermDelete(termName: String)
        fun onTermMoved(fromTermName: String, toTermName: String)
    }
    private lateinit var onTermListener: OnTermListener

    var termNames: ArrayList<String> = ArrayList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(
                TermItemDiffUtil(
                    field,
                    value
                ),
                true
            )
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun getItemCount() = termNames.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TermViewHolder {
        return TermViewHolder(
            AttributeTermListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TermViewHolder, position: Int) {
        holder.bind(termNames[position])

        loadMoreListener
            ?.takeIf { position == itemCount - 1 }
            ?.invoke()
    }

    fun setOnTermListener(listener: OnTermListener) {
        onTermListener = listener
    }

    fun isEmpty() = termNames.isEmpty()

    fun clear() {
        if (!isEmpty()) {
            termNames.clear()
            notifyDataSetChanged()
        }
    }

    fun containsTerm(termName: String): Boolean {
        termNames.forEach { term ->
            if (term.equals(termName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun addTerm(termName: String) {
        if (!containsTerm(termName)) {
            termNames.add(0, termName)
            notifyItemInserted(0)
            if (itemCount == 2) {
                delayedChangeNotification()
            }
        }
    }

    fun removeTerm(term: String) {
        val index = termNames.indexOf(term)
        if (index >= 0) {
            termNames.remove(term)
            notifyItemRemoved(index)
            if (itemCount == 1) {
                delayedChangeNotification()
            }
        }
    }

    /**
     * When the list changes from/to a single term we must refresh all the views since we only show the drag
     * handle when there's more than one term, but we delay the refresh to give the added/removed term time
     * to animate
     */
    private fun delayedChangeNotification() {
        Handler(Looper.getMainLooper()).postDelayed(
            {
                notifyDataSetChanged()
            },
            300
        )
    }

    fun swapItems(from: Int, to: Int) {
        val fromValue = termNames[from]
        val toValue = termNames[to]

        termNames[from] = toValue
        termNames[to] = fromValue
        notifyItemMoved(from, to)

        onTermListener.onTermMoved(fromValue, toValue)
    }

    private class TermItemDiffUtil(
        val oldList: List<String>,
        val newList: List<String>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition] == newList[newItemPosition]

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            areItemsTheSame(oldItemPosition, newItemPosition)
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class TermViewHolder(val viewBinding: AttributeTermListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        init {
            viewBinding.root.setOnClickListener {
                termNames.getOrNull(bindingAdapterPosition)?.let {
                    onTermListener.onTermClick(it)
                }
            }

            if (enableDeleting) {
                viewBinding.termContainer.setBackgroundResource(defaultItemBackground.resourceId)
                viewBinding.termDelete.setOnClickListener {
                    termNames.getOrNull(bindingAdapterPosition)?.let {
                        removeTerm(it)
                        onTermListener.onTermDelete(it)
                    }
                }
            }

            if (enableDragAndDrop && termNames.size > 1) {
                viewBinding.termDragHandle.setOnClickListener {
                    dragHelper?.startDrag(this)
                }
            }
        }

        fun bind(termName: String) {
            viewBinding.termName.text = termName
            viewBinding.termDragHandle.isVisible = enableDragAndDrop && termNames.size > 1
            viewBinding.termDelete.isVisible = enableDeleting
        }
    }
}

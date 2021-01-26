package com.woocommerce.android.ui.products.downloads

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductDownloadsListItemBinding
import com.woocommerce.android.model.ProductFile
import com.woocommerce.android.ui.products.downloads.ProductDownloadsAdapter.ProductDownloadableFileViewHolder

class ProductDownloadsAdapter(
    private val clickListener: (ProductFile) -> Unit,
    private val dragHelper: ItemTouchHelper
) : RecyclerView.Adapter<ProductDownloadableFileViewHolder>() {
    var filesList: List<ProductFile> = ArrayList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(
                ProductFileDiffUtil(
                    field,
                    value
                ), true)
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun getItemCount(): Int = filesList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductDownloadableFileViewHolder {
        return ProductDownloadableFileViewHolder(
            ProductDownloadsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ProductDownloadableFileViewHolder, position: Int) {
        holder.bind(filesList[position])
    }

    inner class ProductDownloadableFileViewHolder(val viewBinding: ProductDownloadsListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(file: ProductFile) {
            viewBinding.productDownloadItemName.text = file.name
            viewBinding.productDownloadItemUrl.text = file.url
            viewBinding.root.setOnClickListener { clickListener.invoke(file) }
            viewBinding.productDownloadItemDragHandle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    dragHelper.startDrag(this)
                }
                false
            }
        }
    }

    private class ProductFileDiffUtil(
        private val oldList: List<ProductFile>,
        private val newList: List<ProductFile>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            areItemsTheSame(oldItemPosition, newItemPosition)
    }
}

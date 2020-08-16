package com.woocommerce.android.ui.products

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R.layout
import com.woocommerce.android.model.ProductFile
import com.woocommerce.android.ui.products.ProductDownloadsAdapter.ProductDownloadableFileViewHolder
import kotlinx.android.synthetic.main.product_downloads_list_item.view.*

class ProductDownloadsAdapter(
    private val clickListener: (ProductFile) -> Unit,
    private val dragHelper: ItemTouchHelper
) : RecyclerView.Adapter<ProductDownloadableFileViewHolder>() {
    var filesList: List<ProductFile> = ArrayList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(ProductFileDiffUtil(field, value), true)
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductDownloadableFileViewHolder {
        return ProductDownloadableFileViewHolder(
            LayoutInflater.from(parent.context).inflate(layout.product_downloads_list_item, parent, false)
        )
    }

    override fun getItemCount(): Int = filesList.size

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ProductDownloadableFileViewHolder, position: Int) {
        holder.fileName.text = filesList[position].name
        holder.fileUrl.text = filesList[position].url
        holder.itemView.setOnClickListener { clickListener.invoke(filesList[position]) }
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                dragHelper.startDrag(holder)
            }
            false
        }
    }

    class ProductDownloadableFileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dragHandle: ImageView = view.product_download_item_drag_handle
        val fileName: TextView = view.product_download_item_name
        val fileUrl: TextView = view.product_download_item_url
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

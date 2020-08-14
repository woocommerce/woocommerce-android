package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R.layout
import com.woocommerce.android.model.ProductFile
import com.woocommerce.android.ui.products.ProductDownloadsAdapter.ProductDownloadableFileViewHolder
import kotlinx.android.synthetic.main.product_downloads_list_item.view.*

class ProductDownloadsAdapter(private val clickListener: (ProductFile) -> Unit) : RecyclerView.Adapter<ProductDownloadableFileViewHolder>() {
    var filesList: List<ProductFile> = ArrayList()
        set(value) {
            if (value != field) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductDownloadableFileViewHolder {
        return ProductDownloadableFileViewHolder(
            LayoutInflater.from(parent.context).inflate(layout.product_downloads_list_item, parent, false)
        )
    }

    override fun getItemCount(): Int = filesList.size

    override fun onBindViewHolder(holder: ProductDownloadableFileViewHolder, position: Int) {
        holder.fileName.text = filesList[position].name
        holder.fileUrl.text = filesList[position].url
        holder.itemView.setOnClickListener { clickListener.invoke(filesList[position]) }
    }

    class ProductDownloadableFileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.product_download_item_name
        val fileUrl: TextView = view.product_download_item_url
    }
}

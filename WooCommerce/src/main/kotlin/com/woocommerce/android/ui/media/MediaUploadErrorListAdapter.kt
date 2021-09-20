package com.woocommerce.android.ui.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.MediaUploadErrorItemBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.ui.media.MediaUploadErrorListAdapter.MediaUploadErrorListItemViewHolder
import com.woocommerce.android.ui.media.MediaUploadErrorListViewModel.ErrorUiModel
import java.io.File

class MediaUploadErrorListAdapter : RecyclerView.Adapter<MediaUploadErrorListItemViewHolder>() {
    var mediaErrorList: List<ErrorUiModel> = ArrayList()
        set(value) {
            val diffUtil = MediaUploadErrorDiffUtil(field, value)
            val diffResult = DiffUtil.calculateDiff(diffUtil, true)
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun getItemCount(): Int = mediaErrorList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaUploadErrorListItemViewHolder {
        return MediaUploadErrorListItemViewHolder(
            MediaUploadErrorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: MediaUploadErrorListItemViewHolder, position: Int) {
        holder.bind(mediaErrorList[position])
    }

    private class MediaUploadErrorDiffUtil(
        private val oldList: List<ErrorUiModel>,
        private val newList: List<ErrorUiModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            areItemsTheSame(oldItemPosition, newItemPosition)
    }

    class MediaUploadErrorListItemViewHolder(val viewBinding: MediaUploadErrorItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(model: ErrorUiModel) {
            with(model) {
                viewBinding.mediaFileName.text = fileName
                viewBinding.mediaFileErrorText.text = errorMessage
                if (filePath.isNotBlank()) {
                    GlideApp.with(viewBinding.root.context)
                        .load(File(filePath))
                        .into(viewBinding.productImage)
                }
            }
        }
    }
}

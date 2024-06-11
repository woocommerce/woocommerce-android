package com.woocommerce.android.ui.payments.cardreader.connect.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.CardReaderConnectReaderItemBinding
import com.woocommerce.android.databinding.CardReaderConnectScanningItemBinding
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderConnectViewModel.ListItemViewState
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderConnectViewModel.ListItemViewState.CardReaderListItem
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderConnectViewModel.ListItemViewState.ScanningInProgressListItem
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.util.WooAnimUtils

sealed class MultipleCardReadersFoundViewHolder(
    val parent: ViewGroup,
    @LayoutRes layout: Int
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: ListItemViewState)

    class CardReaderViewHolder(
        parent: ViewGroup
    ) : MultipleCardReadersFoundViewHolder(parent, R.layout.card_reader_connect_reader_item) {
        var binding: CardReaderConnectReaderItemBinding = CardReaderConnectReaderItemBinding.bind(itemView)

        override fun onBind(uiState: ListItemViewState) {
            uiState as CardReaderListItem
            UiHelpers.setTextOrHide(binding.readersFoundReaderConnectButton, uiState.connectLabel)
            binding.readersFoundContainer.setOnClickListener {
                uiState.onConnectClicked()
            }
            UiHelpers.setTextOrHide(binding.readersFoundReaderId, uiState.readerId)
            UiHelpers.setTextOrHide(binding.readersFoundReaderType, uiState.readerType)
        }
    }

    class ScanningInProgressViewHolder(
        parent: ViewGroup
    ) : MultipleCardReadersFoundViewHolder(parent, R.layout.card_reader_connect_scanning_item) {
        var binding: CardReaderConnectScanningItemBinding = CardReaderConnectScanningItemBinding.bind(itemView)

        init {
            WooAnimUtils.rotate(binding.cardReaderConnectProgressIndicator)
        }

        override fun onBind(uiState: ListItemViewState) {
            uiState as ScanningInProgressListItem
            UiHelpers.setTextOrHide(binding.cardReaderConnectProgressLabel, uiState.label)
            UiHelpers.setImageOrHideInLandscapeOnNonExpandedScreenSizes(
                binding.cardReaderConnectProgressIndicator,
                uiState.scanningIcon
            )
        }
    }
}

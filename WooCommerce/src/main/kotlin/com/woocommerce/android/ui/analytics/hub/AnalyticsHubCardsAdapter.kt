package com.woocommerce.android.ui.analytics.hub

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsHubInformationCardView
import com.woocommerce.android.ui.analytics.hub.listcard.AnalyticsHubListCardView

class AnalyticsHubCardsAdapter : RecyclerView.Adapter<AnalyticsHubCardsViewHolder>() {
    companion object {
        private const val VIEW_TYPE_INFORMATION = 0
        private const val VIEW_TYPE_LIST = 1
    }

    private val params = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

    var cardList: List<AnalyticsCardViewState> = ArrayList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(
                AnalyticsHubCardsDiffUtil(
                    field,
                    value
                ),
                true
            )
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnalyticsHubCardsViewHolder {
        return when (viewType) {
            VIEW_TYPE_INFORMATION -> AnalyticsHubCardsInformationViewHolder(
                AnalyticsHubInformationCardView(parent.context).apply {
                    layoutParams = params
                    elevation = resources.getDimension(R.dimen.minor_25)
                }
            )

            VIEW_TYPE_LIST -> AnalyticsHubCardsListViewHolder(
                AnalyticsHubListCardView(parent.context).apply {
                    layoutParams = params
                    elevation = resources.getDimension(R.dimen.minor_25)
                }
            )

            else -> throw IllegalArgumentException("Unexpected viewType in AnalyticsHubCardsAdapter")
        }
    }

    override fun getItemCount(): Int = cardList.size

    override fun onBindViewHolder(holder: AnalyticsHubCardsViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_INFORMATION -> {
                val state = cardList[position] as AnalyticsHubInformationViewState
                (holder as AnalyticsHubCardsInformationViewHolder).bind(state)
            }

            VIEW_TYPE_LIST -> {
                val state = cardList[position] as AnalyticsHubListViewState
                (holder as AnalyticsHubCardsListViewHolder).bind(state)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (cardList[position]) {
            is AnalyticsHubListViewState -> VIEW_TYPE_LIST
            is AnalyticsHubInformationViewState -> VIEW_TYPE_INFORMATION
        }
    }

    private class AnalyticsHubCardsDiffUtil(
        val oldList: List<AnalyticsCardViewState>,
        val newList: List<AnalyticsCardViewState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].card == newList[newItemPosition].card

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

abstract class AnalyticsHubCardsViewHolder(cardView: View) : RecyclerView.ViewHolder(cardView)

class AnalyticsHubCardsListViewHolder(private val cardView: AnalyticsHubListCardView) :
    AnalyticsHubCardsViewHolder(cardView) {
    fun bind(state: AnalyticsHubListViewState) {
        cardView.updateInformation(state)
    }
}

class AnalyticsHubCardsInformationViewHolder(private val cardView: AnalyticsHubInformationCardView) :
    AnalyticsHubCardsViewHolder(cardView) {
    fun bind(state: AnalyticsHubInformationViewState) {
        cardView.updateInformation(state)
    }
}

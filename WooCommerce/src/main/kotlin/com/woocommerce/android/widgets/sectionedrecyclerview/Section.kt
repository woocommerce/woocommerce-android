package com.woocommerce.android.widgets.sectionedrecyclerview

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.View
import com.woocommerce.android.widgets.sectionedrecyclerview.Section.State.EMPTY
import com.woocommerce.android.widgets.sectionedrecyclerview.Section.State.FAILED
import com.woocommerce.android.widgets.sectionedrecyclerview.Section.State.LOADED
import com.woocommerce.android.widgets.sectionedrecyclerview.Section.State.LOADING
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionParameters.Builder
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionedRecyclerViewAdapter.EmptyViewHolder

/**
 * Abstract Section to be used with [SectionedRecyclerViewAdapter].
 *
 * Original version: https://github.com/luizgrp/SectionedRecyclerViewAdapter
 */
abstract class Section(sectionParameters: SectionParameters) {
    var state = LOADED
        set(state) {
            when (state) {
                LOADING -> if (loadingResourceId == null) {
                    throw IllegalStateException("Missing 'loading mState' resource id")
                }
                FAILED -> if (failedResourceId == null) {
                    throw IllegalStateException("Missing 'failed mState' resource id")
                }
                EMPTY -> if (emptyResourceId == null) {
                    throw IllegalStateException("Missing 'empty mState' resource id")
                }
            }

            field = state
        }

    var isVisible = true

    internal var mHasHeader = false
    internal var mHasFooter = false

    /**
     * Return the layout resource id of the header
     * @return layout resource id of the header
     */
    @LayoutRes val headerResourceId: Int? = sectionParameters.headerResourceId
    /**
     * Return the layout resource id of the footer
     * @return layout resource id of the footer
     */
    @LayoutRes val footerResourceId: Int? = sectionParameters.footerResourceId

    /**
     * Return the layout resource id of the item
     * @return layout resource id of the item
     */
    @LayoutRes val itemResourceId: Int = sectionParameters.itemResourceId

    /**
     * Return the layout resource id of the loading view
     * @return layout resource id of the loading view
     */
    @LayoutRes val loadingResourceId: Int? = sectionParameters.loadingResourceId
    /**
     * Return the layout resource id of the failed view
     * @return layout resource id of the failed view
     */
    @LayoutRes val failedResourceId: Int? = sectionParameters.failedResourceId
    /**
     * Return the layout resource id of the empty view
     * @return layout resource id of the empty view
     */
    @LayoutRes val emptyResourceId: Int? = sectionParameters.emptyResourceId

    /**
     * Return the total of items of this Section, including content items (according to the section
     * mState) plus header and footer
     * @return total of items of this section
     */
    val sectionItemsTotal: Int
        get() {
            val contentTotal: Int = when (state) {
                LOADING -> 1
                LOADED -> getContentItemsTotal()
                FAILED -> 1
                EMPTY -> 1
                else -> throw IllegalStateException("Invalid mState")
            }

            return contentTotal + (if (mHasHeader) 1 else 0) + if (mHasFooter) 1 else 0
        }

    /**
     * Return the total of items of this Section
     * @return total of items of this Section
     */
    abstract fun getContentItemsTotal(): Int

    enum class State {
        LOADING, LOADED, FAILED, EMPTY
    }

    /**
     * Create a Section object with loading/failed states, without header and footer
     *
     * @param itemResourceId layout resource for its items
     * @param loadingResourceId layout resource for its loading mState
     * @param failedResourceId layout resource for its failed mState
     */
    @Deprecated(
            "Replaced by {@link #Section(SectionParameters)}\n" +
                    "     \n" +
                    "      "
    )
    constructor(
        @LayoutRes itemResourceId: Int,
        @LayoutRes loadingResourceId: Int,
        @LayoutRes failedResourceId: Int
    ) : this(
            Builder(itemResourceId)
                    .loadingResourceId(loadingResourceId)
                    .failedResourceId(failedResourceId)
                    .build()
    )

    /**
     * Create a Section object with loading/failed states, with a custom header but without footer
     *
     * @param headerResourceId layout resource for its header
     * @param itemResourceId layout resource for its items
     * @param loadingResourceId layout resource for its loading mState
     * @param failedResourceId layout resource for its failed mState
     */
    @Deprecated(
            "Replaced by {@link #Section(SectionParameters)}\n" +
                    "     \n" +
                    "      "
    )
    constructor(
        @LayoutRes headerResourceId: Int,
        @LayoutRes itemResourceId: Int,
        @LayoutRes loadingResourceId: Int,
        @LayoutRes failedResourceId: Int
    ) : this(
            Builder(itemResourceId)
                    .headerResourceId(headerResourceId)
                    .loadingResourceId(loadingResourceId)
                    .failedResourceId(failedResourceId)
                    .build()
    )

    /**
     * Create a Section object with loading/failed states, with a custom header and a custom footer
     *
     * @param headerResourceId layout resource for its header
     * @param footerResourceId layout resource for its footer
     * @param itemResourceId layout resource for its items
     * @param loadingResourceId layout resource for its loading mState
     * @param failedResourceId layout resource for its failed mState
     */
    @Deprecated(
            "Replaced by {@link #Section(SectionParameters)}\n" +
                    "     \n" +
                    "      "
    )
    constructor(
        @LayoutRes headerResourceId: Int,
        @LayoutRes footerResourceId: Int,
        @LayoutRes itemResourceId: Int,
        @LayoutRes loadingResourceId: Int,
        @LayoutRes failedResourceId: Int
    ) : this(
            Builder(itemResourceId)
                    .headerResourceId(headerResourceId)
                    .footerResourceId(footerResourceId)
                    .loadingResourceId(loadingResourceId)
                    .failedResourceId(failedResourceId)
                    .build()
    )

    init {
        this.mHasHeader = this.headerResourceId != null
        this.mHasFooter = this.footerResourceId != null
    }

    /**
     * Check if this Section has a header
     * @return true if this Section has a header
     */
    fun hasHeader(): Boolean {
        return mHasHeader
    }

    /**
     * Set if this Section has header
     * @param hasHeader true if this Section has a header
     */
    fun setHasHeader(hasHeader: Boolean) {
        this.mHasHeader = hasHeader
    }

    /**
     * Check if this Section has a footer
     * @return true if this Section has a footer
     */
    fun hasFooter(): Boolean {
        return mHasFooter
    }

    /**
     * Set if this Section has footer
     * @param hasFooter true if this Section has a footer
     */
    fun setHasFooter(hasFooter: Boolean) {
        this.mHasFooter = hasFooter
    }

    /**
     * Bind the data to the ViewHolder for the Content of this Section, that can be the Items,
     * Loading view or Failed view, depending on the current mState of the section
     * @param holder ViewHolder for the Content of this Section
     * @param position position of the item in the Section, not in the RecyclerView
     */
    fun onBindContentViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (state) {
            LOADING -> onBindLoadingViewHolder(holder)
            LOADED -> onBindItemViewHolder(holder, position)
            FAILED -> onBindFailedViewHolder(holder)
            EMPTY -> onBindEmptyViewHolder(holder)
            else -> throw IllegalStateException("Invalid mState")
        }
    }

    /**
     * Return the ViewHolder for the Header of this Section
     * @param view View inflated by resource returned by getHeaderResourceId
     * @return ViewHolder for the Header of this Section
     */
    open fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
        return EmptyViewHolder(view)
    }

    /**
     * Bind the data to the ViewHolder for the Header of this Section
     * @param holder ViewHolder for the Header of this Section
     */
    open fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
        // Nothing to bind here.
    }

    /**
     * Return the ViewHolder for the Footer of this Section
     * @param view View inflated by resource returned by getFooterResourceId
     * @return ViewHolder for the Footer of this Section
     */
    open fun getFooterViewHolder(view: View): RecyclerView.ViewHolder {
        return EmptyViewHolder(view)
    }

    /**
     * Bind the data to the ViewHolder for the Footer of this Section
     * @param holder ViewHolder for the Footer of this Section
     */
    open fun onBindFooterViewHolder(holder: RecyclerView.ViewHolder) {
        // Nothing to bind here.
    }

    /**
     * Return the ViewHolder for a single Item of this Section
     * @param view View inflated by resource returned by getItemResourceId
     * @return ViewHolder for the Item of this Section
     */
    abstract fun getItemViewHolder(view: View): RecyclerView.ViewHolder

    /**
     * Bind the data to the ViewHolder for an Item of this Section
     * @param holder ViewHolder for the Item of this Section
     * @param position position of the item in the Section, not in the RecyclerView
     */
    abstract fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int)

    /**
     * Return the ViewHolder for the Loading mState of this Section
     * @param view View inflated by resource returned by getItemResourceId
     * @return ViewHolder for the Loading mState of this Section
     */
    open fun getLoadingViewHolder(view: View): RecyclerView.ViewHolder {
        return EmptyViewHolder(view)
    }

    /**
     * Bind the data to the ViewHolder for Loading mState of this Section
     * @param holder ViewHolder for the Loading mState of this Section
     */
    open fun onBindLoadingViewHolder(holder: RecyclerView.ViewHolder) {
        // Nothing to bind here.
    }

    /**
     * Return the ViewHolder for the Failed mState of this Section
     * @param view View inflated by resource returned by getItemResourceId
     * @return ViewHolder for the Failed of this Section
     */
    open fun getFailedViewHolder(view: View): RecyclerView.ViewHolder {
        return EmptyViewHolder(view)
    }

    /**
     * Bind the data to the ViewHolder for the Failed mState of this Section
     * @param holder ViewHolder for the Failed mState of this Section
     */
    open fun onBindFailedViewHolder(holder: RecyclerView.ViewHolder) {
        // Nothing to bind here.
    }

    /**
     * Return the ViewHolder for the Empty mState of this Section
     * @param view View inflated by resource returned by getItemResourceId
     * @return ViewHolder for the Empty of this Section
     */
    open fun getEmptyViewHolder(view: View): RecyclerView.ViewHolder {
        return EmptyViewHolder(view)
    }

    /**
     * Bind the data to the ViewHolder for the Empty mState of this Section
     * @param holder ViewHolder for the Empty mState of this Section
     */
    open fun onBindEmptyViewHolder(holder: RecyclerView.ViewHolder) {
        // Nothing to bind here.
    }
}

package com.woocommerce.android.widgets

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Abstract Section to be used with [SectionedRecyclerViewAdapter].
 *
 * Original version: https://github.com/luizgrp/SectionedRecyclerViewAdapter
 */
abstract class Section
/**
 * Create a Section object based on [SectionParameters]
 * @param sectionParameters section parameters
 */
(sectionParameters: SectionParameters) {

    /**
     * Return the current State of this Section
     * @return current mState of this section
     */
    /**
     * Set the State of this Section
     * @param state mState of this section
     */
    var state = State.LOADED
        set(state) {
            when (state) {
                Section.State.LOADING -> if (loadingResourceId == null) {
                    throw IllegalStateException("Missing 'loading mState' resource id")
                }
                Section.State.FAILED -> if (failedResourceId == null) {
                    throw IllegalStateException("Missing 'failed mState' resource id")
                }
                Section.State.EMPTY -> if (emptyResourceId == null) {
                    throw IllegalStateException("Missing 'empty mState' resource id")
                }
            }

            field = state
        }

    /**
     * Check if this Section is mVisible
     * @return true if this Section is mVisible
     */
    /**
     * Set if this Section is mVisible
     * @param visible true if this Section is mVisible
     */
    var isVisible = true

    internal var mHasHeader = false
    internal var mHasFooter = false

    /**
     * Return the layout resource id of the header
     * @return layout resource id of the header
     */
    @LayoutRes val headerResourceId: Int?
    /**
     * Return the layout resource id of the footer
     * @return layout resource id of the footer
     */
    @LayoutRes val footerResourceId: Int?

    /**
     * Return the layout resource id of the item
     * @return layout resource id of the item
     */
    @LayoutRes val itemResourceId: Int

    /**
     * Return the layout resource id of the loading view
     * @return layout resource id of the loading view
     */
    @LayoutRes val loadingResourceId: Int?
    /**
     * Return the layout resource id of the failed view
     * @return layout resource id of the failed view
     */
    @LayoutRes val failedResourceId: Int?
    /**
     * Return the layout resource id of the empty view
     * @return layout resource id of the empty view
     */
    @LayoutRes val emptyResourceId: Int?

    /**
     * Return the total of items of this Section, including content items (according to the section
     * mState) plus header and footer
     * @return total of items of this section
     */
    val sectionItemsTotal: Int
        get() {
            val contentItemsTotal: Int

            when (state) {
                Section.State.LOADING -> contentItemsTotal = 1
                Section.State.LOADED -> contentItemsTotal = contentItemsTotal
                Section.State.FAILED -> contentItemsTotal = 1
                Section.State.EMPTY -> contentItemsTotal = 1
                else -> throw IllegalStateException("Invalid mState")
            }

            return contentItemsTotal + (if (mHasHeader) 1 else 0) + if (mHasFooter) 1 else 0
        }

    /**
     * Return the total of items of this Section
     * @return total of items of this Section
     */
    abstract val contentItemsTotal: Int

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
        @LayoutRes itemResourceId: Int, @LayoutRes loadingResourceId: Int,
        @LayoutRes failedResourceId: Int
    ) : this(
            SectionParameters.Builder(itemResourceId)
                    .loadingResourceId(loadingResourceId)
                    .failedResourceId(failedResourceId)
                    .build()
    ) {
    }

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
        @LayoutRes headerResourceId: Int, @LayoutRes itemResourceId: Int,
        @LayoutRes loadingResourceId: Int, @LayoutRes failedResourceId: Int
    ) : this(
            SectionParameters.Builder(itemResourceId)
                    .headerResourceId(headerResourceId)
                    .loadingResourceId(loadingResourceId)
                    .failedResourceId(failedResourceId)
                    .build()
    ) {
    }

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
        @LayoutRes headerResourceId: Int, @LayoutRes footerResourceId: Int,
        @LayoutRes itemResourceId: Int, @LayoutRes loadingResourceId: Int,
        @LayoutRes failedResourceId: Int
    ) : this(
            SectionParameters.Builder(itemResourceId)
                    .headerResourceId(headerResourceId)
                    .footerResourceId(footerResourceId)
                    .loadingResourceId(loadingResourceId)
                    .failedResourceId(failedResourceId)
                    .build()
    ) {
    }

    init {
        this.headerResourceId = sectionParameters.headerResourceId
        this.footerResourceId = sectionParameters.footerResourceId
        this.itemResourceId = sectionParameters.itemResourceId
        this.loadingResourceId = sectionParameters.loadingResourceId
        this.failedResourceId = sectionParameters.failedResourceId
        this.emptyResourceId = sectionParameters.emptyResourceId

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
            Section.State.LOADING -> onBindLoadingViewHolder(holder)
            Section.State.LOADED -> onBindItemViewHolder(holder, position)
            Section.State.FAILED -> onBindFailedViewHolder(holder)
            Section.State.EMPTY -> onBindEmptyViewHolder(holder)
            else -> throw IllegalStateException("Invalid mState")
        }
    }

    /**
     * Return the ViewHolder for the Header of this Section
     * @param view View inflated by resource returned by getHeaderResourceId
     * @return ViewHolder for the Header of this Section
     */
    open fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
        return SectionedRecyclerViewAdapter.EmptyViewHolder(view)
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
    fun getFooterViewHolder(view: View): RecyclerView.ViewHolder {
        return SectionedRecyclerViewAdapter.EmptyViewHolder(view)
    }

    /**
     * Bind the data to the ViewHolder for the Footer of this Section
     * @param holder ViewHolder for the Footer of this Section
     */
    fun onBindFooterViewHolder(holder: RecyclerView.ViewHolder) {
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
        return SectionedRecyclerViewAdapter.EmptyViewHolder(view)
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
        return SectionedRecyclerViewAdapter.EmptyViewHolder(view)
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
        return SectionedRecyclerViewAdapter.EmptyViewHolder(view)
    }

    /**
     * Bind the data to the ViewHolder for the Empty mState of this Section
     * @param holder ViewHolder for the Empty mState of this Section
     */
    open fun onBindEmptyViewHolder(holder: RecyclerView.ViewHolder) {
        // Nothing to bind here.
    }
}

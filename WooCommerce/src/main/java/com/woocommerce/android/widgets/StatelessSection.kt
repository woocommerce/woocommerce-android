package com.woocommerce.android.widgets

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Abstract [Section] with no states.
 *
 * Original version: https://github.com/luizgrp/SectionedRecyclerViewAdapter
 */
abstract class StatelessSection
/**
 * Create a stateless Section object based on [SectionParameters]
 * @param sectionParameters section parameters
 */
(sectionParameters: SectionParameters) : Section(sectionParameters) {
    /**
     * Create a stateless Section object without header and footer
     *
     * @param itemResourceId layout resource for its items
     */
    @Deprecated(
            "Replaced by {@link #StatelessSection(SectionParameters)}\n" +
                    "     \n" +
                    "      "
    )
    constructor(@LayoutRes itemResourceId: Int) : this(
            SectionParameters.Builder(itemResourceId)
                    .build()
    ) {
    }

    /**
     * Create a stateless Section object, with a custom header but without footer
     *
     * @param headerResourceId layout resource for its header
     * @param itemResourceId layout resource for its items
     */
    @Deprecated(
            "Replaced by {@link #StatelessSection(SectionParameters)}\n" +
                    "     \n" +
                    "      "
    )
    constructor(@LayoutRes headerResourceId: Int, @LayoutRes itemResourceId: Int) : this(
            SectionParameters.Builder(itemResourceId)
                    .headerResourceId(headerResourceId)
                    .build()
    ) {
    }

    /**
     * Create a stateless Section object, with a custom header and a custom footer
     *
     * @param headerResourceId layout resource for its header
     * @param footerResourceId layout resource for its footer
     * @param itemResourceId layout resource for its items
     */
    @Deprecated(
            "Replaced by {@link #StatelessSection(SectionParameters)}\n" +
                    "     \n" +
                    "      "
    )
    constructor(
        @LayoutRes headerResourceId: Int, @LayoutRes footerResourceId: Int,
        @LayoutRes itemResourceId: Int
    ) : this(
            SectionParameters.Builder(itemResourceId)
                    .headerResourceId(headerResourceId)
                    .footerResourceId(footerResourceId)
                    .build()
    ) {
    }

    init {
        if (sectionParameters.loadingResourceId != null) {
            throw IllegalArgumentException("Stateless section shouldn't have a loading state resource")
        }

        if (sectionParameters.failedResourceId != null) {
            throw IllegalArgumentException("Stateless section shouldn't have a failed state resource")
        }

        if (sectionParameters.emptyResourceId != null) {
            throw IllegalArgumentException("Stateless section shouldn't have an empty state resource")
        }
    }

    override fun onBindLoadingViewHolder(holder: RecyclerView.ViewHolder) {
        super.onBindLoadingViewHolder(holder)
    }

    override fun getLoadingViewHolder(view: View): RecyclerView.ViewHolder {
        return super.getLoadingViewHolder(view)
    }

    override fun onBindFailedViewHolder(holder: RecyclerView.ViewHolder) {
        super.onBindFailedViewHolder(holder)
    }

    override fun getFailedViewHolder(view: View): RecyclerView.ViewHolder {
        return super.getFailedViewHolder(view)
    }

    override fun onBindEmptyViewHolder(holder: RecyclerView.ViewHolder) {
        super.onBindEmptyViewHolder(holder)
    }

    override fun getEmptyViewHolder(view: View): RecyclerView.ViewHolder {
        return super.getEmptyViewHolder(view)
    }
}

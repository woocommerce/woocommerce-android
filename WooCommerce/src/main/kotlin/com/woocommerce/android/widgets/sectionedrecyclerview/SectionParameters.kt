package com.woocommerce.android.widgets.sectionedrecyclerview

import androidx.annotation.LayoutRes

/**
 * Class used as constructor parameters of [Section].
 *
 * Original version: https://github.com/luizgrp/SectionedRecyclerViewAdapter
 */
class SectionParameters private constructor(builder: Builder) {
    @LayoutRes val headerResourceId: Int?

    @LayoutRes val footerResourceId: Int?

    @LayoutRes val itemResourceId: Int

    @LayoutRes val loadingResourceId: Int?

    @LayoutRes val failedResourceId: Int?

    @LayoutRes val emptyResourceId: Int?

    /**
     * Builder of [SectionParameters]
     */
    class Builder
    /**
     * Constructor with mandatory parameters of [Section]
     * @param mItemResourceId layout resource for Section's items
     */
    (@param:LayoutRes val mItemResourceId: Int) {
        @LayoutRes var headerResourceId: Int? = null

        @LayoutRes var footerResourceId: Int? = null

        @LayoutRes var loadingResourceId: Int? = null

        @LayoutRes var failedResourceId: Int? = null

        @LayoutRes var emptyResourceId: Int? = null

        /**
         * Set layout resource for Section's header
         * @param headerResourceId layout resource for Section's header
         * @return this builder
         */
        fun headerResourceId(@LayoutRes headerResourceId: Int): Builder {
            this.headerResourceId = headerResourceId

            return this
        }

        /**
         * Build an instance of SectionParameters
         * @return an instance of SectionParameters
         */
        fun build(): SectionParameters {
            return SectionParameters(this)
        }
    }

    init {
        this.headerResourceId = builder.headerResourceId
        this.footerResourceId = builder.footerResourceId
        this.itemResourceId = builder.mItemResourceId
        this.loadingResourceId = builder.loadingResourceId
        this.failedResourceId = builder.failedResourceId
        this.emptyResourceId = builder.emptyResourceId
    }
}

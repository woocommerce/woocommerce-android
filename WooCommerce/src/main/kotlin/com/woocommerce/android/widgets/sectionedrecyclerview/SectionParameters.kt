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
         * Set layout resource for Section's footer
         * @param footerResourceId layout resource for Section's footer
         * @return this builder
         */
        fun footerResourceId(@LayoutRes footerResourceId: Int): Builder {
            this.footerResourceId = footerResourceId

            return this
        }

        /**
         * Set layout resource for Section's loading state
         * @param loadingResourceId layout resource for Section's loading state
         * @return this builder
         */
        fun loadingResourceId(@LayoutRes loadingResourceId: Int): Builder {
            this.loadingResourceId = loadingResourceId

            return this
        }

        /**
         * Set layout resource for Section's failed state
         * @param failedResourceId layout resource for Section's failed state
         * @return this builder
         */
        fun failedResourceId(@LayoutRes failedResourceId: Int): Builder {
            this.failedResourceId = failedResourceId

            return this
        }

        /**
         * Set layout resource for Section's empty state
         * @param emptyResourceId layout resource for Section's empty state
         * @return this builder
         */
        fun emptyResourceId(@LayoutRes emptyResourceId: Int): Builder {
            this.emptyResourceId = emptyResourceId

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

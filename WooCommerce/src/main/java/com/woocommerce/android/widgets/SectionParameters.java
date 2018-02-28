package com.woocommerce.android.widgets;

import android.support.annotation.LayoutRes;

/**
 * Class used as constructor parameters of {@link Section}.
 *
 * Original version: https://github.com/luizgrp/SectionedRecyclerViewAdapter
 */
@SuppressWarnings({"CheckStyle, WeakerAccess, unused"})
public class SectionParameters {
    @LayoutRes public final Integer headerResourceId;
    @LayoutRes public final Integer footerResourceId;
    @LayoutRes public final int itemResourceId;
    @LayoutRes public final Integer loadingResourceId;
    @LayoutRes public final Integer failedResourceId;
    @LayoutRes public final Integer emptyResourceId;

    /**
     * Builder of {@link SectionParameters}
     */
    @SuppressWarnings("unused")
    public static class Builder {
        private final int mItemResourceId;

        @LayoutRes private Integer mHeaderResourceId;
        @LayoutRes private Integer mFooterResourceId;
        @LayoutRes private Integer mLoadingResourceId;
        @LayoutRes private Integer mFailedResourceId;
        @LayoutRes private Integer mEmptyResourceId;

        /**
         * Constructor with mandatory parameters of {@link Section}
         * @param itemResourceId layout resource for Section's items
         */
        public Builder(@LayoutRes int itemResourceId) {
            this.mItemResourceId = itemResourceId;
        }

        /**
         * Set layout resource for Section's header
         * @param headerResourceId layout resource for Section's header
         * @return this builder
         */
        public Builder headerResourceId(@LayoutRes int headerResourceId) {
            this.mHeaderResourceId = headerResourceId;

            return this;
        }

        /**
         * Set layout resource for Section's footer
         * @param footerResourceId layout resource for Section's footer
         * @return this builder
         */
        public Builder footerResourceId(@LayoutRes int footerResourceId) {
            this.mFooterResourceId = footerResourceId;

            return this;
        }

        /**
         * Set layout resource for Section's loading state
         * @param loadingResourceId layout resource for Section's loading state
         * @return this builder
         */
        public Builder loadingResourceId(@LayoutRes int loadingResourceId) {
            this.mLoadingResourceId = loadingResourceId;

            return this;
        }

        /**
         * Set layout resource for Section's failed state
         * @param failedResourceId layout resource for Section's failed state
         * @return this builder
         */
        public Builder failedResourceId(@LayoutRes int failedResourceId) {
            this.mFailedResourceId = failedResourceId;

            return this;
        }

        /**
         * Set layout resource for Section's empty state
         * @param emptyResourceId layout resource for Section's empty state
         * @return this builder
         */
        @SuppressWarnings("unused")
        public Builder emptyResourceId(@LayoutRes int emptyResourceId) {
            this.mEmptyResourceId = emptyResourceId;

            return this;
        }

        /**
         * Build an instance of SectionParameters
         * @return an instance of SectionParameters
         */
        public SectionParameters build() {
            return new SectionParameters(this);
        }
    }

    private SectionParameters(Builder builder) {
        this.headerResourceId = builder.mHeaderResourceId;
        this.footerResourceId = builder.mFooterResourceId;
        this.itemResourceId = builder.mItemResourceId;
        this.loadingResourceId = builder.mLoadingResourceId;
        this.failedResourceId = builder.mFailedResourceId;
        this.emptyResourceId = builder.mEmptyResourceId;
    }
}

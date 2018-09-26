package com.woocommerce.android.widgets;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Abstract Section to be used with {@link SectionedRecyclerViewAdapter}.
 *
 * Original version: https://github.com/luizgrp/SectionedRecyclerViewAdapter
 */
public abstract class Section {
    public enum State { LOADING, LOADED, FAILED, EMPTY }

    private State mState = State.LOADED;

    private boolean mVisible = true;

    boolean mHasHeader = false;
    boolean mHasFooter = false;

    @LayoutRes private final Integer mHeaderResourceId;
    @LayoutRes private final Integer mFooterResourceId;

    @LayoutRes private final int mItemResourceId;

    @LayoutRes private final Integer mLoadingResourceId;
    @LayoutRes private final Integer mFailedResourceId;
    @LayoutRes private final Integer mEmptyResourceId;

    /**
     * Create a Section object with loading/failed states, without header and footer
     *
     * @deprecated Replaced by {@link #Section(SectionParameters)}
     *
     * @param itemResourceId layout resource for its items
     * @param loadingResourceId layout resource for its loading mState
     * @param failedResourceId layout resource for its failed mState
     */
    @SuppressWarnings("unused")
    @Deprecated
    public Section(@LayoutRes int itemResourceId, @LayoutRes int loadingResourceId,
                   @LayoutRes int failedResourceId) {
        this(new SectionParameters.Builder(itemResourceId)
                .loadingResourceId(loadingResourceId)
                .failedResourceId(failedResourceId)
                .build());
    }

    /**
     * Create a Section object with loading/failed states, with a custom header but without footer
     *
     * @deprecated Replaced by {@link #Section(SectionParameters)}
     *
     * @param headerResourceId layout resource for its header
     * @param itemResourceId layout resource for its items
     * @param loadingResourceId layout resource for its loading mState
     * @param failedResourceId layout resource for its failed mState
     */
    @Deprecated
    public Section(@LayoutRes int headerResourceId, @LayoutRes int itemResourceId,
                   @LayoutRes int loadingResourceId, @LayoutRes int failedResourceId) {
        this(new SectionParameters.Builder(itemResourceId)
                .headerResourceId(headerResourceId)
                .loadingResourceId(loadingResourceId)
                .failedResourceId(failedResourceId)
                .build());
    }

    /**
     * Create a Section object with loading/failed states, with a custom header and a custom footer
     *
     * @deprecated Replaced by {@link #Section(SectionParameters)}
     *
     * @param headerResourceId layout resource for its header
     * @param footerResourceId layout resource for its footer
     * @param itemResourceId layout resource for its items
     * @param loadingResourceId layout resource for its loading mState
     * @param failedResourceId layout resource for its failed mState
     */
    @Deprecated
    public Section(@LayoutRes int headerResourceId, @LayoutRes int footerResourceId,
                   @LayoutRes int itemResourceId, @LayoutRes int loadingResourceId,
                   @LayoutRes int failedResourceId) {
        this(new SectionParameters.Builder(itemResourceId)
                .headerResourceId(headerResourceId)
                .footerResourceId(footerResourceId)
                .loadingResourceId(loadingResourceId)
                .failedResourceId(failedResourceId)
                .build());
    }

    /**
     * Create a Section object based on {@link SectionParameters}
     * @param sectionParameters section parameters
     */
    @SuppressWarnings("WeakerAccess")
    public Section(SectionParameters sectionParameters) {
        this.mHeaderResourceId = sectionParameters.headerResourceId;
        this.mFooterResourceId = sectionParameters.footerResourceId;
        this.mItemResourceId = sectionParameters.itemResourceId;
        this.mLoadingResourceId = sectionParameters.loadingResourceId;
        this.mFailedResourceId = sectionParameters.failedResourceId;
        this.mEmptyResourceId = sectionParameters.emptyResourceId;

        this.mHasHeader = (this.mHeaderResourceId != null);
        this.mHasFooter = (this.mFooterResourceId != null);
    }

    /**
     * Set the State of this Section
     * @param state mState of this section
     */
    @SuppressWarnings({"EnumSwitchStatementWhichMissesCases, unused", "unused"})
    public final void setState(State state) {
        switch (state) {
            case LOADING:
                if (mLoadingResourceId == null) {
                    throw new IllegalStateException("Missing 'loading mState' resource id");
                }
                break;
            case FAILED:
                if (mFailedResourceId == null) {
                    throw new IllegalStateException("Missing 'failed mState' resource id");
                }
                break;
            case EMPTY:
                if (mEmptyResourceId == null) {
                    throw new IllegalStateException("Missing 'empty mState' resource id");
                }
                break;
        }

        this.mState = state;
    }

    /**
     * Return the current State of this Section
     * @return current mState of this section
     */
    @SuppressWarnings("WeakerAccess")
    public final State getState() {
        return mState;
    }

    /**
     * Check if this Section is mVisible
     * @return true if this Section is mVisible
     */
    @SuppressWarnings("WeakerAccess")
    public final boolean isVisible() {
        return mVisible;
    }

    /**
     * Set if this Section is mVisible
     * @param visible true if this Section is mVisible
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public final void setVisible(boolean visible) {
        this.mVisible = visible;
    }

    /**
     * Check if this Section has a header
     * @return true if this Section has a header
     */
    @SuppressWarnings("WeakerAccess")
    public final boolean hasHeader() {
        return mHasHeader;
    }

    /**
     * Set if this Section has header
     * @param hasHeader true if this Section has a header
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public final void setHasHeader(boolean hasHeader) {
        this.mHasHeader = hasHeader;
    }

    /**
     * Check if this Section has a footer
     * @return true if this Section has a footer
     */
    @SuppressWarnings("WeakerAccess")
    public final boolean hasFooter() {
        return mHasFooter;
    }

    /**
     * Set if this Section has footer
     * @param hasFooter true if this Section has a footer
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public final void setHasFooter(boolean hasFooter) {
        this.mHasFooter = hasFooter;
    }

    /**
     * Return the layout resource id of the header
     * @return layout resource id of the header
     */
    @SuppressWarnings("WeakerAccess")
    public final Integer getHeaderResourceId() {
        return mHeaderResourceId;
    }

    /**
     * Return the layout resource id of the footer
     * @return layout resource id of the footer
     */
    @SuppressWarnings("WeakerAccess")
    public final Integer getFooterResourceId() {
        return mFooterResourceId;
    }

    /**
     * Return the layout resource id of the item
     * @return layout resource id of the item
     */
    @SuppressWarnings("WeakerAccess")
    public final int getItemResourceId() {
        return mItemResourceId;
    }

    /**
     * Return the layout resource id of the loading view
     * @return layout resource id of the loading view
     */
    @SuppressWarnings("WeakerAccess")
    public final Integer getLoadingResourceId() {
        return mLoadingResourceId;
    }

    /**
     * Return the layout resource id of the failed view
     * @return layout resource id of the failed view
     */
    @SuppressWarnings("WeakerAccess")
    public final Integer getFailedResourceId() {
        return mFailedResourceId;
    }

    /**
     * Return the layout resource id of the empty view
     * @return layout resource id of the empty view
     */
    @SuppressWarnings("WeakerAccess")
    public final Integer getEmptyResourceId() {
        return mEmptyResourceId;
    }

    /**
     * Bind the data to the ViewHolder for the Content of this Section, that can be the Items,
     * Loading view or Failed view, depending on the current mState of the section
     * @param holder ViewHolder for the Content of this Section
     * @param position position of the item in the Section, not in the RecyclerView
     */
    @SuppressWarnings("WeakerAccess")
    public final void onBindContentViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (mState) {
            case LOADING:
                onBindLoadingViewHolder(holder);
                break;
            case LOADED:
                onBindItemViewHolder(holder, position);
                break;
            case FAILED:
                onBindFailedViewHolder(holder);
                break;
            case EMPTY:
                onBindEmptyViewHolder(holder);
                break;
            default:
                throw new IllegalStateException("Invalid mState");
        }
    }

    /**
     * Return the total of items of this Section, including content items (according to the section
     * mState) plus header and footer
     * @return total of items of this section
     */
    @SuppressWarnings("WeakerAccess")
    public final int getSectionItemsTotal() {
        int contentItemsTotal;

        switch (mState) {
            case LOADING:
                contentItemsTotal = 1;
                break;
            case LOADED:
                contentItemsTotal = getContentItemsTotal();
                break;
            case FAILED:
                contentItemsTotal = 1;
                break;
            case EMPTY:
                contentItemsTotal = 1;
                break;
            default:
                throw new IllegalStateException("Invalid mState");
        }

        return contentItemsTotal + (mHasHeader ? 1 : 0) + (mHasFooter ? 1 : 0);
    }

    /**
     * Return the total of items of this Section
     * @return total of items of this Section
     */
    public abstract int getContentItemsTotal();

    /**
     * Return the ViewHolder for the Header of this Section
     * @param view View inflated by resource returned by getHeaderResourceId
     * @return ViewHolder for the Header of this Section
     */
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new SectionedRecyclerViewAdapter.EmptyViewHolder(view);
    }

    /**
     * Bind the data to the ViewHolder for the Header of this Section
     * @param holder ViewHolder for the Header of this Section
     */
    @SuppressWarnings({"EmptyMethod", "unused"})
    public void onBindHeaderViewHolder(@SuppressWarnings("unused") RecyclerView.ViewHolder holder) {
        // Nothing to bind here.
    }

    /**
     * Return the ViewHolder for the Footer of this Section
     * @param view View inflated by resource returned by getFooterResourceId
     * @return ViewHolder for the Footer of this Section
     */
    @SuppressWarnings("WeakerAccess")
    public RecyclerView.ViewHolder getFooterViewHolder(View view) {
        return new SectionedRecyclerViewAdapter.EmptyViewHolder(view);
    }

    /**
     * Bind the data to the ViewHolder for the Footer of this Section
     * @param holder ViewHolder for the Footer of this Section
     */
    @SuppressWarnings({"WeakerAccess", "unused", "EmptyMethod"})
    public void onBindFooterViewHolder(RecyclerView.ViewHolder holder) {
        // Nothing to bind here.
    }

    /**
     * Return the ViewHolder for a single Item of this Section
     * @param view View inflated by resource returned by getItemResourceId
     * @return ViewHolder for the Item of this Section
     */
    public abstract RecyclerView.ViewHolder getItemViewHolder(View view);
    /**
     * Bind the data to the ViewHolder for an Item of this Section
     * @param holder ViewHolder for the Item of this Section
     * @param position position of the item in the Section, not in the RecyclerView
     */
    public abstract void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position);

    /**
     * Return the ViewHolder for the Loading mState of this Section
     * @param view View inflated by resource returned by getItemResourceId
     * @return ViewHolder for the Loading mState of this Section
     */
    public RecyclerView.ViewHolder getLoadingViewHolder(View view) {
        return new SectionedRecyclerViewAdapter.EmptyViewHolder(view);
    }
    /**
     * Bind the data to the ViewHolder for Loading mState of this Section
     * @param holder ViewHolder for the Loading mState of this Section
     */
    @SuppressWarnings({"WeakerAccess", "EmptyMethod"})
    public void onBindLoadingViewHolder(RecyclerView.ViewHolder holder) {
        // Nothing to bind here.
    }

    /**
     * Return the ViewHolder for the Failed mState of this Section
     * @param view View inflated by resource returned by getItemResourceId
     * @return ViewHolder for the Failed of this Section
     */
    public RecyclerView.ViewHolder getFailedViewHolder(View view) {
        return new SectionedRecyclerViewAdapter.EmptyViewHolder(view);
    }
    /**
     * Bind the data to the ViewHolder for the Failed mState of this Section
     * @param holder ViewHolder for the Failed mState of this Section
     */
    @SuppressWarnings({"WeakerAccess", "EmptyMethod"})
    public void onBindFailedViewHolder(RecyclerView.ViewHolder holder) {
        // Nothing to bind here.
    }

    /**
     * Return the ViewHolder for the Empty mState of this Section
     * @param view View inflated by resource returned by getItemResourceId
     * @return ViewHolder for the Empty of this Section
     */
    public RecyclerView.ViewHolder getEmptyViewHolder(View view) {
        return new SectionedRecyclerViewAdapter.EmptyViewHolder(view);
    }
    /**
     * Bind the data to the ViewHolder for the Empty mState of this Section
     * @param holder ViewHolder for the Empty mState of this Section
     */
    @SuppressWarnings({"WeakerAccess", "EmptyMethod"})
    public void onBindEmptyViewHolder(RecyclerView.ViewHolder holder) {
        // Nothing to bind here.
    }
}

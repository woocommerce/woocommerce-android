package com.woocommerce.android.widgets.sectionedrecyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.widgets.sectionedrecyclerview.Section.State
import java.util.UUID

/**
 * A custom RecyclerView Adapter that allows [Sections][Section] to be added to it.
 * Sections are displayed in the same order they were added.
 *
 * Original version: https://github.com/luizgrp/SectionedRecyclerViewAdapter
 */
open class SectionedRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    /**
     * Return a map with all mSections of this adapter.
     *
     * @return a map with all mSections
     */
    val sectionsMap: LinkedHashMap<String, Section> = LinkedHashMap()
    private val sectionViewTypeNumbers: HashMap<String, Int> = HashMap()
    private var viewTypeCount = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        var viewHolder: RecyclerView.ViewHolder? = null

        for (entry in sectionViewTypeNumbers) {
            if (viewType >= entry.value && viewType < entry.value + VIEW_TYPE_QTY) {
                val section = sectionsMap[entry.key]!!
                val sectionViewType = viewType - entry.value

                when (sectionViewType) {
                    VIEW_TYPE_HEADER -> {
                        viewHolder = getHeaderViewHolder(parent, section)
                    }
                    VIEW_TYPE_FOOTER -> {
                        viewHolder = getFooterViewHolder(parent, section)
                    }
                    VIEW_TYPE_ITEM_LOADED -> {
                        viewHolder = getItemViewHolder(parent, section)
                    }
                    VIEW_TYPE_LOADING -> {
                        viewHolder = getLoadingViewHolder(parent, section)
                    }
                    VIEW_TYPE_FAILED -> {
                        viewHolder = getFailedViewHolder(parent, section)
                    }
                    VIEW_TYPE_EMPTY -> {
                        viewHolder = getEmptyViewHolder(parent, section)
                    }
                    else -> throw IllegalArgumentException("Invalid viewType")
                }
            }
        }

        return viewHolder!!
    }

    private fun getItemViewHolder(parent: ViewGroup, section: Section): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            section.itemResourceId,
            parent,
            false
        )
        // get the item viewholder from the section
        return section.getItemViewHolder(view)
    }

    private fun getHeaderViewHolder(parent: ViewGroup, section: Section): RecyclerView.ViewHolder {
        val resId = section.headerResourceId ?: throw NullPointerException("Missing 'header' resource id")

        val view = LayoutInflater.from(parent.context).inflate(resId, parent, false)
        // get the header viewholder from the section
        return section.getHeaderViewHolder(view)
    }

    private fun getFooterViewHolder(parent: ViewGroup, section: Section): RecyclerView.ViewHolder {
        val resId = section.footerResourceId ?: throw NullPointerException("Missing 'footer' resource id")

        val view = LayoutInflater.from(parent.context).inflate(resId, parent, false)
        // get the footer viewholder from the section
        return section.getFooterViewHolder(view)
    }

    private fun getLoadingViewHolder(parent: ViewGroup, section: Section): RecyclerView.ViewHolder {
        val resId = section.loadingResourceId ?: throw NullPointerException("Missing 'loading state' resource id")

        val view = LayoutInflater.from(parent.context).inflate(resId, parent, false)
        // get the loading viewholder from the section
        return section.getLoadingViewHolder(view)
    }

    private fun getFailedViewHolder(parent: ViewGroup, section: Section): RecyclerView.ViewHolder {
        val resId = section.failedResourceId ?: throw NullPointerException("Missing 'failed state' resource id")

        val view = LayoutInflater.from(parent.context).inflate(resId, parent, false)
        // get the failed load viewholder from the section
        return section.getFailedViewHolder(view)
    }

    private fun getEmptyViewHolder(parent: ViewGroup, section: Section): RecyclerView.ViewHolder {
        val resId = section.emptyResourceId ?: throw NullPointerException("Missing 'empty state' resource id")

        val view = LayoutInflater.from(parent.context).inflate(resId, parent, false)
        // get the empty load viewholder from the section
        return section.getEmptyViewHolder(view)
    }

    /**
     * Return the total number of sections
     * from the list
     */
    fun getSectionTotal(): Int {
        return sectionsMap.size
    }

    /**
     * Add a section to this recyclerview.
     *
     * @param tag unique identifier of the section
     * @param section section to be added
     */
    fun addSection(tag: String, section: Section) {
        this.sectionsMap[tag] = section
        this.sectionViewTypeNumbers[tag] = viewTypeCount
        viewTypeCount += VIEW_TYPE_QTY
    }

    /**
     * Add a section to this recyclerview with a random tag;
     *
     * @param section section to be added
     * @return generated tag
     */
    fun addSection(section: Section): String {
        val tag = UUID.randomUUID().toString()

        addSection(tag, section)

        return tag
    }

    /**
     * Remove all mSections from this recyclerview.
     */
    fun removeAllSections() {
        this.sectionsMap.clear()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var currentPos = 0

        for (entry in sectionsMap) {
            val section = entry.value

            // ignore invisible mSections
            if (!section.isVisible) continue

            val sectionTotal = section.sectionItemsTotal

            // check if position is in this section
            if (position >= currentPos && position <= currentPos + sectionTotal - 1) {
                if (section.hasHeader()) {
                    if (position == currentPos) {
                        // delegate the binding to the section header
                        getSectionForPosition(position).onBindHeaderViewHolder(holder)
                        return
                    }
                }

                if (section.hasFooter()) {
                    if (position == currentPos + sectionTotal - 1) {
                        // delegate the binding to the section header
                        getSectionForPosition(position).onBindFooterViewHolder(holder)
                        return
                    }
                }

                // delegate the binding to the section content
                getSectionForPosition(position).onBindContentViewHolder(
                    holder,
                    getPositionInSection(position)
                )
                return
            }

            currentPos += sectionTotal
        }

        throw IndexOutOfBoundsException("Invalid position")
    }

    override fun getItemCount(): Int {
        var count = 0

        for (entry in sectionsMap) {
            val section = entry.value

            // ignore invisible mSections
            if (!section.isVisible) continue

            count += section.sectionItemsTotal
        }

        return count
    }

    override fun getItemViewType(position: Int): Int {
        /*
         Each Section has 6 "viewtypes":
         1) header
         2) footer
         3) items
         4) loading
         5) load failed
         6) empty
         */
        var currentPos = 0

        for (entry in sectionsMap) {
            val section = entry.value

            // ignore invisible mSections
            if (!section.isVisible) continue

            val sectionTotal = section.sectionItemsTotal

            // check if position is in this section
            if (position >= currentPos && position <= currentPos + sectionTotal - 1) {
                val viewType = sectionViewTypeNumbers[entry.key]!!

                if (section.hasHeader()) {
                    if (position == currentPos) {
                        return viewType
                    }
                }

                if (section.hasFooter()) {
                    if (position == currentPos + sectionTotal - 1) {
                        return viewType + 1
                    }
                }

                return when (section.state) {
                    State.LOADED -> viewType + 2
                    State.LOADING -> viewType + 3
                    State.FAILED -> viewType + 4
                    State.EMPTY -> viewType + 5
                }
            }

            currentPos += sectionTotal
        }

        throw IndexOutOfBoundsException("Invalid position")
    }

    /**
     * Returns the Section object for a position in the adapter.
     *
     * @param position position in the adapter
     * @return Section object for that position
     */
    fun getSectionForPosition(position: Int): Section {
        var currentPos = 0

        for (entry in sectionsMap) {
            val section = entry.value

            // ignore invisible mSections
            if (!section.isVisible) continue

            val sectionTotal = section.sectionItemsTotal

            // check if position is in this section
            if (position >= currentPos && position <= currentPos + sectionTotal - 1) {
                return section
            }

            currentPos += sectionTotal
        }

        throw IndexOutOfBoundsException("Invalid position")
    }

    /**
     * Return the item position relative to the section.
     *
     * @param position position of the item in the adapter
     * @return position of the item in the section
     */
    fun getPositionInSection(position: Int): Int {
        var currentPos = 0

        for (entry in sectionsMap) {
            val section = entry.value

            // ignore invisible mSections
            if (!section.isVisible) continue

            val sectionTotal = section.sectionItemsTotal

            // check if position is in this section
            if (position >= currentPos && position <= currentPos + sectionTotal - 1) {
                return position - currentPos - if (section.hasHeader()) 1 else 0
            }

            currentPos += sectionTotal
        }

        throw IndexOutOfBoundsException("Invalid position")
    }

    /**
     * Return the section position in the adapter.
     *
     * @param section a mVisible section of this adapter
     * @return position of the section in the adapter
     */
    fun getSectionPosition(section: Section): Int {
        var currentPos = 0

        for (entry in sectionsMap) {
            val loopSection = entry.value

            // ignore invisible mSections
            if (!loopSection.isVisible) continue

            if (loopSection === section) {
                return currentPos
            }

            val sectionTotal = loopSection.sectionItemsTotal

            currentPos += sectionTotal
        }

        WooLog.w(T.NOTIFICATIONS, "Invalid section " + section.toString())
        return INVALID_POSITION
    }

    /**
     * A concrete class of an empty ViewHolder.
     * Should be used to avoid the boilerplate of creating a ViewHolder class for simple case
     * scenarios.
     */
    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_FOOTER = 1
        private const val VIEW_TYPE_ITEM_LOADED = 2
        private const val VIEW_TYPE_LOADING = 3
        private const val VIEW_TYPE_FAILED = 4
        private const val VIEW_TYPE_EMPTY = 5
        private const val VIEW_TYPE_QTY = 6

        const val INVALID_POSITION = -1
    }
}

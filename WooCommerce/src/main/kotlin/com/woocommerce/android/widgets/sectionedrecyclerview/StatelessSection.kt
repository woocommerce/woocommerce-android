package com.woocommerce.android.widgets.sectionedrecyclerview

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
}

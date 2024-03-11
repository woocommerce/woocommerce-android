package com.woocommerce.android.extensions

import android.content.Context
import androidx.fragment.app.Fragment

private const val SMALL_SCREEN_MIN_WIDTH = 600
private const val SMALL_SCREEN_MAX_WIDTH = 840
private const val MEDIUM_SCREEN_MAX_WIDTH = 840
private const val EXPANDED_SCREEN_MAX_WIDTH = 1200
private const val LARGE_SCREEN_MAX_WIDTH = 1600

/**
 * @throws [IllegalStateException] if not currently associated with a context.
 */
fun Fragment.isTablet() = requireContext().isTablet()

fun Context.isTablet() = isMediumWindowSize() || isExpandedWindowSize() || isLargeWindowSize() || isXLargeWindowSize()

/**
 * Returns true if the current screen size is of the "Medium" window size class as per
 * [Material Design 3 guidelines](https://m3.material.io/foundations/layout/applying-layout/window-size-classes).
 * This indicates tablet in portrait or foldable in portrait (unfolded).
 */
fun Context.isMediumWindowSize() =
    resources.configuration.screenWidthDp in SMALL_SCREEN_MIN_WIDTH..SMALL_SCREEN_MAX_WIDTH

/**
 * Returns true if the current screen size is of the "Expanded" window size class as per
 * [Material Design 3 guidelines](https://m3.material.io/foundations/layout/applying-layout/window-size-classes).
 * This indicates a tablet in landscape or foldable in landscape (unfolded).
 */
fun Context.isExpandedWindowSize() =
    resources.configuration.screenWidthDp in SMALL_SCREEN_MAX_WIDTH..MEDIUM_SCREEN_MAX_WIDTH

/**
 * Returns true if the current screen size is of the "Large" window size class as per
 * [Material Design 3 guidelines](https://m3.material.io/foundations/layout/applying-layout/window-size-classes).
 * This indicates a desktop screen.
 */
fun Context.isLargeWindowSize() =
    resources.configuration.screenWidthDp in EXPANDED_SCREEN_MAX_WIDTH..LARGE_SCREEN_MAX_WIDTH

/**
 * Returns true if the current screen size is of the "Extra-large" window size class as per
 * [Material Design 3 guidelines](https://m3.material.io/foundations/layout/applying-layout/window-size-classes).
 * This indicates a desktop or ultra-wide screen.
 */
fun Context.isXLargeWindowSize() = resources.configuration.screenWidthDp >= LARGE_SCREEN_MAX_WIDTH

package com.woocommerce.android.extensions

import android.os.Parcelable
import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.woocommerce.android.R
import com.woocommerce.android.support.help.HelpActivity
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.math.abs

/**
 * A process-lifetime token used to tell nav results produced in the current process apart from ones restored from a
 * [SavedStateHandle] after the process was killed (e.g. a tablet woken after a long idle). It is stable across
 * configuration changes and regenerated on process death. See WOOMOB-3275.
 */
private val navResultSessionId: String = UUID.randomUUID().toString()

/**
 * Wraps a nav result together with the [session] that produced it. Only the observed value key is restored across
 * process death (its sibling keys are not), so the discriminator must live *inside* the persisted value. On restore
 * the envelope comes back with its original [session]; if that differs from [navResultSessionId] the result is a
 * leftover from a dead process and is dropped instead of replayed. See WOOMOB-3275.
 */
@Parcelize
private data class NavResultEnvelope(val session: String, val value: @RawValue Any?) : Parcelable

/** True when a restored nav result was produced by a different (already dead) process — see [NavResultEnvelope]. */
private fun Any.isStaleNavResult(): Boolean =
    this is NavResultEnvelope && session != navResultSessionId

/** Unwraps a [NavResultEnvelope]; a value written without the envelope is returned as-is (legacy behaviour). */
private fun Any.unwrapNavResult(): Any? =
    if (this is NavResultEnvelope) value else this

/**
 * A helper function that sets the submitted key-value pair in the Fragment's SavedStateHandle. The value can be
 * observed as a LiveData using the same key - see the Fragment.handleResult() extension function. This mechanism is
 * used to facilitate the request-result communication between 2 separate fragments.
 *
 * @param [key] A unique string that is the same as the one used in [handleResult]
 * @param [result] A result value to be returned
 * @param [destinationId] an optional destinationId, that can be used to navigate up to a specified destination
 * @param [popDestination] whether to pop the destinationId from the back stack when navigating back, used only
 *        when [destinationId] is specified
 * @param [navHostId] an optional navHostId, that can be used to navigate up to a specified navHostId
 *
 */
fun <T> Fragment.navigateBackWithResult(
    key: String,
    result: T,
    @IdRes destinationId: Int? = null,
    popDestination: Boolean = false,
    @IdRes navHostId: Int? = null,
) {
    val navController = if (navHostId != null) findNavController(navHostId) else findNavController()

    if (popDestination && destinationId != null) {
        navController.popBackStack(destinationId, true)
        navController.currentBackStackEntry?.savedStateHandle?.set(key, NavResultEnvelope(navResultSessionId, result))
    } else {
        val entry = if (destinationId != null) {
            navController.getBackStackEntry(destinationId)
        } else {
            navController.previousBackStackEntry
        }
        entry?.savedStateHandle?.set(key, NavResultEnvelope(navResultSessionId, result))

        if (destinationId != null) {
            navController.popBackStack(destinationId, false)
        } else {
            navController.navigateUp()
        }
    }
}

/**
 * A helper function that pops back stack to the [childId] and then invokes [navigateBackWithResult]
 * This is useful for scenarios when the fragment returning result doesn't know who their parent is since
 * they can be added to the navigation graph from various places of the app
 *
 * @param [key] A unique string that is the same as the one used in [handleResult]
 * @param [result] A result value to be returned
 * @param [childId] an destinationId, that used to navigate up from the specified destination
 */
fun <T> Fragment.navigateToParentWithResult(key: String, result: T, @IdRes childId: Int) {
    if (findNavController().currentDestination?.id != childId) {
        findNavController().popBackStack(childId, false)
    }
    navigateBackWithResult(key, result, null)
}

/**
 * A helper function that returns a notification in the Fragment's SavedStateHandle. Its purpose is to notify the caller
 * without returning any value. The notice can be observed as a LiveData using the same key - see the
 * Fragment.handleNotice() extension function.
 *
 * @param [key] A unique string that is the same as the one used in [handleNotice]
 * @param [destinationId] an optional destinationId, that can be used to navigating up to a specified destination
 * @param [popDestination] whether to pop the destinationId from the back stack when navigating back, used only
 *        when [destinationId] is specified
 * @param [navHostId] An optional ID of the NavHostFragment, it's useful when the fragment is used in two-pane layouts
 */
fun Fragment.navigateBackWithNotice(
    key: String,
    @IdRes destinationId: Int? = null,
    popDestination: Boolean = false,
    @IdRes navHostId: Int? = null,
) {
    navigateBackWithResult(key, key, destinationId, popDestination, navHostId)
}

/**
 * A helper function that subscribes a supplied handler function to the Fragment's SavedStateHandle LiveData associated
 * with the supplied key.
 *
 * @param [key] A unique string that is the same as the one used in [navigateBackWithResult]
 * @param [entryId] An optional ID to identify the correct back stack entry. It's required when calling [handleResult]
 *  from a Dialog (otherwise the result will get lost upon configuration change)
 * @param [navHostId] An optional ID of the NavHostFragment, it's useful when the fragment is used in two-pane layouts
 * @param [handler] A result handler
 *
 * Note: The value is consumed once — after the handler runs, the value is nulled so it won't be delivered again.
 * A result restored from a previous process (i.e. the app was killed and recreated, e.g. a tablet woken after a
 * long idle period) is discarded without invoking the handler, so a stale result cannot be replayed. See WOOMOB-3275.
 */
fun <T> Fragment.handleResult(
    key: String,
    @IdRes entryId: Int? = null,
    @IdRes navHostId: Int? = null,
    handler: (T) -> Unit
) {
    val navController = if (navHostId != null) findNavController(navHostId) else findNavController()

    val entry = if (entryId != null) {
        navController.getBackStackEntry(entryId)
    } else {
        navController.currentBackStackEntry
    }

    entry?.savedStateHandle?.let { saveState ->
        saveState.getLiveData<Any?>(key).observe(this.viewLifecycleOwner) { raw ->
            raw?.let { rawValue ->
                if (rawValue.isStaleNavResult()) {
                    // Restored from a dead process (e.g. a tablet woken after a long idle) — drop, don't replay.
                    saveState.set(key, null)
                    return@let
                }
                // A null payload keeps the legacy behaviour of not invoking the handler.
                rawValue.unwrapNavResult()?.let { payload ->
                    @Suppress("UNCHECKED_CAST")
                    handler(payload as T)
                }
                saveState.set(key, null)
            }
        }
    }
}

/**
 * A helper function that subscribes a supplied handler function to the dialog Fragment's SavedStateHandle LiveData
 * associated with the supplied key. This method *must* be used for handling results from dialogs because the entry ID
 * is required.
 *
 * @param [key] A unique string that is the same as the one used in [navigateBackWithResult]
 * @param [entryId] A mandatory ID to identify the correct back stack entry. It's required when calling [handleResult]
 *  from a Dialog (otherwise the result will get lost upon configuration change)
 * @param [navHostId] An optional ID of the NavHostFragment, it's useful when the fragment is used in two-pane layouts
 * @param [handler] A result handler
 *
 * Note: The value is consumed once — after the handler runs, the value is nulled so it won't be delivered again.
 * A result restored from a previous process (i.e. the app was killed and recreated, e.g. a tablet woken after a
 * long idle period) is discarded without invoking the handler, so a stale result cannot be replayed. See WOOMOB-3275.
 */
fun <T> Fragment.handleDialogResult(
    key: String,
    @IdRes entryId: Int,
    @IdRes navHostId: Int? = null,
    handler: (T) -> Unit
) {
    handleResult(key, entryId, navHostId, handler)
}

/**
 * A helper function that subscribes a supplied notice handler function to the dialog Fragment's SavedStateHandle
 * LiveData associated with the supplied key. Its purpose is to handle a notice without any value.
 * This method *must* be used for handling notices from dialogs because the entry ID is required.
 *
 * @param [key] A unique string that is the same as the one used in [navigateBackWithNotice]
 * @param [entryId] A mandatory ID to identify the correct back stack entry. It's required when calling [handleNotice]
 *  from a Dialog (otherwise the result will get lost upon configuration change)
 * @param [navHostId] An optional ID of the NavHostFragment, it's useful when the fragment is used in two-pane layouts
 * @param [handler] A result handler
 *
 * Note: The value is consumed once — after the handler runs, the value is nulled so it won't be delivered again.
 * A result restored from a previous process (i.e. the app was killed and recreated, e.g. a tablet woken after a
 * long idle period) is discarded without invoking the handler, so a stale result cannot be replayed. See WOOMOB-3275.
 */
fun Fragment.handleDialogNotice(
    key: String,
    @IdRes entryId: Int,
    @IdRes navHostId: Int? = null,
    handler: () -> Unit
) {
    handleNotice(key, entryId, navHostId, handler)
}

/**
 * A helper function that subscribes a supplied handler function to the Fragment's SavedStateHandle LiveData associated
 * with the supplied key. Its purpose is to handle a notice without any value.
 *
 * @param [key] A unique string that is the same as the one used in [navigateBackWithNotice]
 * @param [entryId] A mandatory ID to identify the correct back stack entry. It's required when calling [handleNotice]
 *  from a Dialog (otherwise the result will get lost upon configuration change)
 * @param [navHostId] An optional ID of the NavHostFragment, it's useful when the fragment is used in two-pane layouts
 * @param [handler] A result handler
 *
 * Note: The value is consumed once — after the handler runs, the value is nulled so it won't be delivered again.
 * A notice restored from a previous process (i.e. the app was killed and recreated) is discarded without invoking
 * the handler, so a stale notice cannot be replayed. See WOOMOB-3275.
 */
fun Fragment.handleNotice(
    key: String,
    @IdRes entryId: Int? = null,
    @IdRes navHostId: Int? = null,
    handler: () -> Unit
) {
    val navController = if (navHostId != null) findNavController(navHostId) else findNavController()

    val entry = if (entryId != null) {
        navController.getBackStackEntry(entryId)
    } else {
        navController.currentBackStackEntry
    }

    entry?.savedStateHandle?.let { saveState ->
        saveState.getLiveData<Any?>(key).observe(this.viewLifecycleOwner) { raw ->
            raw?.let { rawValue ->
                if (rawValue.isStaleNavResult()) {
                    // Restored from a dead process (e.g. a tablet woken after a long idle) — drop, don't replay.
                    saveState.set(key, null)
                    return@let
                }
                handler()
                saveState.set(key, null)
            }
        }
    }
}

/**
 * A helper function that apply a flow observation to changes of the Fragment view vertical offset,
 * making possible to keep the FAB button pinned into the same position as the view is scrolled
 *
 * @param [fabButton] The FAB button to be pinned in place using the App Bar Layout as reference
 */
fun Fragment.pinFabAboveBottomNavigationBar(fabButton: FloatingActionButton) {
    val appBarLayout = (requireActivity().findViewById<View>(R.id.app_bar_layout) as AppBarLayout)
    appBarLayout.verticalOffsetChanges()
        .onEach { verticalOffset ->
            fabButton.translationY =
                (abs(verticalOffset) - appBarLayout.totalScrollRange).toFloat()
        }.launchIn(viewLifecycleOwner.lifecycleScope)
}

fun Fragment.navigateToHelpScreen(origin: HelpOrigin) {
    startActivity(
        HelpActivity.createIntent(
            context = requireContext(),
            origin = origin,
            extraSupportTags = null
        )
    )
}

fun Fragment.showDateRangePicker(
    fromMillis: Long = System.currentTimeMillis(),
    toMillis: Long = System.currentTimeMillis(),
    onCustomRangeSelected: (Long, Long) -> Unit
) {
    fun shiftTime(time: Long, originalZoneId: ZoneId, targetZoneId: ZoneId): Long {
        // The timestamps returned by the MaterialDatePicker are in UTC timezone, if we use them directly
        // we will get a shift in the selected range.
        // To avoid this, we get the local date using UTC timestamps and then convert it to the selected timezone
        // See: https://github.com/material-components/material-components-android/issues/882
        val originalDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), originalZoneId).toLocalDateTime()
        val targetDateTime = ZonedDateTime.of(originalDateTime, targetZoneId)
        return targetDateTime.toInstant().toEpochMilli()
    }

    val deviceZoneId = ZoneId.systemDefault()
    val datePicker = MaterialDatePicker.Builder.dateRangePicker()
        .setTitleText(getString(R.string.orderfilters_date_range_picker_title))
        .setSelection(
            androidx.core.util.Pair(
                shiftTime(fromMillis, deviceZoneId, ZoneId.of("UTC")),
                shiftTime(toMillis, deviceZoneId, ZoneId.of("UTC"))
            )
        )
        .setCalendarConstraints(
            CalendarConstraints.Builder()
                .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                .setValidator(DateValidatorPointBackward.now())
                .build()
        )
        .build()

    datePicker.show(parentFragmentManager, AnalyticsHubFragment.DATE_PICKER_FRAGMENT_TAG)
    datePicker.addOnPositiveButtonClickListener {
        val start = shiftTime(it.first ?: 0, ZoneId.of("UTC"), deviceZoneId)
        val end = shiftTime(it.second ?: 0, ZoneId.of("UTC"), deviceZoneId)

        onCustomRangeSelected(start, end)
    }
}

/**
 * Finds the NavController associated with the NavHostFragment with the given ID,
 * useful when trying to find a parent NavController from a child fragment used in two-pane layouts.
 *
 * @param navHostId The ID of the NavHostFragment
 * @return The NavController
 */
fun Fragment.findNavController(@IdRes navHostId: Int): NavController {
    var parentFragment = parentFragment
    while (parentFragment != null) {
        if (parentFragment is NavHostFragment && parentFragment.id == navHostId) {
            return parentFragment.navController
        }
        parentFragment = parentFragment.parentFragment
    }
    error("No NavHostFragment found with id $navHostId")
}

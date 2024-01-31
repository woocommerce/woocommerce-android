package com.woocommerce.android.viewmodel

import android.app.Activity
import android.content.DialogInterface.OnClickListener
import android.view.View
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R.string
import com.woocommerce.android.model.UiString
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.dialog.DialogParams
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.dialog.WooDialogFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and Snackbar messages.
 *
 *
 * This avoids a common problem with events: on configuration change (like rotation) an update
 * can be emitted if the observer is active. This LiveData only calls the observable if there's an
 * explicit call to setValue() or call().
 *
 *
 * This is a mutation of SingleLiveEvent, which allows multiple observers. Once an observer marks the event as handled,
 * no other observers are notified and no further updates will be sent, similar to SingleLiveEvent.
 */
open class MultiLiveEvent<T : Event> : MutableLiveData<T>() {
    private val pending = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        // Observe the internal MutableLiveData
        super.observe(owner) { t ->
            if (pending.get()) {
                t.isHandled = true
                observer.onChanged(t)
                pending.compareAndSet(t.isHandled, false)
            }
        }
    }

    fun reset() {
        pending.set(false)
    }

    @MainThread
    override fun setValue(t: T?) {
        pending.set(true)
        super.setValue(t)
    }

    override fun postValue(value: T) {
        pending.set(true)
        super.postValue(value)
    }

    @Suppress("UnnecessaryAbstractClass")
    abstract class Event(var isHandled: Boolean = false) {
        data class ShowSnackbar(
            @StringRes val message: Int,
            val args: Array<String> = arrayOf(),
            val undoAction: (() -> Unit)? = null
        ) : Event() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is ShowSnackbar) return false

                if (message != other.message) return false
                if (!args.contentEquals(other.args)) return false
                if (undoAction != other.undoAction) return false

                return true
            }

            override fun hashCode(): Int {
                var result = message
                result = 31 * result + args.contentHashCode()
                result = 31 * result + (undoAction?.hashCode() ?: 0)
                return result
            }
        }

        data class ShowUndoSnackbar(
            val message: String,
            val args: Array<String> = arrayOf(),
            val undoAction: View.OnClickListener,
            val dismissAction: Snackbar.Callback = Snackbar.Callback()
        ) : Event() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is ShowUndoSnackbar) return false

                if (message != other.message) return false
                if (!args.contentEquals(other.args)) return false
                if (undoAction != other.undoAction) return false
                if (dismissAction != other.dismissAction) return false

                return true
            }

            override fun hashCode(): Int {
                var result = message.hashCode()
                result = 31 * result + args.contentHashCode()
                result = 31 * result + undoAction.hashCode()
                result = 31 * result + dismissAction.hashCode()
                return result
            }
        }

        data class ShowActionSnackbar(
            val message: String,
            val actionText: String,
            val action: View.OnClickListener
        ) : Event()

        data class ShowUiStringSnackbar(val message: UiString) : Event()

        object Logout : Event()
        object Exit : Event()

        data class NavigateToHelpScreen(val origin: HelpOrigin) : Event()

        data class ExitWithResult<out T>(val data: T, val key: String? = null) : Event()

        data class LaunchUrlInChromeTab(val url: String) : Event()

        data class ShowDialogFragment(
            @StringRes val titleId: Int? = null,
            @StringRes val messageId: Int? = null,
            @StringRes val positiveButtonId: Int? = null,
            @StringRes val negativeButtonId: Int? = null,
            @StringRes val neutralButtonId: Int? = null,
            val positiveActionName: String? = null,
            val negativeActionName: String? = null,
            val neutralActionName: String? = null,
            val cancelable: Boolean = true
        ) : Event() {
            fun showIn(fragmentManager: androidx.fragment.app.FragmentManager, listener: WooDialogFragment.DialogInteractionListener) {
                val dialogParams = DialogParams(
                    titleId = titleId,
                    messageId = messageId,
                    positiveButtonId = positiveButtonId,
                    negativeButtonId = negativeButtonId,
                    neutralButtonId = neutralButtonId,
                    cancelable = cancelable
                )
                val dialogFragment = WooDialogFragment.newInstance(dialogParams).apply {
                    setDialogInteractionListener(listener)
                }
                dialogFragment.show(fragmentManager, WooDialogFragment.TAG)
            }
        }

        data class ShowDialog(
            @StringRes val titleId: Int? = null,
            @StringRes val messageId: Int? = null,
            @StringRes val positiveButtonId: Int? = null,
            @StringRes val negativeButtonId: Int? = null,
            @StringRes val neutralButtonId: Int? = null,
            val positiveBtnAction: OnClickListener? = null,
            val negativeBtnAction: OnClickListener? = null,
            val neutralBtnAction: OnClickListener? = null,
            val cancelable: Boolean = true,
            val onDismiss: (() -> Unit)? = null
        ) : Event() {
            fun showIn(activity: Activity) {
                WooDialog.showDialog(
                    activity = activity,
                    titleId = this.titleId,
                    messageId = this.messageId,
                    positiveButtonId = this.positiveButtonId,
                    posBtnAction = this.positiveBtnAction,
                    negativeButtonId = this.negativeButtonId,
                    negBtnAction = this.negativeBtnAction,
                    neutralButtonId = this.neutralButtonId,
                    neutBtAction = this.neutralBtnAction,
                    cancellable = this.cancelable,
                    onDismiss = this.onDismiss
                )
            }

            companion object {
                fun buildDiscardDialogEvent(
                    messageId: Int = string.discard_message,
                    positiveButtonId: Int = string.discard,
                    negativeButtonId: Int = string.keep_editing,
                    positiveBtnAction: OnClickListener,
                    negativeBtnAction: OnClickListener? = null
                ) = ShowDialog(
                    messageId = messageId,
                    positiveButtonId = positiveButtonId,
                    positiveBtnAction = positiveBtnAction,
                    negativeButtonId = negativeButtonId,
                    negativeBtnAction = negativeBtnAction
                )
            }
        }
    }
}

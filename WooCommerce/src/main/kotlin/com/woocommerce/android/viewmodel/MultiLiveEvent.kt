package com.woocommerce.android.viewmodel

import android.content.DialogInterface.OnClickListener
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
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
        super.observe(owner, Observer { t ->
            if (pending.get()) {
                t.isHandled = true
                observer.onChanged(t)
                pending.compareAndSet(t.isHandled, false)
            }
        })
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

        object Exit : Event()

        data class ShowDiscardDialog(
            val positiveBtnAction: OnClickListener? = null,
            val negativeBtnAction: OnClickListener? = null,
            @StringRes val messageId: Int? = null,
            @StringRes val titleId: Int? = null,
            @StringRes val positiveButtonId: Int? = null,
            @StringRes val negativeButtonId: Int? = null
        ) : Event() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is ShowDiscardDialog) return false

                if (titleId != other.titleId) return false
                if (messageId != other.messageId) return false
                if (positiveButtonId != other.positiveButtonId) return false
                if (negativeButtonId != other.negativeButtonId) return false
                if (positiveBtnAction != other.positiveBtnAction) return false
                if (negativeBtnAction != other.negativeBtnAction) return false

                return true
            }

            override fun hashCode(): Int {
                var result = positiveBtnAction?.hashCode() ?: 0
                result = 31 * result + (negativeBtnAction?.hashCode() ?: 0)
                return result
            }
        }
    }
}

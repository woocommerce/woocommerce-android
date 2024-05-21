package com.woocommerce.android

import org.greenrobot.eventbus.Subscribe
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action

class FakeDispatcher(private val dispatchCallback: Dispatcher.(action: Action<*>) -> Unit = {}) : Dispatcher() {
    private val listeners = mutableListOf<Any>()

    @Synchronized
    override fun register(`object`: Any) {
        listeners.add(`object`)
    }

    override fun unregister(`object`: Any) {
        listeners.remove(`object`)
    }

    override fun emitChange(changeEvent: Any) {
        // Copy the listeners to avoid any issues because of concurrent changes
        val listenersCopy = listeners.toList()

        listenersCopy.forEach { listener ->
            listener::class.java.methods
                .filter { method -> method.annotations.any { it.annotationClass == Subscribe::class } }
                .forEach { method ->
                    if (method.parameters.single().type.isAssignableFrom(changeEvent::class.java)) {
                        method.invoke(listener, changeEvent)
                    }
                }
        }
    }

    override fun dispatch(action: Action<*>) {
        dispatchCallback(action)
    }
}

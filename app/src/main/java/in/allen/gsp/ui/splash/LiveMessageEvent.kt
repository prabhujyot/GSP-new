package `in`.allen.gsp.ui.splash

import `in`.allen.gsp.utils.SingleLiveEvent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

class LiveMessageEvent<T>: SingleLiveEvent<(T.() -> Unit)?>() {

    fun setEventReceiver(owner: LifecycleOwner, receiver: T) {
        observe(owner, Observer { event ->
            if ( event != null ) {
                receiver.event()
            }
        })
    }

    fun sendEvent(event: (T.() -> Unit)?) {
        value = event
    }
}
package com.woocommerce.android.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<MutableList<T>>.addNewItem(item: T) {
    val oldValue = this.value ?: mutableListOf()
    oldValue.add(item)
    this.value = oldValue
}

fun <T> MutableLiveData<MutableList<T>>.containsItem(item: T): Boolean {
    val oldValue = this.value ?: mutableListOf()
    return oldValue.contains(item)
}

fun <T> MutableLiveData<MutableList<T>>.isEmpty(): Boolean {
    val oldValue = this.value ?: mutableListOf()
    return oldValue.isEmpty()
}

fun <T> MutableLiveData<MutableList<T>>.removeItem(item: T) {
    if (!this.value.isNullOrEmpty()) {
        val oldValue = this.value
        oldValue?.remove(item)
        this.value = oldValue
    } else {
        this.value = mutableListOf()
    }
}

fun <T> MutableLiveData<MutableList<T>>.getList(): MutableList<T> =
    this.value ?: mutableListOf()

fun <T> MutableLiveData<MutableList<T>>.clearList() {
    this.value?.clear()
}

fun <T> LiveData<T>.drop(number: Int): LiveData<T> {
    val outputLiveData: MediatorLiveData<T> = MediatorLiveData<T>()
    var remainingItemsToSkip: Int = number
    outputLiveData.addSource(this) {
        if (it == null) return@addSource
        if (remainingItemsToSkip != 0) {
            remainingItemsToSkip--
            return@addSource
        }
        outputLiveData.value = it
    }
    return outputLiveData
}

fun <T : Any> LiveData<T?>.filterNotNull(): LiveData<T> {
    val mediator = MediatorLiveData<T>()
    mediator.addSource(this) {
        if (it != null) {
            mediator.value = it
        }
    }
    return mediator
}

fun <T> LiveData<T>.filter(predicate: (T) -> Boolean): LiveData<T> {
    val mediator = MediatorLiveData<T>()
    mediator.addSource(this) {
        if (it != null && predicate(it)) {
            mediator.value = it
        }
    }
    return mediator
}

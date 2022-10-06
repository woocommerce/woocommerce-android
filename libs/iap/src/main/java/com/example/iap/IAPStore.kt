package com.example.iap

import com.example.iap.model.IAPProduct

interface IAPStore {
    fun confirmOrderOnServer(iapProduct: IAPProduct)
}

package com.woocommerce.android.ui.main

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.woocommerce.android.R
class MainActivity : AppCompatActivity(), MainContract.View {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}

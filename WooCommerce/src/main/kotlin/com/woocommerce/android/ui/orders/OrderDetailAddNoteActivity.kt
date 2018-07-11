package com.woocommerce.android.ui.orders

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.woocommerce.android.R
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class OrderDetailAddNoteActivity : AppCompatActivity(), OrderDetailAddNoteContract.View {
    companion object {
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"
    }

    @Inject lateinit var presenter: OrderDetailAddNoteContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail_add_note)
        setSupportActionBar(toolbar as Toolbar)

        presenter.takeView(this)
    }

    override fun onDestroy() {
        presenter.dropView()
        super.onDestroy()
    }
}

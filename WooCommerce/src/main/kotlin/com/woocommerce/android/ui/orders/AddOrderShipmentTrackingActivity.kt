package com.woocommerce.android.ui.orders

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.activity_add_shipment_tracking.*

class AddOrderShipmentTrackingActivity : AppCompatActivity() {
    companion object {
        const val FIELD_ORDER_NUMBER = "order-number"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_shipment_tracking)

        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)
        title = getString(R.string.order_shipment_tracking_toolbar_title)
    }

    /**
     * Reusing the same menu used for adding order notes
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_note, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.menu_add -> {
                // TODO: add button functionality to be added
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

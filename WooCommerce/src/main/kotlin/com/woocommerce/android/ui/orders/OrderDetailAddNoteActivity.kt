package com.woocommerce.android.ui.orders

import android.app.Activity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.woocommerce.android.R
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_order_detail_add_note.*
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class OrderDetailAddNoteActivity : AppCompatActivity(), OrderDetailAddNoteContract.View {
    companion object {
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"
        const val FIELD_ORDER_NUMBER = "order-number"
        const val FIELD_IS_ADDING_NOTE = "is_adding_note"
        const val FIELD_IS_CUSTOMER_NOTE = "is_customer_note"
    }

    @Inject lateinit var presenter: OrderDetailAddNoteContract.Presenter
    private lateinit var orderId: OrderIdentifier
    private lateinit var orderNumber: String
    private var isAddingNote = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail_add_note)

        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)

        if (savedInstanceState == null) {
            orderId = intent.getStringExtra(FIELD_ORDER_IDENTIFIER)
            orderNumber = intent.getStringExtra(FIELD_ORDER_NUMBER)
        } else {
            orderId = savedInstanceState.getString(FIELD_ORDER_IDENTIFIER)
            orderNumber = savedInstanceState.getString(FIELD_ORDER_NUMBER)
            isAddingNote = savedInstanceState.getBoolean(FIELD_IS_ADDING_NOTE)
            addNote_switch.isChecked = savedInstanceState.getBoolean(FIELD_IS_CUSTOMER_NOTE)
        }

        title = getString(R.string.orderdetail_orderstatus_ordernum, orderNumber)

        addNote_switch.setOnClickListener {
            val isPublic = addNote_switch.isChecked
            val drawableId = if (isPublic) R.drawable.ic_note_public else R.drawable.ic_note_private
            addNote_icon.setImageDrawable(ContextCompat.getDrawable(this, drawableId))
        }

        presenter.takeView(this)
    }

    override fun onDestroy() {
        presenter.dropView()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (!isAddingNote) {
            super.onBackPressed()
        }
    }

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
                val noteText = addNote_editor.text.toString()
                if (!noteText.isEmpty() && NetworkUtils.checkConnection(this)) {
                    val isCustomerNote = addNote_switch.isChecked
                    presenter.pushOrderNote(orderId, noteText, isCustomerNote)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString(FIELD_ORDER_IDENTIFIER, orderId)
        outState?.putString(FIELD_ORDER_NUMBER, orderNumber)
        outState?.putBoolean(FIELD_IS_CUSTOMER_NOTE, addNote_switch.isChecked)
        super.onSaveInstanceState(outState)
    }

    override fun showNullOrderError() {
        ToastUtils.showToast(this, R.string.orderdetail_add_note_null_order_error)
    }

    override fun doBeforeAddNote() {
        isAddingNote = true
        progressBar.visibility = View.VISIBLE
        addNote_editor.isEnabled = false
        addNote_switch.isEnabled = false
    }

    override fun doAfterAddNote(didSucced: Boolean) {
        isAddingNote = false

        progressBar.visibility = View.GONE
        addNote_editor.isEnabled = true
        addNote_switch.isEnabled = true

        if (didSucced) {
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            ToastUtils.showToast(this, R.string.orderdetail_add_note_error)
        }
    }
}

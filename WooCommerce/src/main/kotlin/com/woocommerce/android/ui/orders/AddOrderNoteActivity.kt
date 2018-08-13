package com.woocommerce.android.ui.orders

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.woocommerce.android.R
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_add_order_note.*
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject

class AddOrderNoteActivity : AppCompatActivity(), AddOrderNoteContract.View {
    companion object {
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"
        const val FIELD_ORDER_NUMBER = "order-number"
        const val FIELD_NOTE_TEXT = "note_text"
        const val FIELD_IS_CUSTOMER_NOTE = "is_customer_note"
        const val FIELD_IS_CONFIRMING_DISCARD = "is_confirming_discard"
    }

    @Inject lateinit var presenter: AddOrderNoteContract.Presenter

    private lateinit var orderId: OrderIdentifier
    private lateinit var orderNumber: String
    private var isConfirmingDiscard = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_order_note)

        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)

        if (savedInstanceState == null) {
            orderId = intent.getStringExtra(FIELD_ORDER_IDENTIFIER)
            orderNumber = intent.getStringExtra(FIELD_ORDER_NUMBER)
        } else {
            orderId = savedInstanceState.getString(FIELD_ORDER_IDENTIFIER)
            orderNumber = savedInstanceState.getString(FIELD_ORDER_NUMBER)
            addNote_switch.isChecked = savedInstanceState.getBoolean(FIELD_IS_CUSTOMER_NOTE)
            if (savedInstanceState.getBoolean(FIELD_IS_CONFIRMING_DISCARD)) {
                confirmDiscard()
            }
        }

        if (presenter.hasBillingEmail(orderId)) {
            addNote_switch.setOnCheckedChangeListener { buttonView, isChecked ->
                val drawableId = if (isChecked) R.drawable.ic_note_public else R.drawable.ic_note_private
                addNote_icon.setImageDrawable(ContextCompat.getDrawable(this, drawableId))
            }
        } else {
            addNote_switchContainer.visibility = View.GONE
            addNote_switchDivider.visibility = View.GONE
            addNote_editDivider.visibility = View.GONE
        }

        title = getString(R.string.orderdetail_orderstatus_ordernum, orderNumber)

        presenter.takeView(this)
    }

    override fun onDestroy() {
        presenter.dropView()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_note, menu)
        return true
    }

    override fun onBackPressed() {
        if (getNoteText().isNotEmpty()) {
            confirmDiscard()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.menu_add -> {
                val noteText = getNoteText()
                if (!noteText.isEmpty() && NetworkUtils.checkConnection(this)) {
                    val isCustomerNote = addNote_switch.isChecked
                    val data = Intent()
                    data.putExtra(FIELD_NOTE_TEXT, noteText)
                    data.putExtra(FIELD_IS_CUSTOMER_NOTE, isCustomerNote)
                    setResult(Activity.RESULT_OK, data)
                    finish()
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
        outState?.putBoolean(FIELD_IS_CONFIRMING_DISCARD, isConfirmingDiscard)
        super.onSaveInstanceState(outState)
    }

    override fun getNoteText() = addNote_editor.text.toString().trim()

    override fun confirmDiscard() {
        isConfirmingDiscard = true
        AlertDialog.Builder(this)
                .setMessage(R.string.add_order_note_confirm_discard)
                .setCancelable(true)
                .setPositiveButton(R.string.discard) { _, _ ->
                    finish()
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    isConfirmingDiscard = false
                }
                .show()
    }
}

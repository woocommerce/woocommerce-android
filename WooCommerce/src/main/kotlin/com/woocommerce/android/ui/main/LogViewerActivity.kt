package com.woocommerce.android.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.extensions.setHtmlText
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.android.synthetic.main.activity_main.*
import org.wordpress.android.util.ToastUtils
import java.lang.String.format
import java.util.ArrayList
import java.util.Locale

class LogViewerActivity : AppCompatActivity() {
    companion object {
        private const val ID_SHARE = 1
        private const val ID_COPY_TO_CLIPBOARD = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logviewer)

        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val listView = findViewById<View>(android.R.id.list) as ListView
        listView.adapter = LogAdapter(this)
    }

    private fun shareAppLog() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, WooLog.toList(false))
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " " + title)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (ex: android.content.ActivityNotFoundException) {
            ToastUtils.showToast(this, R.string.logviewer_share_error)
        }
    }

    private fun copyAppLogToClipboard() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip = ClipData.newPlainText("AppLog", WooLog.toList(false).toString())
            ToastUtils.showToast(this, R.string.logviewer_copied_to_clipboard)
        } catch (e: Exception) {
            WooLog.e(T.UTILS, e)
            ToastUtils.showToast(this, R.string.logviewer_error_copy_to_clipboard)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        // Copy to clipboard button
        var item = menu.add(Menu.NONE, ID_COPY_TO_CLIPBOARD, Menu.NONE, android.R.string.copy)
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        item.setIcon(R.drawable.abc_ic_menu_copy_mtrl_am_alpha)

        // Share button
        item = menu.add(Menu.NONE, ID_SHARE, Menu.NONE, R.string.share)
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        item.setIcon(R.drawable.abc_ic_menu_share_mtrl_alpha)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            ID_SHARE -> {
                shareAppLog()
                true
            }
            ID_COPY_TO_CLIPBOARD -> {
                copyAppLogToClipboard()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private inner class LogAdapter constructor(context: Context) : BaseAdapter() {
        private val entries: ArrayList<String> = WooLog.toList(true)
        private val inflater: LayoutInflater = LayoutInflater.from(context)

        override fun getCount(): Int {
            return entries.size
        }

        override fun getItem(position: Int): Any {
            return entries[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View
            val holder: LogViewHolder
            if (convertView == null) {
                view = inflater.inflate(R.layout.logviewer_listitem, parent, false)
                holder = LogViewHolder(view)
                view.tag = holder
            } else {
                view = convertView
                holder = view.tag as LogViewHolder
            }

            holder.txtLineNumber.text = format(Locale.US, "%02d", position)
            holder.txtLogEntry.setHtmlText(entries[position])

            return view
        }

        private inner class LogViewHolder internal constructor(view: View) {
            val txtLineNumber: TextView = view.findViewById(R.id.text_line) as TextView
            val txtLogEntry: TextView = view.findViewById(R.id.text_log) as TextView
        }
    }
}

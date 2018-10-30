package com.woocommerce.android.ui.main

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.util.WooLog
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.String.format
import java.util.ArrayList
import java.util.Locale

class LogViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logviewer)

        setSupportActionBar(toolbar as Toolbar)
        actionBar?.let {
            it.setDisplayShowTitleEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            it.setTitle(R.string.logviewer_activity_title)
        }

        val listView = findViewById<View>(android.R.id.list) as ListView
        listView.adapter = LogAdapter(this)
    }

    inner class LogAdapter private constructor(context: Context) : BaseAdapter() {
        private val entries: ArrayList<String> = WooLog.toList()
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
            var view = convertView
            val holder: LogViewHolder
            if (view == null) {
                view = inflater.inflate(R.layout.logviewer_listitem, parent, false)
                holder = LogViewHolder(view)
                view.tag = holder
            } else {
                holder = convertView.tag as LogViewHolder
            }

            holder.txtLineNumber.text = format(Locale.US, "%02d", position)
            holder.txtLogEntry.text = entries[position]

            return view
        }

        private inner class LogViewHolder internal constructor(view: View) {
            val txtLineNumber: TextView = view.findViewById(R.id.text_line) as TextView
            val txtLogEntry: TextView = view.findViewById(R.id.text_log) as TextView
        }
    }
}

package com.secuso.privacyfriendlycodescanner.qrscanner.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.RecyclerView
import com.secuso.privacyfriendlycodescanner.qrscanner.R

class EditableListAdapter(val buttonAction: (data: Data) -> Unit, val textAction: (data: Data) -> Unit) : RecyclerView.Adapter<EditableListAdapter.ViewHolder>() {
    private var data: ArrayList<Data> = ArrayList()

    fun updateData(newData: Collection<Data>) {
        data.clear()
        data.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.simple_list_item, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = data[position].text
        holder.textView.setOnClickListener {
            textAction(data[position])
        }
        holder.button.setOnClickListener {
            buttonAction(data[position])
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView
        val button: AppCompatImageButton

        init {
            textView = view.findViewById(R.id.textView)
            button = view.findViewById(R.id.delete_button)
        }
    }

    class Data(val text: String, val id: Int)
}
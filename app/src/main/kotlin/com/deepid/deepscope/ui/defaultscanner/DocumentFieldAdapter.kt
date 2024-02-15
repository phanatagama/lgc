package com.deepid.deepscope.ui.defaultscanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.deepid.deepscope.R
import com.deepid.deepscope.domain.model.TextFieldAttribute

class DocumentFieldAdapter :
    ListAdapter<TextFieldAttribute, DocumentFieldAdapter.DocumentFieldViewHolder>(DiffCallback) {
    inner class DocumentFieldViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.title_tv)
        private val content: TextView = itemView.findViewById(R.id.content_tv)
        fun bind(item: TextFieldAttribute) {
            title.text = item.name
            content.text = item.value

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentFieldViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return DocumentFieldViewHolder(layoutInflater.inflate(R.layout.item_text_result, parent, false))
    }

    override fun onBindViewHolder(holder: DocumentFieldViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<TextFieldAttribute>() {
        override fun areItemsTheSame(
            oldItem: TextFieldAttribute,
            newItem: TextFieldAttribute
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: TextFieldAttribute,
            newItem: TextFieldAttribute
        ): Boolean {
            return oldItem.value == newItem.value
        }
    }
}
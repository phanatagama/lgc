package com.deepscope.deepscope.ui.customerInformation.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.deepscope.deepscope.data.common.toDateString
import com.deepscope.deepscope.databinding.RvItemCustomerInformationBinding
import com.deepscope.deepscope.domain.model.CustomerInformation

class CustomerInformationAdapter :
    ListAdapter<CustomerInformation, CustomerInformationAdapter.ViewHolder>(
        DiffCallback
    ) {
    interface OnItemClickListener {
        fun onItemClickListener(
            view: RvItemCustomerInformationBinding,
            customerInformation: CustomerInformation
        )

        fun onDeleteItem(customerInformation: CustomerInformation)
    }

    var listener: OnItemClickListener? = null

    inner class ViewHolder(val binding: RvItemCustomerInformationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(customerInformation: CustomerInformation) {
            with(binding) {
                titleTv.text = customerInformation.name
                issueTv.text = customerInformation.issueDate.toDateString()
                addressTv.text = customerInformation.address
            }
        }

    }

    object DiffCallback : DiffUtil.ItemCallback<CustomerInformation>() {
        override fun areItemsTheSame(
            oldItem: CustomerInformation,
            newItem: CustomerInformation
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: CustomerInformation,
            newItem: CustomerInformation
        ): Boolean {
            return oldItem.id == newItem.id
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        RvItemCustomerInformationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.binding.root.setOnClickListener {
            listener?.onItemClickListener(holder.binding, item)
        }
        holder.binding.btnDelete.setOnClickListener {
            listener?.onDeleteItem(item)
        }
    }
}
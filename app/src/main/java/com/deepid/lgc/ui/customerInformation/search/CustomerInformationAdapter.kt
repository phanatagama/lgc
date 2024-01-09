package com.deepid.lgc.ui.customerInformation.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.deepid.lgc.databinding.RvItemCustomerInformationBinding
import com.deepid.lgc.domain.model.CustomerInformation

class CustomerInformationAdapter :
    ListAdapter<CustomerInformation, CustomerInformationAdapter.ViewHolder>(
        DiffCallback
    ) {
    inner class ViewHolder(private val binding: RvItemCustomerInformationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(customerInformation: CustomerInformation) {
            with(binding){
                titleTv.text = customerInformation.name
                issueTv.text = customerInformation.issueDate
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
        holder.bind(getItem(position))
    }
}
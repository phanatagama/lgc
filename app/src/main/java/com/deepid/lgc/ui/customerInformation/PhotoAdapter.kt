package com.deepid.lgc.ui.customerInformation

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.deepid.lgc.databinding.RvItemImageBinding
import com.deepid.lgc.domain.model.DataImage
import java.io.File


class PhotoAdapter :
    ListAdapter<DataImage, PhotoAdapter.PhotoAdapterViewHolder>(DiffCallback) {
    var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClickListener(view: View, dataImage: DataImage)
    }

    inner class PhotoAdapterViewHolder(val binding: RvItemImageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DataImage) {
            with(binding) {
                item.bitmap?.let {
                    addIcon.visibility = View.GONE
                    image.setImageBitmap(it)
                }
                item.path?.let {
                    addIcon.visibility = View.GONE
                    val imgFile = File(it)
                    if (imgFile.exists()) {
                        image.setImageURI(Uri.fromFile(imgFile))
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoAdapterViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return PhotoAdapterViewHolder(RvItemImageBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: PhotoAdapterViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.binding.imageCard.setOnClickListener { listener?.onItemClickListener(it, item) }
    }

    fun updateList(dataImage: DataImage) {
        val newList = currentList.toMutableList().map {
            if (it.id == dataImage.id) {
                Log.d("DEBUGX", "DEBUGX updateList: CHANGED")
                it.copy(bitmap = dataImage.bitmap)
            } else {
                it
            }
        }
        submitList(newList)
    }

    fun updateList(rawImage: DataImage, uvImage: DataImage) {
        val filterList = currentList.toMutableList().filter { data ->
            data.id != rawImage.id && data.id != uvImage.id
        }
        val newList = listOf(rawImage, uvImage) + filterList
        submitList(newList)
    }

    object DiffCallback : DiffUtil.ItemCallback<DataImage>() {
        override fun areItemsTheSame(
            oldItem: DataImage,
            newItem: DataImage
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: DataImage,
            newItem: DataImage
        ): Boolean {
            return oldItem.id == newItem.id
        }
    }
}
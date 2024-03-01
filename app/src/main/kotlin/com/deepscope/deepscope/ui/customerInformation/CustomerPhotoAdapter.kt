package com.deepscope.deepscope.ui.customerInformation

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.deepscope.deepscope.databinding.RvItemImageBinding
import com.deepscope.deepscope.domain.model.DataImage
import timber.log.Timber
import java.io.File


class CustomerPhotoAdapter :
    ListAdapter<DataImage, CustomerPhotoAdapter.PhotoAdapterViewHolder>(DiffCallback) {
    var listener: OnItemClickListener? = null
    var parentType: Int = 1

    interface OnItemClickListener {
        fun onItemClickListener(view: View, dataImage: DataImage)
        fun onItemDeleteClickListener(view: View, dataImage: DataImage)
    }

    inner class PhotoAdapterViewHolder(val binding: RvItemImageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private fun setVisibility() {
            with(binding) {
                addIcon.visibility = View.GONE
                deleteIcon.visibility = View.VISIBLE
            }
        }

        fun hideDeleteIcon() {
            with(binding) {
                deleteIcon.visibility = View.GONE
            }
        }

        fun bind(item: DataImage) {
            with(binding) {
                if (item.bitmap == null && item.path == null) {
                    addIcon.visibility = View.VISIBLE
                    deleteIcon.visibility = View.GONE
                    image.setImageResource(android.R.color.darker_gray)
                    return
                }
                item.bitmap?.let {
                    image.setImageBitmap(it)
                }
                item.path?.let {
                    val imgFile = File(it)
                    if (imgFile.exists()) {
                        image.setImageURI(Uri.fromFile(imgFile))
                    }
                }
                setVisibility()

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
        holder.binding.deleteIcon.setOnClickListener {
            listener?.onItemDeleteClickListener(
                it,
                item
            )
        }
    }

    fun updateList(dataImage: DataImage) {
        val newList = currentList.toMutableList().map {
            if (it.id == dataImage.id) {
                Timber.d( "DEBUGX updateList: CHANGED")
                it.copy(bitmap = dataImage.bitmap, type = dataImage.type)
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
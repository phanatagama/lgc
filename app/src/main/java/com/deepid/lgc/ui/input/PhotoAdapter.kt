package com.deepid.lgc.ui.input

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.deepid.lgc.R

data class DataImage(
    val id:Int,
    val bitmap: Bitmap
)
class PhotoAdapter :
    ListAdapter<DataImage, PhotoAdapter.PhotoAdapterViewHolder>(DiffCallback) {
    inner class PhotoAdapterViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.image)
        fun bind(item: DataImage) {
            imageView.setImageBitmap(item.bitmap)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoAdapterViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return PhotoAdapterViewHolder(layoutInflater.inflate(R.layout.item_image, parent, false))
    }

    override fun onBindViewHolder(holder: PhotoAdapterViewHolder, position: Int) {
        holder.bind(getItem(position))
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
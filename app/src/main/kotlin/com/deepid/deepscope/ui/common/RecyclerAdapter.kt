package com.deepid.deepscope.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.deepid.deepscope.R
import com.deepid.deepscope.databinding.RvItemMenuBinding
import com.deepid.deepscope.util.Base
import com.deepid.deepscope.util.ItemMenu
import com.deepid.deepscope.util.Utils.getColorResource

class RecyclerAdapter(private val items: List<Base>) :
    RecyclerView.Adapter<RecyclerAdapter.VH>() {
    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ItemMenu -> ITEM_MENU
        else -> ITEM_MENU
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val li = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_MENU -> VH.ItemMenuVH(RvItemMenuBinding.inflate(li,parent,false))
            else -> VH.ItemMenuVH(RvItemMenuBinding.inflate(li,parent,false))
        }
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(vh: VH, i: Int) = vh.bind(items[i])

    sealed class VH(v: View) : RecyclerView.ViewHolder(v) {
        val context: Context = v.context
        abstract fun bind(base: Base)

        class ItemMenuVH(private val binding: RvItemMenuBinding): VH(binding.root){
            override fun bind(base: Base) {
                val itemMenu = base as ItemMenu
                val color = getColorResource(if(itemMenu.isActive) R.color.secondary_color else R.color.background_color, context)
                with(binding){
                    menuTitleTv.text = itemMenu.title
                    menuIconTv.setImageDrawable(itemMenu.image)
                    menuLayout.setCardBackgroundColor(color)
                    root.setOnClickListener {
                        itemMenu.onClick()
                    }
                }
            }
        }
    }

    companion object {
        private const val SECTION = 0
        private const val BUTTON = 1
        private const val SWITCH = 2
        private const val STEPPER = 3
        private const val BOTTOM_SHEET = 4
        private const val BOTTOM_SHEET_MULTI = -2
        private const val INPUT_INT = 5
        private const val INPUT_STRING = 6
        private const val TEXT_RESULT = 7
        private const val IMAGE = 8
        private const val STATUS = 9
        private const val INPUT_DOUBLE = 10
        private const val HEADER = 11
        private const val ITEM_MENU = 12
    }
}
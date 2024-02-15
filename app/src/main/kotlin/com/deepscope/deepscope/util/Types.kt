package com.deepscope.deepscope.util

import android.graphics.drawable.Drawable

abstract class Base(
    val title: String
)

class ItemMenu(
    title: String,
    val image: Drawable,
    var isActive: Boolean = true,
    var onClick: () -> Unit = {}
) : Base(title)
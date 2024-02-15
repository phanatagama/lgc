package com.deepid.deepscope.domain.model

import android.graphics.Bitmap
import java.io.Serializable

class TextFieldAttribute(
    var name: String,
    var value: String? = null,
    var lcid: Int? = null,
    var pageIndex: Int? = null,
    var valid: Int? = null,
    var source: Int? = null,
    @Transient
    var image: Bitmap? = null,
    var equality: Boolean = true,
    var rfidStatus: Int? = null,
    var checkResult: Int? = null,
) : Serializable {
    override fun equals(other: Any?) = (other is TextFieldAttribute) && name == other.name
    override fun hashCode() = name.hashCode()
}
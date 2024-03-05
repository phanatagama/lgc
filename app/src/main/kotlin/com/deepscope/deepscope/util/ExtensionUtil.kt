package com.deepscope.deepscope.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.deepscope.deepscope.data.repository.local.entity.CustomerInformationEntity
import com.deepscope.deepscope.domain.model.CustomerInformation
import com.deepscope.deepscope.domain.model.TextFieldAttribute
import com.deepscope.deepscope.util.Utils.resizeBitmap
import com.regula.documentreader.api.R
import com.regula.documentreader.api.enums.eCheckResult
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRPRM_Lights
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.api.results.DocumentReaderValidity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import timber.log.Timber
import java.io.File


val String.Companion.Empty: String
    inline get() = ""


fun List<CustomerInformationEntity>.mapToModel(): List<CustomerInformation> {
    return map {
        it.mapToModel()
    }
}

fun Job.launchIn(scope: CoroutineScope, onComplete: () -> Unit) {
    scope.launch {
        join()
        withContext(Dispatchers.Main) {
            onComplete.invoke()
        }
    }
}

fun CoroutineScope.debounce(
    waitMs: Long = 300L,
    destinationFunction: () -> Unit
): () -> Unit {
    var debounceJob: Job? = null
    return {
        debounceJob?.cancel()
        debounceJob = launch {
            delay(waitMs)
            destinationFunction()
        }
    }
}

fun File.mimeType(): String? =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(this.extension)

fun ContentResolver.readAsRequestBody(uri: Uri): RequestBody =
    object : RequestBody() {
        override fun contentType(): MediaType? =
            this@readAsRequestBody.getType(uri)?.toMediaTypeOrNull()

        override fun writeTo(sink: BufferedSink) {
            this@readAsRequestBody.openInputStream(uri)?.source()?.use(sink::writeAll)
        }

        override fun contentLength(): Long =
            this@readAsRequestBody.query(uri, null, null, null, null)?.use { cursor ->
                val sizeColumnIndex: Int = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                cursor.getLong(sizeColumnIndex)
            } ?: super.contentLength()
    }

fun getValidity(
    list: List<DocumentReaderValidity>,
    type: Int?
): Int {
    for (validity in list) {
        if (validity.sourceType == type)
            return validity.status
    }

    return eCheckResult.CH_CHECK_WAS_NOT_DONE
}

fun DocumentReaderResults.toParcelable(
    context: Context,
    faceCaptureImage: Bitmap? = null
): Parcelable {
    val attributes = mutableListOf<TextFieldAttribute>()
    this.textResult?.fields?.forEach {
        val name = it.getFieldName(context)
        Timber.d( "[DEBUGX] fieldname ${it.getFieldName(context)} ")
        for (value in it.values) {
            Timber.d( "[DEBUGX] fieldtype ${value.field.fieldType} ")
            Timber.d( "[DEBUGX] source ${value.sourceType} ")
            Timber.d( "[DEBUGX] value ${value.value} ")
            val valid = getValidity(value.field.validityList, value.sourceType)
            val item = TextFieldAttribute(
                name!!,
                value.value,
                it.lcid,
                value.pageIndex,
                valid,
                value.sourceType
            )
            attributes.add(item)
        }
    }

    val statusDrawable = Utils.getDrawable(
        if (this.status.overallStatus == eCheckResult.CH_CHECK_OK) R.drawable.reg_ok else R.drawable.reg_fail,
        context
    )
    val name = this.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES)
    val gender = this.getTextFieldValueByType(eVisualFieldType.FT_SEX)
    val age = this.getTextFieldValueByType(eVisualFieldType.FT_AGE)
    val ageFieldName =
        this.getTextFieldByType(eVisualFieldType.FT_AGE)?.getFieldName(context)
    val userPhoto = this.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
        ?: this.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)

    val birth = this.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_BIRTH)
    val address = this.getTextFieldValueByType(eVisualFieldType.FT_ISSUING_STATE_NAME)
    val expiry = this.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_EXPIRY)
    val rawImage = this.getGraphicFieldImageByType(
        eGraphicFieldType.GF_PORTRAIT,
        eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
        0,
        eRPRM_Lights.RPRM_LIGHT_WHITE_FULL
    )
        ?: this.getGraphicFieldImageByType(
            eGraphicFieldType.GF_DOCUMENT_IMAGE,
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
        )
    val uvImage = this.getGraphicFieldByType(
        eGraphicFieldType.GF_DOCUMENT_IMAGE,
        eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE, 0, eRPRM_Lights.RPRM_LIGHT_UV
    )?.bitmap
    val documentName = if (this.documentType.isNotEmpty()) {
        Timber.d( "debugx document name ${this.documentType.first().name}")
        Timber.d(

            "debugx document documentid ${this.documentType.first().documentID}"
        )
        Timber.d( "debugx document dtypr ${this.documentType.first().dType}")
        Timber.d(
            "debugx document countty ${this.documentType.first().dCountryName}"
        )
        this.documentType.first().name
    } else {
        "-"
    }
    return DocumentReaderResultsParcel(
        userName = name,
        userDescription = if (ageFieldName != null) "$gender, ${ageFieldName}: $age" else String.Empty,
        userAddress = address,
        userDateOfBirth = birth,
        userDateOfIssue = expiry,
        documentName = documentName,
        rawImage = rawImage.resizeBitmap(),
        photoImage = userPhoto.resizeBitmap(),
        uvImage = uvImage.resizeBitmap(),
        faceCaptureImage = faceCaptureImage.resizeBitmap(),
        textField = attributes
    )
}


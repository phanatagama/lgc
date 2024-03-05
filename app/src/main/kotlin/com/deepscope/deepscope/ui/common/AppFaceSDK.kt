package com.deepscope.deepscope.ui.common

import android.graphics.Bitmap
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.callback.FaceCaptureCallback
import com.regula.facesdk.configuration.FaceCaptureConfiguration
import com.regula.facesdk.model.results.FaceCaptureResponse

interface IRegulaFaceSDK {
    val faceSDKInstance: FaceSDK
    val faceCaptureCallback: FaceCaptureCallback
    val faceCaptureConfiguration: FaceCaptureConfiguration
    fun presentFaceCaptureActivity()
    fun onFaceCaptured(response: FaceCaptureResponse)
    fun onFaceCaptured(bitmap: Bitmap)
}

abstract class AppFaceSDK : AppDocReaderSDK(), IRegulaFaceSDK {
    override val faceSDKInstance = FaceSDK.Instance()
    override val faceCaptureConfiguration: FaceCaptureConfiguration
        get() = FaceCaptureConfiguration.Builder()
            .registerUiFragmentClass(FaceCameraFragment::class.java)
            .setCloseButtonEnabled(true)
            .setCameraSwitchEnabled(false)
            .build()

    override val faceCaptureCallback: FaceCaptureCallback
        get() = FaceCaptureCallback {
            onFaceCaptured(it)
        }

    override fun onFaceCaptured(response: FaceCaptureResponse) {
        // ... check response.image for capture result
        if (response.image?.bitmap == null) {
            response.exception?.message?.let {
                // show error message
                showToast(it)
            }
        }
        // ... do something with response.image
        // showToast(getString(R.string.face_captured_successfully))
        onFaceCaptured(response.image?.bitmap!!)
    }

    override fun presentFaceCaptureActivity() {
        faceSDKInstance.presentFaceCaptureActivity(
            requireActivity(),
            faceCaptureConfiguration,
            faceCaptureCallback
        )
    }
}
package com.deepid.deepscope.ui.common

import android.view.View
import android.widget.TextView
import com.deepid.deepscope.R
import com.regula.facesdk.fragment.FaceUiFragment

class FaceCameraFragment : FaceUiFragment() {
    override fun getCloseButton(view: View): View {
        return view.findViewById(R.id.exitButton)
    }

    override fun getSwapCameraButton(view: View): View {
        return view.findViewById(R.id.swapCameraButton)
    }

    override fun getFlashLightButton(view: View): View {
        return view.findViewById(R.id.lightButton)
    }

    override fun getNotificationView(view: View): TextView {
        return view.findViewById(R.id.notificationTextView)
    }

    override fun getResourceLayoutId(): Int {
        return R.layout.fragment_face_camera
    }

}
package com.deepscope.deepscope.ui.customerInformation

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.deepscope.deepscope.databinding.FragmentPhotoDialogBinding


class PhotoDialogFragment : DialogFragment() {
    private var _binding: FragmentPhotoDialogBinding? = null
    private val binding: FragmentPhotoDialogBinding
        get() = _binding!!

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            dialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoDialogBinding.inflate(inflater, container, false)
        binding.closeIcon.setOnClickListener { dismiss() }
        binding.zoomImageView.setImageBitmap(arguments?.getParcelable(IMAGE_BITMAP))
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        internal val TAG = PhotoDialogFragment::class.java.simpleName
        const val IMAGE_BITMAP = "IMAGE_BITMAP"

        fun newInstance(bitmap: Bitmap): PhotoDialogFragment {
            val fragment = PhotoDialogFragment()
            val args = Bundle()
            args.putParcelable(IMAGE_BITMAP, bitmap)
            fragment.arguments = args
            return fragment
        }
    }
}
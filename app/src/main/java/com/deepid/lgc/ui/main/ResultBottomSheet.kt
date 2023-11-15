package com.deepid.lgc.ui.main

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.FragmentManager
import com.deepid.lgc.databinding.LayoutResultBottomsheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRPRM_Lights
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.results.DocumentReaderResults

class ResultBottomSheet : BottomSheetDialogFragment() {
    private var _binding: LayoutResultBottomsheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutResultBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            val ft = manager?.beginTransaction()
            ft?.add(this, tag)
            ft?.commitAllowingStateLoss()
        } catch (ignored: IllegalStateException) {

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val name = results.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES)
        val gender = results.getTextFieldValueByType(eVisualFieldType.FT_SEX)
        val age = results.getTextFieldValueByType(eVisualFieldType.FT_AGE)
        val birth = results.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_BIRTH)
        val address = results.getTextFieldValueByType(eVisualFieldType.FT_ADDRESS)
        val expiry = results.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_EXPIRY)
        val image = results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
            ?: results.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
        val rawImage = results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT,  eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE, 0, eRPRM_Lights.RPRM_LIGHT_WHITE_FULL)
            ?: results.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE, eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,)
        val documentName = if(results.documentType.isNotEmpty()) {
            Log.d(TAG, "debugx document name ${results.documentType.first().name}")
            Log.d(TAG, "debugx document documentid ${results.documentType.first().documentID}")
            Log.d(TAG, "debugx document dtypr ${results.documentType.first().dType}")
            Log.d(TAG, "debugx document countty ${results.documentType.first().dCountryName}")
            results.documentType.first().name
        }else{
            "-"
        }
        with(binding) {
            titleTv.text = name
            detailTv.text = "$gender, AGE: $age"
            birthDateTv.text = birth
            addressTv.text = address
            issueTv.text = expiry
            documentTv.text = documentName
            faceIv.setImageBitmap(image)
            rawImageIv.setImageBitmap(rawImage)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {

            val bottomSheetDialog = it as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { it ->
                val behaviour = BottomSheetBehavior.from(it)
                setupFullHeight(it)
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        lateinit var results: DocumentReaderResults
        const val TAG = "ModalBottomSheet"
        fun newInstance(): BottomSheetDialogFragment {
            return ResultBottomSheet()
        }
    }
}
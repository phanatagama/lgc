package com.deepid.deepscope.ui.main

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepid.deepscope.R
import com.deepid.deepscope.databinding.LayoutResultBottomsheetBinding
import com.deepid.deepscope.ui.defaultscanner.DocumentFieldAdapter
import com.deepid.deepscope.ui.main.fragment.GraphicfieldFragment
import com.deepid.deepscope.ui.scanner.ScannerViewModel
import com.deepid.deepscope.util.DocumentReaderResultsParcel
import com.deepid.deepscope.util.Utils.getDrawable
import com.deepid.deepscope.util.Utils.resizeBitmap
import com.deepid.deepscope.util.Utils.saveBitmap
import com.deepid.deepscope.util.toParcelable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.regula.documentreader.api.enums.eCheckResult
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRPRM_Lights
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.enums.ImageType
import com.regula.facesdk.model.MatchFacesImage
import com.regula.facesdk.model.results.matchfaces.MatchFacesResponse
import com.regula.facesdk.model.results.matchfaces.MatchFacesSimilarityThresholdSplit
import com.regula.facesdk.request.MatchFacesRequest
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ResultBottomSheet : DialogFragment() {
    private var _binding: LayoutResultBottomsheetBinding? = null
    private val binding get() = _binding!!
    private val scannerViewModel: ScannerViewModel by activityViewModel()
    private val rvAdapter: DocumentFieldAdapter by lazy {
        DocumentFieldAdapter()
    }
    private var documentImage: Bitmap? = null

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
            val ft = manager.beginTransaction()
            val prev: Fragment? = manager.findFragmentByTag(TAG)
            if (prev != null) {
                Log.d(TAG, "showFragment: remove prev...." + prev.javaClass.name)
                ft.remove(prev)
            }
            manager.executePendingTransactions()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        } catch (ignored: IllegalStateException) {

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe()
    }

    private fun observe() {
        scannerViewModel.documentReaderResultLiveData.observe(viewLifecycleOwner) {
            initViews(it)
            Log.d(TAG, "[DEBUGX] observe: ${it == null}")
            Log.d(TAG, "[DEBUGX] IT RAW RES: ${it?.rawResult}")
        }
        scannerViewModel.faceCaptureResponse.observe(viewLifecycleOwner) {
            if (documentImage != null && it?.image?.bitmap != null) {
                matchFaces(documentImage!!, it.image!!.bitmap)
            }
            Log.d(TAG, "[DEBUGX] observe faceCaptureResponse: ${it == null}")
        }
    }

    private fun matchFaces(first: Bitmap, second: Bitmap) {
        val firstImage = MatchFacesImage(first, ImageType.DOCUMENT_WITH_LIVE, true)
        val secondImage = MatchFacesImage(second, ImageType.LIVE, true)
        val matchFacesRequest = MatchFacesRequest(arrayListOf(firstImage, secondImage))
        FaceSDK.Instance().matchFaces(matchFacesRequest) { matchFacesResponse: MatchFacesResponse ->
            val split = MatchFacesSimilarityThresholdSplit(matchFacesResponse.results, 0.75)
            with(binding) {
                try {
                    if (split.matchedFaces.size > 0) {
                        val similarity = split.matchedFaces[0].similarity
                        similarityTv.text =
                            "Similarity: " + String.format("%.2f", similarity * 100) + "%"
                        if (similarity > 0.8) {
                            statusTv.text = "(Valid)"
                            statusTv.setTextColor(
                                ContextCompat.getColor(
                                    requireActivity(),
                                    com.regula.common.R.color.dark_green
                                )
                            )
                        } else {
                            statusTv.text = "(Not Valid)"
                            statusTv.setTextColor(
                                ContextCompat.getColor(
                                    requireActivity(),
                                    com.regula.common.R.color.red
                                )
                            )
                        }
                    } else {
                        similarityTv.text = "Similarity: 0%"
                        statusTv.text = "(Not Valid)"
                        statusTv.setTextColor(
                            ContextCompat.getColor(
                                requireActivity(),
                                com.regula.common.R.color.red
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(GraphicfieldFragment.TAG, "[DEBUGX] matchFaces: $e")
                }
//                btnScan.isEnabled = true
            }
        }
    }

    private fun initViews(results: DocumentReaderResults?) {
        Log.d(TAG, "raw result ${results?.rawResult}")
        val statusDrawable = getDrawable(
            if (results?.status?.overallStatus == eCheckResult.CH_CHECK_OK) com.regula.documentreader.api.R.drawable.reg_ok else com.regula.documentreader.api.R.drawable.reg_fail,
            requireActivity()
        )
        val name = results?.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES)
        Log.d(TAG, "[DEBUGX] name from bottom sheet $name ")
        val gender = results?.getTextFieldValueByType(eVisualFieldType.FT_SEX)
        val age = results?.getTextFieldValueByType(eVisualFieldType.FT_AGE)
        val ageFieldName =
            results?.getTextFieldByType(eVisualFieldType.FT_AGE)?.getFieldName(requireActivity())
        val image = results?.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
            ?: results?.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)

        documentImage = image
        val birth = results?.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_BIRTH)
        val address = results?.getTextFieldValueByType(eVisualFieldType.FT_ISSUING_STATE_NAME)
        val expiry = results?.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_EXPIRY)
        val rawImage = results?.getGraphicFieldImageByType(
            eGraphicFieldType.GF_PORTRAIT,
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
            0,
            eRPRM_Lights.RPRM_LIGHT_WHITE_FULL
        )
            ?: results?.getGraphicFieldImageByType(
                eGraphicFieldType.GF_DOCUMENT_IMAGE,
                eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
            )
        val documentName = if (results?.documentType?.isNotEmpty() == true) {
            Log.d(TAG, "debugx document name ${results.documentType.first().name}")
            Log.d(TAG, "debugx document documentid ${results.documentType.first().documentID}")
            Log.d(TAG, "debugx document dtypr ${results.documentType.first().dType}")
            Log.d(TAG, "debugx document countty ${results.documentType.first().dCountryName}")
            results.documentType.first().name
        } else {
            "-"
        }
        val parcelableTextField =
            results?.toParcelable(requireActivity()) as DocumentReaderResultsParcel?
        showUvImage(results)
        image?.saveBitmap(requireActivity())
        rawImage?.saveBitmap(requireActivity())


        with(binding) {
            titleTv.text = name
            detailTv.text = if (ageFieldName != null) "$gender, ${ageFieldName}: $age" else ""
            birthDateTv.text = birth
            addressTv.text = address
            issueTv.text = expiry
            documentTv.text = documentName
            rawImageIv.setImageBitmap(rawImage)
            faceIv.setImageBitmap(image)
            checkResultIv.setImageDrawable(statusDrawable)

            // add recyclerview
            recyclerView.layoutManager = LinearLayoutManager(requireActivity())
            recyclerView.adapter = rvAdapter

            recyclerView.addItemDecoration(
                DividerItemDecoration(
                    requireActivity(),
                    DividerItemDecoration.VERTICAL
                )
            )
            if (parcelableTextField?.textField?.isNotEmpty() == true) {
                rvAdapter.submitList(parcelableTextField.textField)
                hideRecyclerView(false)
            }
            ViewCompat.setNestedScrollingEnabled(recyclerView, false)
        }
    }

    private fun showUvImage(documentReaderResults: DocumentReaderResults?) {
        val uvDocumentReaderGraphicField = documentReaderResults?.getGraphicFieldByType(
            eGraphicFieldType.GF_DOCUMENT_IMAGE,
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE, 0, eRPRM_Lights.RPRM_LIGHT_UV
        )

        Log.d(TAG, "UV Graphic Field: $uvDocumentReaderGraphicField")

        if (uvDocumentReaderGraphicField != null && uvDocumentReaderGraphicField.bitmap != null) {
            val resizedBitmap = resizeBitmap(uvDocumentReaderGraphicField.bitmap)
            Log.d(TAG, "Resized UV Bitmap: $resizedBitmap")
            binding.uvImageIv.visibility = View.VISIBLE
            binding.uvImageIv.setImageBitmap(resizedBitmap)
            resizedBitmap!!.saveBitmap(requireActivity())
        } else {
            Log.d(TAG, "UV Graphic Field or Bitmap is null")
        }
    }

    private fun hideRecyclerView(isHide: Boolean) {
        binding.recyclerView.visibility = if (isHide) View.GONE else View.VISIBLE
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
        @StringRes
        private val TAB_TITLES = intArrayOf(
            R.string.tabs_overall,
            R.string.tabs_text,
            R.string.tabs_image
        )
        const val TAG = "ModalBottomSheet"
        fun newInstance(): DialogFragment {
            return ResultBottomSheet()
        }
    }
}
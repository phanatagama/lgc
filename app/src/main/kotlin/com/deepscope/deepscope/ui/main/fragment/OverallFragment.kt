package com.deepscope.deepscope.ui.main.fragment

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.deepscope.deepscope.databinding.FragmentOverallBinding
import com.deepscope.deepscope.ui.scanner.ScannerViewModel
import com.deepscope.deepscope.util.DocumentReaderResultsParcel
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRPRM_Lights
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.results.DocumentReaderResults
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber


class OverallFragment : Fragment() {
    private var documentReaderResults: DocumentReaderResultsParcel? = null
    private var _binding: FragmentOverallBinding? = null
    private val binding get() = _binding!!
    private val scannerViewModel: ScannerViewModel by activityViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            documentReaderResults = it.getParcelable(DOCUMENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentOverallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe()

    }

    private fun observe() {
        scannerViewModel.documentReaderResultLiveData.observe(viewLifecycleOwner) {
            initViews(it)
        }
    }

    private fun initViews(results: DocumentReaderResults?) {
        val birthFieldName =
            results?.getTextFieldByType(eVisualFieldType.FT_DATE_OF_BIRTH)
                ?.getFieldName(requireActivity())
        val birth =
            results?.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_BIRTH)
        val addressFieldName =
            results?.getTextFieldByType(eVisualFieldType.FT_ISSUING_STATE_NAME)
                ?.getFieldName(requireActivity())
        val address =
            results?.getTextFieldValueByType(eVisualFieldType.FT_ISSUING_STATE_NAME)
        val expiredFieldName =
            results?.getTextFieldByType(eVisualFieldType.FT_DATE_OF_EXPIRY)
                ?.getFieldName(requireActivity())
        val expired =
            results?.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_EXPIRY)
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
            Timber.d(
                "document name ${results.documentType.first()?.name}"
            )
            Timber.d(
                "document documentid ${results.documentType.first()?.documentID}"
            )
            Timber.d(
                "document dtypr ${results.documentType.first()?.dType}"
            )
            Timber.d(
                "document countty ${results.documentType.first()?.dCountryName}"
            )
            results.documentType.first()?.name
        } else {
            "-"
        }
        with(binding) {
            birthDateTv.text = birth ?: "-"
            birthDateTitleTv.text = birthFieldName ?: "Birth Date"
            addressTv.text = address ?: "-"
            addressTitleTv.text = addressFieldName ?: "Address"
            issueTv.text = expired ?: "-"
            issueTitleTv.text = expiredFieldName ?: "Issue Date"
            documentTv.text = documentName
            rawImageIv.setImageBitmap(rawImage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val DOCUMENT = "DOCUMENT_READER_RESULT"

        @JvmStatic
        fun newInstance(documentReaderResults: Parcelable?) =
            OverallFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(DOCUMENT, documentReaderResults)
                }
            }
    }
}
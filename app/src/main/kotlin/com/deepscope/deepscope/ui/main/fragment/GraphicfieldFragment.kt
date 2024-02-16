package com.deepscope.deepscope.ui.main.fragment

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.deepscope.deepscope.databinding.FragmentGraphicfieldBinding
import com.deepscope.deepscope.ui.scanner.ScannerViewModel
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.enums.ImageType
import com.regula.facesdk.model.MatchFacesImage
import com.regula.facesdk.model.results.FaceCaptureResponse
import com.regula.facesdk.model.results.matchfaces.MatchFacesResponse
import com.regula.facesdk.model.results.matchfaces.MatchFacesSimilarityThresholdSplit
import com.regula.facesdk.request.MatchFacesRequest
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class GraphicfieldFragment : Fragment() {
    private var _binding: FragmentGraphicfieldBinding? = null
    private val binding get() = _binding!!
    private val scannerViewModel: ScannerViewModel by activityViewModel()
    private var documentImage: Bitmap? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentGraphicfieldBinding.inflate(inflater, container, false)
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
        scannerViewModel.faceCaptureResponse.observe(viewLifecycleOwner) {
            initLiveImage(it)
        }
    }

    private fun initViews(results: DocumentReaderResults?) {
        documentImage =
            results?.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
                ?: results?.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
        displayResults(documentImage)
    }

    private fun initLiveImage(faceCaptureResponse: FaceCaptureResponse?) {
        faceCaptureResponse?.image?.bitmap?.let { bitmap ->
            val liveImage: Bitmap = bitmap
            documentImage?.let {
                matchFaces(it, liveImage)
                displayResults(documentImage, liveImage)
            }
        } ?: run {
            faceCaptureResponse?.exception?.message?.let {
                Toast.makeText(
                    requireActivity(),
                    "Error: $it",
                    Toast.LENGTH_LONG
                ).show()
            }
            displayResults(documentImage)
        }
    }

    private fun displayResults(documentImage: Bitmap?, liveImage: Bitmap? = null) {
        with(binding) {
            documentIv.setImageBitmap(documentImage)
            liveIv.setImageBitmap(liveImage)
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
                } catch (e: Exception){
                    Log.e(TAG, "[DEBUGX] matchFaces: $e", )
                }
//                btnScan.isEnabled = true
            }
        }
    }

    companion object {
        const val GRAPHIC = "GRAPHIC_FIELD"
        const val TAG = "GRAPHIC_FIELD_FRAGMENT"
        @JvmStatic
        fun newInstance() =
            GraphicfieldFragment()
    }
}
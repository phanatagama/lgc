package com.deepscope.deepscope.ui.result

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepscope.deepscope.R
import com.deepscope.deepscope.databinding.ActivityScanResultBinding
import com.deepscope.deepscope.ui.defaultscanner.DocumentFieldAdapter
import com.deepscope.deepscope.util.DocumentReaderResultsParcel
import com.deepscope.deepscope.util.Utils.saveBitmap
import com.deepscope.deepscope.util.toParcelable
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRPRM_Lights
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.enums.ImageType
import com.regula.facesdk.model.MatchFacesImage
import com.regula.facesdk.model.results.FaceCaptureResponse
import com.regula.facesdk.model.results.matchfaces.MatchFacesResponse
import com.regula.facesdk.model.results.matchfaces.MatchFacesSimilarityThresholdSplit
import com.regula.facesdk.request.MatchFacesRequest
import timber.log.Timber

class ScanResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanResultBinding
    private val rvAdapter: DocumentFieldAdapter by lazy {
        DocumentFieldAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Timber.d("[DEBUGX] onCreate: $documentResults")
        initViews()
    }

    private fun initViews() {
        val name = documentResults?.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES)
        val userPhoto = documentResults?.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
            ?: documentResults?.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)


        val birth = documentResults?.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_BIRTH)
        val address = documentResults?.getTextFieldValueByType(eVisualFieldType.FT_ISSUING_STATE_NAME)
        val expiry = documentResults?.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_EXPIRY)
        val rawImage = documentResults?.getGraphicFieldImageByType(
            eGraphicFieldType.GF_PORTRAIT,
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
            0,
            eRPRM_Lights.RPRM_LIGHT_WHITE_FULL
        )
            ?: documentResults?.getGraphicFieldImageByType(
                eGraphicFieldType.GF_DOCUMENT_IMAGE,
                eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
            )
        val documentName = if (documentResults?.documentType?.isNotEmpty() == true) {
            documentResults?.documentType?.first()?.name
        } else {
            "-"
        }
        val parcelableTextField =
            documentResults?.toParcelable(this) as DocumentReaderResultsParcel?
        userPhoto?.saveBitmap(this)
        rawImage?.saveBitmap(this)

        val uvImage = documentResults?.getGraphicFieldByType(
            eGraphicFieldType.GF_DOCUMENT_IMAGE,
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE, 0, eRPRM_Lights.RPRM_LIGHT_UV
        )?.bitmap

        if (userPhoto != null && faceCaptureResponse?.image?.bitmap != null) {
            matchFaces(userPhoto, faceCaptureResponse?.image?.bitmap!!)
        }
        with(binding) {
            titleTv.text = name
            detailTv.text = parcelableTextField?.userDescription
            birthDateTv.text = birth
            addressTv.text = address
            issueTv.text = expiry
            documentTv.text = documentName
            rawImageIv.setImageBitmap(rawImage)
            faceIv.setImageBitmap(userPhoto)

            if (uvImage != null) {
                uvImageIv.visibility = View.VISIBLE
                uvImageIv.setImageBitmap(uvImage)
            }

            // add recyclerview
            recyclerView.layoutManager = LinearLayoutManager(this@ScanResultActivity)
            recyclerView.adapter = rvAdapter

            recyclerView.addItemDecoration(
                DividerItemDecoration(
                    this@ScanResultActivity,
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

    private fun hideRecyclerView(isHide: Boolean) {
        binding.recyclerView.visibility = if (isHide) View.GONE else View.VISIBLE
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
                        similarityTv.text = getString(R.string.similarity, similarity * 100)
//                            "Similarity: " + String.format("%.2f", similarity * 100) + "%"
                        if (similarity > 0.8) {
                            statusTv.text = getString(R.string.valid)
                            statusTv.setTextColor(
                                ContextCompat.getColor(
                                    this@ScanResultActivity,
                                    com.regula.common.R.color.dark_green
                                )
                            )
                        } else {
                            statusTv.text = getString(R.string.not_valid)
                            statusTv.setTextColor(
                                ContextCompat.getColor(
                                    this@ScanResultActivity,
                                    com.regula.common.R.color.red
                                )
                            )
                        }
                    } else {
                        similarityTv.text = getString(R.string.similarity_0)
                        statusTv.text = getString(R.string.not_valid)
                        statusTv.setTextColor(
                            ContextCompat.getColor(
                                this@ScanResultActivity,
                                com.regula.common.R.color.red
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.e( "[DEBUGX] matchFaces: $e")
                }
//                btnScan.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        documentResults = null
        faceCaptureResponse = null
    }
    companion object {
        var documentResults: DocumentReaderResults? = null
        var faceCaptureResponse: FaceCaptureResponse? = null
        const val TAG = "ScanResultActivity"
    }
}
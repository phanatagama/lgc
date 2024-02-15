package com.deepid.deepscope.ui.defaultscanner

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepid.deepscope.domain.model.TextFieldAttribute
import com.deepid.deepscope.databinding.ActivityDefaultScannerBinding
import com.deepid.deepscope.ui.common.FaceCameraFragment
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.CaptureMode
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.enums.eCheckResult
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.errors.DocReaderRfidException
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.results.DocumentReaderNotification
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.api.results.DocumentReaderValidity
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.configuration.FaceCaptureConfiguration
import com.regula.facesdk.enums.ImageType
import com.regula.facesdk.enums.LivenessStatus
import com.regula.facesdk.model.MatchFacesImage
import com.regula.facesdk.model.results.FaceCaptureResponse
import com.regula.facesdk.model.results.LivenessResponse
import com.regula.facesdk.model.results.matchfaces.MatchFacesResponse
import com.regula.facesdk.model.results.matchfaces.MatchFacesSimilarityThresholdSplit
import com.regula.facesdk.request.MatchFacesRequest

class DefaultScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDefaultScannerBinding
    private var currentScenario = Scenario.SCENARIO_OCR
    private val rvAdapter: DocumentFieldAdapter by lazy {
        DocumentFieldAdapter()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDefaultScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        setUpFunctionality()
    }
    private fun setUpFunctionality() {
        DocumentReader.Instance().processParams().timeout = Double.MAX_VALUE
        DocumentReader.Instance().processParams().timeoutFromFirstDetect = Double.MAX_VALUE
        DocumentReader.Instance().processParams().timeoutFromFirstDocType = Double.MAX_VALUE
        DocumentReader.Instance().functionality().edit()
            .setShowCaptureButton(true)
            .setShowCaptureButtonDelayFromStart(0)
            .setShowCaptureButtonDelayFromDetect(0)
            .setCaptureMode(CaptureMode.AUTO)
            .setDisplayMetadata(true)
            .apply()
    }

    private fun showScanner() {
        Log.d(TAG, "[DEBUGX] showScanner: currentscenario $currentScenario")
        val scannerConfig = ScannerConfig.Builder(currentScenario).build()
        DocumentReader.Instance()
            .showScanner(this@DefaultScannerActivity, scannerConfig, completion)
    }

    private fun displayResults(results: DocumentReaderResults) {
        val documentImage: Bitmap? =
            results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
                ?: results.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
        if (documentImage != null) {
            Log.d(TAG, "[DEBUGX] documentImage is not null")
            binding.documentIv.setImageBitmap(documentImage)
            return
        }
        results.graphicResult?.fields?.first()?.let {
            val name = it.getFieldName(this) + " [${it.pageIndex}]"
            binding.titleTv.text = name
            val image = it.bitmap
            binding.documentIv.setImageBitmap(image)
            Log.d(TAG, "[DEBUGX] initResults: name = ${name} ")
        }
    }

    private fun displayResults(documentImage: Bitmap, liveImage: Bitmap) {
        with(binding) {
            documentIv.setImageBitmap(documentImage)
            liveIv.setImageBitmap(liveImage)
        }
    }
    private fun updateRecyclerViews(results: DocumentReaderResults){
        val attributes = mutableListOf<TextFieldAttribute>()
        results.textResult?.fields?.forEach {
            val name = it.getFieldName(this)
            Log.d(TAG, "[DEBUGX] fieldname ${it.getFieldName(this)} ")
            for (value in it.values) {
                Log.d(TAG, "[DEBUGX] fieldtype ${value.field.fieldType} ")
                Log.d(TAG, "[DEBUGX] source ${value.sourceType} ")
                Log.d(TAG, "[DEBUGX] value ${value.value} ")
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
        Log.d(TAG, "[DEBUGX] updateRecyclerViews")
        if(attributes.isNotEmpty()){
            Log.d(TAG, "[DEBUGX] attribut is not empty")
            rvAdapter.submitList(attributes)
            hideRecyclerView(false)
        }
    }
    private fun hideRecyclerView(isHide: Boolean){
        binding.recyclerView.visibility = if(isHide) View.GONE else View.VISIBLE
    }

    private fun getValidity(
        list: List<DocumentReaderValidity>,
        type: Int?
    ): Int {
        for (validity in list) {
            if (validity.sourceType == type)
                return validity.status
        }

        return eCheckResult.CH_CHECK_WAS_NOT_DONE
    }

    @Transient
    private val completion = IDocumentReaderCompletion { action, results, error ->
        if (action == DocReaderAction.COMPLETE
            || action == DocReaderAction.TIMEOUT
        ) {
            if (DocumentReader.Instance().functionality().isManualMultipageMode) {
                Log.d(TAG, "[DEBUGX] MULTIPAGEMODE: ")
                if (results?.morePagesAvailable != 0) {
                    DocumentReader.Instance().startNewPage()
                    Handler(Looper.getMainLooper()).postDelayed({
                        showScanner()
                    }, 100)
                    return@IDocumentReaderCompletion
                } else {
                    DocumentReader.Instance().functionality().edit().setManualMultipageMode(false)
                        .apply()
                }
            }
            if (results?.chipPage != 0) {
                Log.d(TAG, "[DEBUGX] RFID IS PERFORMED: ")
                DocumentReader.Instance().startRFIDReader(this, object : IRfidReaderCompletion() {
                    override fun onChipDetected() {
                        Log.d("Rfid", "Chip detected")
                    }

                    override fun onProgress(notification: DocumentReaderNotification) {
//                        rfidProgress(notification.code, notification.value)
                    }

                    override fun onRetryReadChip(exception: DocReaderRfidException) {
                        Log.d("Rfid", "Retry with error: " + exception.errorCode)
                    }

                    override fun onCompleted(
                        rfidAction: Int,
                        results_RFIDReader: DocumentReaderResults?,
                        error: DocumentReaderException?
                    ) {
                        if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL)
                            captureFace(results_RFIDReader!!)
                    }
                })
            } else {
                Log.d(TAG, "[DEBUGX] NO RFID PERFORMED ")
                /**
                * perform [livenessFace] or [captureface] then check similarity
                */
              //  livenessFace(results)
                captureFace(results)
            }
        } else
            if (action == DocReaderAction.CANCEL) {
                if (DocumentReader.Instance().functionality().isManualMultipageMode)
                    DocumentReader.Instance().functionality().edit().setManualMultipageMode(false)
                        .apply()

                Toast.makeText(this, "Scanning was cancelled", Toast.LENGTH_LONG).show()
            } else if (action == DocReaderAction.ERROR) {
                Toast.makeText(this, "Error:$error", Toast.LENGTH_LONG).show()
            }
    }

    private fun livenessFace(results: DocumentReaderResults) {
        FaceSDK.Instance().startLiveness(this) { livenessResponse: LivenessResponse? ->
            livenessResponse?.liveness?.let {
                if (it == LivenessStatus.PASSED) {
                    Toast.makeText(
                        this,
                        "Liveness check is Passed",
                        Toast.LENGTH_LONG
                    ).show()
                    val documentImage: Bitmap? =
                        results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
                            ?: results.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
                    val liveImage: Bitmap? = livenessResponse.bitmap
                    if (documentImage != null && liveImage != null) {
                        matchFaces(documentImage, liveImage)
                        displayResults(documentImage, liveImage)
                    } else {
                        displayResults(results)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Liveness check is Failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun captureFace(results: DocumentReaderResults) {
        val faceCaptureConfiguration: FaceCaptureConfiguration =
            FaceCaptureConfiguration.Builder()
                .registerUiFragmentClass(FaceCameraFragment::class.java)
                .setCloseButtonEnabled(true)
                .setCameraSwitchEnabled(false)
                .build()
        FaceSDK.Instance()
            .presentFaceCaptureActivity(
                this@DefaultScannerActivity,
                faceCaptureConfiguration
            ) { response: FaceCaptureResponse ->
                // ... check response.image for capture result
                response.image?.bitmap?.let { bitmap ->
                    val documentImage: Bitmap? =
                        results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
                            ?: results.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
                    val liveImage: Bitmap = bitmap
                    documentImage?.let {
                        matchFaces(documentImage, liveImage)
                        displayResults(documentImage, liveImage)
                    }
                } ?: run {
                    response.exception?.message?.let {
                        Toast.makeText(
                            this@DefaultScannerActivity,
                            "Error: $it",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    displayResults(results)
                }
                updateRecyclerViews(results)
            }
    }

    private fun matchFaces(first: Bitmap, second: Bitmap) {
        val firstImage = MatchFacesImage(first, ImageType.DOCUMENT_WITH_LIVE, true)
        val secondImage = MatchFacesImage(second, ImageType.LIVE, true)
        val matchFacesRequest = MatchFacesRequest(arrayListOf(firstImage, secondImage))
        FaceSDK.Instance().matchFaces(matchFacesRequest) { matchFacesResponse: MatchFacesResponse ->
            val split = MatchFacesSimilarityThresholdSplit(matchFacesResponse.results, 0.75)
            with(binding) {
                if (split.matchedFaces.size > 0) {
                    val similarity = split.matchedFaces[0].similarity
                    similarityTv.text =
                        "Similarity: " + String.format("%.2f", similarity * 100) + "%"
                    if (similarity > 0.8) {
                        statusTv.text = "(Valid)"
                        statusTv.setTextColor(
                            ContextCompat.getColor(
                                this@DefaultScannerActivity,
                                com.regula.common.R.color.dark_green
                            )
                        )

                    } else {
                        statusTv.text = "(Not Valid)"
                        statusTv.setTextColor(
                            ContextCompat.getColor(
                                this@DefaultScannerActivity,
                                com.regula.common.R.color.red
                            )
                        )
                    }
                } else {
                    similarityTv.text = "Similarity: 0%"
                    statusTv.text = "(Not Valid)"
                    statusTv.setTextColor(
                        ContextCompat.getColor(
                            this@DefaultScannerActivity,
                            com.regula.common.R.color.red
                        )
                    )
                }
//                btnScan.isEnabled = true
            }
        }
    }

    private fun resetViews() {
        with(binding) {
            similarityTv.text = "Similarity: -"
            statusTv.text = ""
            documentIv.setImageBitmap(null)
            liveIv.setImageBitmap(null)
        }
        hideRecyclerView(true)
    }

    private fun initViews() {
        with(binding) {
            recyclerView.layoutManager = LinearLayoutManager(this@DefaultScannerActivity)
            recyclerView.adapter = rvAdapter
            btnScan.setOnClickListener {
                resetViews()
                showScanner()
            }
        }
    }

    companion object{
        const val TAG = "DefaultScannerActivity"
    }
}
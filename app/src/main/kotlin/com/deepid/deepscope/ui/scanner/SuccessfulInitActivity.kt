package com.deepid.deepscope.ui.scanner

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.deepid.deepscope.R
import com.deepid.deepscope.ui.common.FaceCameraFragment
import com.deepid.deepscope.ui.main.ResultBottomSheet
import com.deepid.deepscope.util.Utils.resizeBitmap
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRPRM_Lights
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.configuration.FaceCaptureConfiguration
import com.regula.facesdk.model.results.FaceCaptureResponse
import org.koin.androidx.viewmodel.ext.android.viewModel

class SuccessfulInitActivity : AppCompatActivity() {
    private var uvImage: ImageView? = null
    private var rfidImage: ImageView? = null
    private var showScannerBtn: Button? = null
    private var currentScenario: String = Scenario.SCENARIO_FULL_AUTH

    // TODO: add view model and upload image when scan is successfull
    private val scannerViewModel: ScannerViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sucessfull_init_activity)
        initViews()

        if (!DocumentReader.Instance().isReady) {
            showScannerBtn!!.isEnabled = false
        }
        showScannerBtn!!.setOnClickListener {
            val scannerConfig = ScannerConfig.Builder(currentScenario).build()
            DocumentReader.Instance().showScanner(
                this, scannerConfig
            ) { action, results, error ->
                if (action == DocReaderAction.COMPLETE) {
                    if (results != null) {
                        Log.d(
                            TAG,
                            "[DEBUGX] DocReaderAction is Timeout: ${action == DocReaderAction.TIMEOUT} "
                        )
                        scannerViewModel.setDocumentReaderResults(results)
                    }
                    showUvImage(results)
                    //Checking, if nfc chip reading should be performed
                    if (results!!.chipPage != 0) {
                        //starting chip reading
                        DocumentReader.Instance().startRFIDReader(
                            this@SuccessfulInitActivity,
                            object : IRfidReaderCompletion() {
                                override fun onCompleted(
                                    rfidAction: Int,
                                    resultsRFIDReader: DocumentReaderResults?,
                                    error: DocumentReaderException?
                                ) {
                                    if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                                        scannerViewModel.setDocumentReaderResults(
                                            resultsRFIDReader ?: results
                                        )
                                        captureFace()
                                        showGraphicFieldImage(results)
                                    }

                                }

                            })
                    } else {
                        captureFace()
                    }
                    Log.d(
                        this@SuccessfulInitActivity.localClassName,
                        "completion raw result: " + results.rawResult
                    )
                } else {
                    //something happened before all results were ready
                    if (action == DocReaderAction.CANCEL) {
                        Toast.makeText(
                            this@SuccessfulInitActivity,
                            "Scanning was cancelled",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    } else if (action == DocReaderAction.ERROR) {
                        Toast.makeText(
                            this@SuccessfulInitActivity,
                            "Error:$error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun displayResults() {
        val dialog = ResultBottomSheet.newInstance()
        dialog.show(supportFragmentManager, ResultBottomSheet.TAG)
    }

    fun captureFace() {
        val faceCaptureConfiguration: FaceCaptureConfiguration =
            FaceCaptureConfiguration.Builder()
                .registerUiFragmentClass(FaceCameraFragment::class.java)
                .setCloseButtonEnabled(true)
                .setCameraSwitchEnabled(false)
                .build()
        FaceSDK.Instance()
            .presentFaceCaptureActivity(
                this@SuccessfulInitActivity,
                faceCaptureConfiguration
            ) { response: FaceCaptureResponse ->
                scannerViewModel.setFaceCaptureResponse(response)
                // ... check response.image for capture result
                if (response.image?.bitmap == null) {
                    response.exception?.message?.let {
                        Toast.makeText(
                            this@SuccessfulInitActivity,
                            "Error: $it",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                displayResults()
            }
    }

    override fun onPause() {
        super.onPause()
        resetViews()
    }

    private fun resetViews() {
        uvImage?.invalidate()
        rfidImage?.invalidate()
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
            uvImage?.setImageBitmap(resizedBitmap)
        } else {
            Log.d(TAG, "UV Graphic Field or Bitmap is null")
        }
    }

    private fun showGraphicFieldImage(results: DocumentReaderResults?) {
        val documentImage: Bitmap? =
            if (results?.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT) == null) {
                results?.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
            } else {
                results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
            }
        if (documentImage != null)
            rfidImage?.setImageBitmap(resizeBitmap(documentImage))
    }


    private fun initViews() {
        showScannerBtn = findViewById(R.id.showScannerBtn)
        uvImage = findViewById(R.id.uvImageView)
        rfidImage = findViewById(R.id.documentImageIv)
    }

    companion object {
        const val TAG = "SuccessfullInitActivity"
    }
}
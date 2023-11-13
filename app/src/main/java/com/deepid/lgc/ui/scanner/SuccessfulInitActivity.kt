package com.deepid.lgc.ui.scanner

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.deepid.lgc.R
import com.deepid.lgc.data.model.FileUploadRequest
import com.deepid.lgc.databinding.SucessfullInitActivityBinding
import com.deepid.lgc.util.getImageFile
import com.deepid.lgc.util.mimeType
import com.deepid.lgc.util.readAsRequestBody
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
import com.regula.facesdk.enums.LivenessStatus
import com.regula.facesdk.model.results.LivenessResponse
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class SuccessfulInitActivity : AppCompatActivity() {
    private var uvImage: ImageView? = null
    private var rfidImage: ImageView? = null
    private var showScannerBtn: Button? = null
    private lateinit var binding: SucessfullInitActivityBinding

    // TODO: add view model and upload image when scan is successfull
    private val scannerViewModel: ScannerViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SucessfullInitActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        observe()

        if (!DocumentReader.Instance().isReady)
            showScannerBtn!!.isEnabled = false
        showScannerBtn!!.setOnClickListener {
            val scannerConfig = ScannerConfig.Builder(Scenario.SCENARIO_OCR).build()
            DocumentReader.Instance().showScanner(
                this, scannerConfig
            ) { action, results, error ->
                if (action == DocReaderAction.COMPLETE) {
                    FaceSDK.Instance().startLiveness(this) { livenessResponse: LivenessResponse? ->
                        livenessResponse?.liveness?.let {
                            if (it == LivenessStatus.PASSED) {
                                binding.uploadBtn.isEnabled = true
                                Toast.makeText(
                                    this@SuccessfulInitActivity,
                                    "Liveness check is Passed",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                binding.uploadBtn.isEnabled = false
                                Toast.makeText(
                                    this@SuccessfulInitActivity,
                                    "Liveness check is Failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
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
                                    results: DocumentReaderResults?,
                                    error: DocumentReaderException?
                                ) {
                                    if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                                        showGraphicFieldImage(results)
                                    }
                                }

                            })
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

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                scannerViewModel.state.collect { uiState ->
                    handleStateChange(uiState)
                }
            }
        }
    }

    private fun handleStateChange(uiState: ScannerUiState) {
        when (uiState) {
            is ScannerUiState.Init -> Unit
            is ScannerUiState.Loading -> Unit
            is ScannerUiState.Error -> Unit
            is ScannerUiState.Success -> Unit
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
        if (uvDocumentReaderGraphicField != null && uvDocumentReaderGraphicField.bitmap != null)
            uvImage?.setImageBitmap(resizeBitmap(uvDocumentReaderGraphicField.bitmap))
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

    private fun resizeBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap != null) {
            val aspectRatio = bitmap.width.toDouble() / bitmap.height.toDouble()
            return Bitmap.createScaledBitmap(bitmap, (480 * aspectRatio).toInt(), 480, false)
        }
        return null
    }

    private fun initViews() {
        showScannerBtn = findViewById(R.id.showScannerBtn)
        uvImage = findViewById(R.id.uvImageView)
        rfidImage = findViewById(R.id.documentImageIv)
        binding.uploadBtn.setOnClickListener {
            uvImage?.drawable?.toBitmap()?.let { bitmap ->
                val file: File = getImageFile(this, bitmap)
                val fileUploadRequest =
                    FileUploadRequest(
                        mimeType = file.mimeType().toString(),
                        fileLength = file.length()
                    )
                val fileRequestBody = contentResolver.readAsRequestBody(Uri.fromFile(file))
                scannerViewModel.uploadFile(fileUploadRequest, fileRequestBody)
            }
        }
    }
}

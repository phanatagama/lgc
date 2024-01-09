package com.deepid.lgc.ui.customerInformation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.deepid.lgc.databinding.ActivityCustomerInformationBinding
import com.deepid.lgc.domain.model.DataImage
import com.deepid.lgc.domain.model.generateImagePlaceholder
import com.deepid.lgc.ui.scanner.InputDeviceActivity
import com.deepid.lgc.util.Helpers
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.errors.DocReaderRfidException
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.results.DocumentReaderNotification
import com.regula.documentreader.api.results.DocumentReaderResults
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class CustomerInformationActivity : AppCompatActivity() {
    private val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private val currentDateTime : String= LocalDateTime.now().format(formatter)
//    private val customerInformationViewModel: CustomerInformationViewModel by viewModel()
    private val currentScenario: String = Scenario.SCENARIO_FULL_AUTH
    private lateinit var binding: ActivityCustomerInformationBinding
    private var selectedImage: DataImage = DataImage(1,null)
    private val rvAdapter: PhotoAdapter by lazy {
        PhotoAdapter()
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                Helpers.PERMISSIONS_REQUEST_CAMERA
            )
        } else {
            takePhotoLauncher.launch(null)
        }
    }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { result ->
            result?.let {
                rvAdapter.updateList(selectedImage.copy(bitmap = it))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observe()
        bindViews()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Request camera permissions
        if (allPermissionsGranted()) {
            if (documentResults == null) {
                takePhoto()
            }
        } else {
            requestPermissions()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                takePhoto()
            }
        }

    private fun bindViews() {
        with(binding) {
            issueTv.text = currentDateTime
            rvPhoto.layoutManager =
                GridLayoutManager(this@CustomerInformationActivity, 2)
            rvPhoto.adapter = rvAdapter
            rvAdapter.submitList(generateImagePlaceholder)
            rvAdapter.listener = object : PhotoAdapter.OnItemClickListener {
                override fun onItemClickListener(view: View, dataImage: DataImage) {
                    selectedImage = dataImage
                    if (dataImage.bitmap != null) {
                        Toast.makeText(
                            this@CustomerInformationActivity,
                            "Grid ${dataImage.id} was filled",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }
                    takePhoto()
                }
            }
            ViewCompat.setNestedScrollingEnabled(rvPhoto, false)
        }
        insertOpticalImage(documentResults)
    }

    private fun insertOpticalImage(documentReaderResults: DocumentReaderResults?) {
        val userPhoto = documentReaderResults?.getGraphicFieldImageByType(
            eGraphicFieldType.GF_PORTRAIT
        )
            ?: documentReaderResults?.getGraphicFieldImageByType(
                eGraphicFieldType.GF_DOCUMENT_IMAGE
            )
//        userPhoto?.let { customerInformationViewModel.addImage(it) }
    }
    @Transient
    private val completion = IDocumentReaderCompletion { action, results, error ->
        if (action == DocReaderAction.COMPLETE
            || action == DocReaderAction.TIMEOUT
        ) {
            if (results != null) {
                documentResults = results
                insertOpticalImage(results)
            }
            if (DocumentReader.Instance().functionality().isManualMultipageMode) {
                Log.d(InputDeviceActivity.TAG, "[DEBUGX] MULTIPAGEMODE: ")
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
                Log.d(InputDeviceActivity.TAG, "DEBUGX RFID IS PERFORMED: ")
                DocumentReader.Instance().startRFIDReader(this, rfidCompletion)
            } else {
                Log.d(InputDeviceActivity.TAG, "[DEBUGX] NO RFID PERFORMED ")
                /**
                 * perform @livenessFace or @captureFace then check similarity
                 */
            }
        } else {
            if (action == DocReaderAction.CANCEL) {
                if (DocumentReader.Instance().functionality().isManualMultipageMode)
                    DocumentReader.Instance().functionality().edit().setManualMultipageMode(false)
                        .apply()

                Toast.makeText(this, "Scanning was cancelled", Toast.LENGTH_LONG).show()
            } else if (action == DocReaderAction.ERROR) {
                Toast.makeText(this, "Error:$error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val rfidCompletion = object : IRfidReaderCompletion() {
        override fun onChipDetected() {
            Log.d("Rfid", "Chip detected")
        }

        override fun onProgress(notification: DocumentReaderNotification) {
//            rfidProgress(notification.code, notification.value)
        }

        override fun onRetryReadChip(exception: DocReaderRfidException) {
            Log.d("Rfid", "Retry with error: " + exception.errorCode)
        }

        override fun onCompleted(
            rfidAction: Int,
            resultsRFIDReader: DocumentReaderResults?,
            error: DocumentReaderException?
        ) {
            if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                // TODO: show result here
            }
        }
    }

    private fun showScanner() {
        val scannerConfig = ScannerConfig.Builder(currentScenario).build()
        DocumentReader.Instance()
            .showScanner(this@CustomerInformationActivity, scannerConfig, completion)
    }

    override fun onDestroy() {
        super.onDestroy()
        documentResults = null
    }

    private fun observe() {
        // TODO: observe view model LiveData/Flow here
    }

    companion object {
        var documentResults: DocumentReaderResults? = null
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}
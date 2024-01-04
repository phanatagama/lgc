package com.deepid.lgc.ui.input

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepid.lgc.R
import com.deepid.lgc.databinding.ActivityInputBinding
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
import org.koin.androidx.viewmodel.ext.android.viewModel


class InputActivity : AppCompatActivity() {
    private val inputViewModel: InputViewModel by viewModel()
    private var currentScenario: String = Scenario.SCENARIO_FULL_AUTH
    private lateinit var binding: ActivityInputBinding
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
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, RESULT_ADD_PHOTO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_ADD_PHOTO && resultCode == RESULT_OK) {
            val photo = data?.extras!!["data"] as Bitmap?
            if (photo != null) {
                inputViewModel.addImage(photo)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Helpers.PERMISSIONS_REQUEST_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onRequestPermissionsResult: permission is granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observe()
        bindViews()
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
            } else {
                takePhoto()
            }
        }

    private fun bindViews() {
        with(binding) {
            rvPhoto.layoutManager =
                LinearLayoutManager(this@InputActivity, LinearLayoutManager.HORIZONTAL, false)
            rvPhoto.adapter = rvAdapter
            btnAdd.setOnClickListener {
                if (documentResults == null) {
                    takePhoto()
                } else {
                    showScanner()
                }
            }
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
        userPhoto?.let { inputViewModel.addImage(it) }
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
                        if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                            // TODO: show result here
                        }
                    }
                })
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

    private fun showScanner() {
        val scannerConfig = ScannerConfig.Builder(currentScenario).build()
        DocumentReader.Instance()
            .showScanner(this@InputActivity, scannerConfig, completion)
    }

    override fun onDestroy() {
        super.onDestroy()
        documentResults = null
    }

    private fun observe() {
        inputViewModel.images.observe(this) { data ->
            if (data != null) {
                rvAdapter.submitList(data.mapIndexed { index, it -> DataImage(index, it) })
            }
        }
        inputViewModel.count.observe(this) { it ->
            with(binding) {
                btnAdd.isEnabled = it < 10
                btnAdd.text = getString(R.string.add_image, it)
            }
        }
    }

    companion object {
        const val TAG = "InputActivity"
        const val RESULT_ADD_PHOTO = 101
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
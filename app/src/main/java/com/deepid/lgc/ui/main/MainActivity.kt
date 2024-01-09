package com.deepid.lgc.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepid.lgc.R
import com.deepid.lgc.databinding.ActivityMainBinding
import com.deepid.lgc.ui.common.FaceCameraFragment
import com.deepid.lgc.ui.common.RecyclerAdapter
import com.deepid.lgc.ui.customerInformation.CustomerInformationActivity
import com.deepid.lgc.ui.customerInformation.search.SearchCustomerInformationActivity
import com.deepid.lgc.ui.result.ScanResultActivity
import com.deepid.lgc.ui.scanner.InputDeviceActivity
import com.deepid.lgc.ui.scanner.ScannerUiState
import com.deepid.lgc.ui.scanner.ScannerViewModel
import com.deepid.lgc.util.Base
import com.deepid.lgc.util.Helpers.Companion.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
import com.deepid.lgc.util.Utils
import com.deepid.lgc.util.Utils.getRealPathFromURI
import com.deepid.lgc.util.Utils.setFunctionality
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.CaptureMode
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.errors.DocReaderRfidException
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.params.Functionality
import com.regula.documentreader.api.results.DocumentReaderNotification
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.configuration.FaceCaptureConfiguration
import com.regula.facesdk.exception.InitException
import com.regula.facesdk.model.results.FaceCaptureResponse
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File


class MainActivity : AppCompatActivity() {
    private var isShowFaceRecognition = false
    private var isShowRfid = false
    private var loadingDialog: AlertDialog? = null
    private var currentScenario: String = Scenario.SCENARIO_OCR
    private lateinit var binding: ActivityMainBinding
    private val rvAdapter: RecyclerAdapter by lazy {
        RecyclerAdapter(getRvData())
    }

    private fun getRvData(): List<Base> {
        val rvData = mutableListOf<Base>()
        return rvData
    }

    private val scannerViewModel: ScannerViewModel by viewModel()

    @Transient
    val imageBrowsingIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.let { intent ->
                    val imageUris = ArrayList<Uri>()
                    if (intent.clipData == null) {
                        intent.data?.let { uri ->
                            imageUris.add(uri)
                        }
                    } else {
                        intent.clipData?.let { clipData ->
                            for (i in 0 until clipData.itemCount) {
                                imageUris.add(clipData.getItemAt(i).uri)
                            }
                        }
                    }
                    Log.d(TAG, "[DEBUGX] Image Path: ${imageUris[0].path!!} ")
                    val realPath = getRealPathFromURI(imageUris[0], this)
                    scannerViewModel.uploadImage(File(realPath))
                }
            }
        }



    @Transient
    private val completion = IDocumentReaderCompletion { action, results, error ->
        if (action == DocReaderAction.COMPLETE
            || action == DocReaderAction.TIMEOUT
        ) {
            dismissDialog()
            if (results != null) {
                Log.d(
                    TAG,
                    "[DEBUGX] DocReaderAction is Timeout: ${action == DocReaderAction.TIMEOUT} "
                )
                scannerViewModel.setDocumentReaderResults(results)
                ScanResultActivity.documentResults = results
            }
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
            if (results?.chipPage != 0 && isShowRfid) {
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
                        if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                            scannerViewModel.setDocumentReaderResults(results_RFIDReader ?: results)
                            if (isShowFaceRecognition) {
                                captureFace(results_RFIDReader ?: results)
                            } else {
                                displayResults()
                            }
                        }

                    }
                })
            } else {
                Log.d(TAG, "[DEBUGX] NO RFID PERFORMED ")
                /**
                 * perform @livenessFace or @captureFace then check similarity
                 */
                if (isShowFaceRecognition) {
                    if (results != null) {
                        captureFace(results)
                    }
                } else {
                    displayResults()
                }
            }
        } else {
            dismissDialog()
            if (action == DocReaderAction.CANCEL) {
                if (DocumentReader.Instance().functionality().isManualMultipageMode)
                    DocumentReader.Instance().functionality().edit().setManualMultipageMode(false)
                        .apply()

                Toast.makeText(this, "Scanning was cancelled", Toast.LENGTH_LONG).show()
                isShowFaceRecognition = false
                isShowRfid = false
            } else if (action == DocReaderAction.ERROR) {
                Toast.makeText(this, "Error:$error", Toast.LENGTH_LONG).show()
                isShowFaceRecognition = false
                isShowRfid = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        observe()
        initFaceSDK()
        prepareDatabase()
        setupFunctionality()
    }

    override fun onResume() {
        super.onResume()
        setFunctionality(Functionality())
        setupFunctionality()
    }

    private fun setupFunctionality() {
        DocumentReader.Instance().processParams().timeout = Double.MAX_VALUE
        DocumentReader.Instance().processParams().timeoutFromFirstDetect = Double.MAX_VALUE
        DocumentReader.Instance().processParams().timeoutFromFirstDocType = Double.MAX_VALUE
        DocumentReader.Instance().functionality().edit()
            .setBtDeviceName("Regula 0326")
            .setShowCaptureButton(true)
            .setShowCaptureButtonDelayFromStart(0)
            .setShowCaptureButtonDelayFromDetect(0)
            .setCaptureMode(CaptureMode.AUTO)
            .setDisplayMetadata(true)
            .apply()
    }

    private fun initFaceSDK() {
        FaceSDK.Instance().init(this) { status: Boolean, e: InitException? ->
            if (!status) {
                Toast.makeText(
                    this@MainActivity,
                    "Init FaceSDK finished with error: " + if (e != null) e.message else "",
                    Toast.LENGTH_LONG
                ).show()
                return@init
            }
            Log.d(TAG, "[DEBUGX] FaceSDK init completed successfully")
            setButtonEnable()
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
            is ScannerUiState.Loading -> {
                if (uiState.isLoading) {
                    showDialog("Upload Image")
                } else {
                    dismissDialog()
                }
            }

            is ScannerUiState.Error -> Toast.makeText(
                this,
                "Error ${uiState.message}",
                Toast.LENGTH_LONG
            ).show()

            is ScannerUiState.Success -> Toast.makeText(
                this,
                "Image has been uploaded",
                Toast.LENGTH_LONG
            ).show()
        }

    }

    private fun prepareDatabase() {
        showDialog("preparing database")
        DocumentReader.Instance()
            .prepareDatabase(//call prepareDatabase not necessary if you have local database at assets/Regula/db.dat
                this@MainActivity,
                "FullAuth",
                object : IDocumentReaderPrepareCompletion {
                    override fun onPrepareProgressChanged(progress: Int) {
                        if (loadingDialog != null)
                            loadingDialog?.setTitle("Downloading database: $progress%")
                    }

                    override fun onPrepareCompleted(
                        status: Boolean,
                        error: DocumentReaderException?
                    ) {
                        if (status) {
                            Log.d(TAG, "[DEBUGX] database onPreparedComplete then initializeReader")
                            initializeReader()
                        } else {
                            dismissDialog()
                            Toast.makeText(
                                this@MainActivity,
                                "Prepare DB failed:$error",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
    }

    private fun showScanner() {
        Log.d(TAG, "[DEBUGX] showScanner: currentscenario $currentScenario")
        resetScannerResult()
        val scannerConfig = ScannerConfig.Builder(currentScenario).build()
        DocumentReader.Instance()
            .showScanner(this@MainActivity, scannerConfig, completion)
    }

    private fun resetScannerResult() {
        scannerViewModel.setDocumentReaderResults(null)
        scannerViewModel.setFaceCaptureResponse(null)
    }

    fun captureFace(results: DocumentReaderResults?) {
        val faceCaptureConfiguration: FaceCaptureConfiguration =
            FaceCaptureConfiguration.Builder()
                .registerUiFragmentClass(FaceCameraFragment::class.java)
                .setCloseButtonEnabled(true)
                .setCameraSwitchEnabled(false)
                .build()
        FaceSDK.Instance()
            .presentFaceCaptureActivity(
                this@MainActivity,
                faceCaptureConfiguration
            ) { response: FaceCaptureResponse ->
                scannerViewModel.setFaceCaptureResponse(response)
                ScanResultActivity.faceCaptureResponse = response
                // ... check response.image for capture result
                if (response.image?.bitmap == null) {
                    response.exception?.message?.let {
                        Toast.makeText(
                            this@MainActivity,
                            "Error: $it",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                displayResults()
                isShowFaceRecognition = false
                isShowRfid = false
            }
    }

    private fun displayResults() {
        startActivity(Intent(this, ScanResultActivity::class.java))
    }

    private fun createImageBrowsingRequest() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        intent.action = Intent.ACTION_GET_CONTENT
        imageBrowsingIntentLauncher.launch(Intent.createChooser(intent, "Select Picture"))
    }

    private fun recognizeImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
            )
        } else
            createImageBrowsingRequest()
    }

    private fun initViews() {
        with(binding.contentMain) {
            menuRv.layoutManager = LinearLayoutManager(this@MainActivity)
            menuRv.adapter = rvAdapter
            btnOcr.setOnClickListener {
                showScanner()
            }
            btnFacial.setOnClickListener {
                showFullScanner(faceRecognition = true, rfid = false)
            }
            btnChip.setOnClickListener {
                showFullScanner(faceRecognition = true, rfid = true)
            }
            btnConnect.setOnClickListener { _: View? ->
                startActivity(Intent(this@MainActivity, InputDeviceActivity::class.java))
            }
            btnCertificate.setOnClickListener {
                recognizeImage()
            }
            btnVisible.setOnClickListener {
                startActivity(Intent(this@MainActivity, CustomerInformationActivity::class.java))
            }
            btnInvisible.setOnClickListener {
                startActivity(Intent(this@MainActivity, InputDeviceActivity::class.java))
            }
            btnAuto.setOnClickListener {  }
            btnPrescription.setOnClickListener {
                startActivity(Intent(this@MainActivity, SearchCustomerInformationActivity::class.java))
            }
        }

    }

    fun initializeReader() {
        Log.d(TAG, "[DEBUGX] initializeReader")
        val license = Utils.getLicense(this) ?: return
        showDialog("Initializing")

        DocumentReader.Instance()
            .initializeReader(this@MainActivity, DocReaderConfig(license), initCompletion)
    }

    private val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (result) {
                Log.d(TAG, "[DEBUGX] init DocumentReaderSDK is complete")
                setButtonEnable()
            } else {
                Log.e(TAG, "[DEBUGX] init DocumentReaderSDK is failed: $error ")
                Toast.makeText(this@MainActivity, "Init failed:$error", Toast.LENGTH_LONG).show()
                return@IDocumentReaderInitCompletion
            }
        }

    private fun setButtonEnable() {
        if (FaceSDK.Instance().isInitialized && DocumentReader.Instance().isReady) {
            with(binding.contentMain) {
                btnOcr.isEnabled = true
                btnFacial.isEnabled = true
                btnChip.isEnabled = true
                btnConnect.isEnabled = true
                btnCertificate.isEnabled = true
                btnVisible.isEnabled = true
                btnInvisible.isEnabled = true
                btnPrescription.isEnabled = true
            }
        } else {
            with(binding.contentMain) {
                btnOcr.isEnabled = false
                btnFacial.isEnabled = false
                btnChip.isEnabled = false
                btnConnect.isEnabled = false
                btnCertificate.isEnabled = false
                btnVisible.isEnabled = false
                btnInvisible.isEnabled = false
                btnAuto.isEnabled = false
                btnPrescription.isEnabled = false
            }
        }
    }

    private fun showFullScanner(faceRecognition: Boolean, rfid: Boolean) {
        isShowFaceRecognition = faceRecognition
        isShowRfid = rfid
        showScanner()
    }

    private fun dismissDialog() {
        if (loadingDialog != null) {
            loadingDialog!!.dismiss()
        }
    }

    private fun showDialog(msg: String?) {
        dismissDialog()
        val builderDialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.simple_dialog, null)
        builderDialog.setTitle(msg)
        builderDialog.setView(dialogView)
        builderDialog.setCancelable(false)
        loadingDialog = builderDialog.show()
    }

    companion object {
        const val TAG: String = "MainActivity"
    }
}

package com.deepscope.deepscope.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepscope.deepscope.R
import com.deepscope.deepscope.databinding.ActivityHomeBinding
import com.deepscope.deepscope.ui.BaseRegulaSdkActivity
import com.deepscope.deepscope.ui.common.FaceCameraFragment
import com.deepscope.deepscope.ui.common.RecyclerAdapter
import com.deepscope.deepscope.ui.customerInformation.CustomerInformationActivity
import com.deepscope.deepscope.ui.customerInformation.search.SearchCustomerInformationActivity
import com.deepscope.deepscope.ui.result.ScanResultActivity
import com.deepscope.deepscope.ui.scanner.InputDeviceActivity
import com.deepscope.deepscope.ui.scanner.ScannerUiState
import com.deepscope.deepscope.ui.scanner.ScannerViewModel
import com.deepscope.deepscope.util.Base
import com.deepscope.deepscope.util.Empty
import com.deepscope.deepscope.util.Utils.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
import com.deepscope.deepscope.util.Utils.getRealPathFromURI
import com.deepscope.deepscope.util.Utils.resetFunctionality
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.enums.CaptureMode
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.errors.DocReaderRfidException
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.Functionality
import com.regula.documentreader.api.results.DocumentReaderNotification
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.callback.FaceCaptureCallback
import com.regula.facesdk.configuration.FaceCaptureConfiguration
import com.regula.facesdk.exception.InitException
import com.regula.facesdk.model.results.FaceCaptureResponse
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File

class HomeActivity : BaseRegulaSdkActivity() {
    override val currentScenario: String = Scenario.SCENARIO_CAPTURE
    private var isShowFaceRecognition = false
    private var isShowRfid = false
    private lateinit var binding: ActivityHomeBinding
    private lateinit var server: ApplicationEngine
    private val rvAdapter: RecyclerAdapter by lazy {
        RecyclerAdapter(getRvData())
    }

    private fun getRvData(): List<Base> {
        val rvData = mutableListOf<Base>()
        return rvData
    }

    private val scannerViewModel: ScannerViewModel by viewModel()

    // pick image from gallery
    @Transient
    val imageBrowsingIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
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
                    Timber.d("[DEBUGX] Image Path: ${imageUris[0].path!!} ")
                    val realPath = getRealPathFromURI(imageUris[0], this)
                    scannerViewModel.uploadImage(File(realPath))
                }
            }
        }

    @Transient
    override val completion = IDocumentReaderCompletion { action, results, error ->
        if (action == DocReaderAction.COMPLETE
            || action == DocReaderAction.TIMEOUT
        ) {
            dismissDialog()
            if (results != null) {
                Timber.d(
                    "[DEBUGX] DocReaderAction is Timeout: ${action == DocReaderAction.TIMEOUT} "
                )
                scannerViewModel.setDocumentReaderResults(results)
                CustomerInformationActivity.documentResults = results
            }
            if (DocumentReader.Instance().functionality().isManualMultipageMode) {
                Timber.d("[DEBUGX] MULTIPAGEMODE: ")
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
                Timber.d("[DEBUGX] RFID IS PERFORMED: ")
                startRfidReader(results)
            } else {
                Timber.d("[DEBUGX] NO RFID PERFORMED ")
                /**
                 * perform @livenessFace or @captureFace then check similarity
                 */
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

                showToast("Scanning was cancelled")
                isShowFaceRecognition = false
                isShowRfid = false
            } else if (action == DocReaderAction.ERROR) {
                showToast("Error:$error")
                isShowFaceRecognition = false
                isShowRfid = false
            }
        }
    }

    private fun startRfidReader(results: DocumentReaderResults?) {
        DocumentReader.Instance().startRFIDReader(this, object : IRfidReaderCompletion() {
            override fun onChipDetected() {
                Timber.d("Rfid Chip detected")
            }

            override fun onProgress(notification: DocumentReaderNotification) {
                //rfidProgress(notification.code, notification.value)
            }

            override fun onRetryReadChip(exception: DocReaderRfidException) {
                Timber.d("Rfid Retry with error: " + exception.errorCode)
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
    }

    override val faceCaptureCallback: FaceCaptureCallback
        get() = FaceCaptureCallback { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initServer()
        initViews()
        observe()
        initFaceSDK()
        prepareDatabase()
        setupFunctionality()
    }

    override fun setupFunctionality() {
        DocumentReader.Instance().processParams().timeout = Double.MAX_VALUE
        DocumentReader.Instance().processParams().timeoutFromFirstDetect = Double.MAX_VALUE
        DocumentReader.Instance().processParams().timeoutFromFirstDocType = Double.MAX_VALUE
        DocumentReader.Instance().functionality().edit()
            .setBtDeviceName("Regula 0326")
            .setShowCaptureButton(true)
            .setShowTorchButton(true)
            .setShowCaptureButtonDelayFromStart(0)
            .setShowCaptureButtonDelayFromDetect(0)
            .setCaptureMode(CaptureMode.AUTO)
            .setDisplayMetadata(true)
            .setPictureOnBoundsReady(true)
            .setCameraMode(2)
            .apply()
    }

    override fun showDialog(msg: String?) {
        dismissDialog()
        val builderDialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.simple_dialog, null)
        builderDialog.setTitle(msg)
        builderDialog.setView(dialogView)
        builderDialog.setCancelable(false)
        loadingDialog = builderDialog.show()
    }

    override fun initFaceSDK() {
        if (!FaceSDK.Instance().isInitialized) {
            FaceSDK.Instance().init(this) { status: Boolean, e: InitException? ->
                if (!status) {
                    showToast(
                        getString(R.string.init_facesdk_finished_with_error) + if (e != null) e.message else String.Empty,
                    )
                    return@init
                }
                Timber.d(getString(R.string.facesdk_init_completed_successfully))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resetFunctionality(Functionality())
        setupFunctionality()
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

            is ScannerUiState.Error -> showToast(
                "Error ${uiState.message}",
            )

            is ScannerUiState.Success -> showToast(

                "Image has been uploaded",
            )
        }

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
                this@HomeActivity,
                faceCaptureConfiguration
            ) { response: FaceCaptureResponse ->
                scannerViewModel.setFaceCaptureResponse(response)
                ScanResultActivity.faceCaptureResponse = response
                // ... check response.image for capture result
                if (response.image?.bitmap == null) {
                    response.exception?.message?.let {
                        showToast(
                            "Error: $it",
                        )
                    }
                }
                displayResults()
                isShowFaceRecognition = false
                isShowRfid = false
            }
    }

    private fun displayResults() {
        val customerIntent = Intent(this, CustomerInformationActivity::class.java)
        customerIntent.putExtra(CustomerInformationActivity.CUSTOMER_INFORMATION_TYPE, 1)
        customerIntent.putExtra(CustomerInformationActivity.CUSTOMER_INFORMATION_FEATURE, 2)
        startActivity(customerIntent)
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
            menuRv.layoutManager = LinearLayoutManager(this@HomeActivity)
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
                startActivity(Intent(this@HomeActivity, InputDeviceActivity::class.java))
            }
            btnCertificate.setOnClickListener {
                recognizeImage()
            }
            btnVisible.setOnClickListener {
                val visibleIntent =
                    Intent(this@HomeActivity, CustomerInformationActivity::class.java)
                visibleIntent.putExtra(CustomerInformationActivity.CUSTOMER_INFORMATION_TYPE, 1)
                startActivity(visibleIntent)
            }
            btnInvisible.setOnClickListener {
                val invisibleIntent = Intent(this@HomeActivity, InputDeviceActivity::class.java)
                invisibleIntent.putExtra(CustomerInformationActivity.CUSTOMER_INFORMATION_TYPE, 1)
                invisibleIntent.putExtra(
                    CustomerInformationActivity.CUSTOMER_INFORMATION_FEATURE,
                    2
                )
                startActivity(invisibleIntent)
            }
            btnAuto.setOnClickListener {
                val autoIntent = Intent(this@HomeActivity, InputDeviceActivity::class.java)
                autoIntent.putExtra(CustomerInformationActivity.CUSTOMER_INFORMATION_TYPE, 1)
                autoIntent.putExtra(CustomerInformationActivity.CUSTOMER_INFORMATION_FEATURE, 3)
                startActivity(autoIntent)
            }
            btnReport.setOnClickListener {
                startActivity(
                    Intent(
                        this@HomeActivity,
                        SearchCustomerInformationActivity::class.java
                    )
                )
            }
        }

    }

    override val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (result) {
                Timber.d("[DEBUGX] init DocumentReaderSDK is complete")
                setButtonEnable()
            } else {
                Timber.d("[DEBUGX] init DocumentReaderSDK is failed: $error ")
                showToast("Init failed:$error")
                return@IDocumentReaderInitCompletion
            }
        }

    private fun setButtonEnable() {
        val isEnable = FaceSDK.Instance().isInitialized && DocumentReader.Instance().isReady
        with(binding.contentMain) {
            btnOcr.isEnabled = isEnable
            btnFacial.isEnabled = isEnable
            btnChip.isEnabled = isEnable
            btnConnect.isEnabled = isEnable
            btnCertificate.isEnabled = isEnable
            btnVisible.isEnabled = isEnable
            btnInvisible.isEnabled = isEnable
            btnAuto.isEnabled = isEnable
            btnReport.isEnabled = isEnable
        }

    }

    private fun showFullScanner(faceRecognition: Boolean, rfid: Boolean) {
        isShowFaceRecognition = faceRecognition
        isShowRfid = rfid
        showScanner()
    }

    /**
     * Starts a server on port 3333
     * Serves static files from the static folder
     * All other routes are served the index.html file
     * */
    private fun initServer() {
        server = embeddedServer(Netty, 3333) {
            install(ContentNegotiation) {
                gson {}
            }
            routing {
                static {
                    resource("/", "index.html")
                    resource("*", "index.html")
                    static("static") {
                        resources("static")
                    }
                }
            }
        }
            .start(wait = false)
    }

    override fun onDestroy() {
        server.stop(0, 0)
        super.onDestroy()
    }

    companion object {
        const val TAG: String = "HomeActivity"
    }
}
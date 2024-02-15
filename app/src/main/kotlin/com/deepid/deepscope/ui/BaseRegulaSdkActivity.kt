package com.deepid.deepscope.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.deepid.deepscope.R
import com.deepid.deepscope.ui.common.FaceCameraFragment
import com.deepid.deepscope.ui.scanner.InputDeviceActivity
import com.deepid.deepscope.util.Utils
import com.deepid.deepscope.util.debounce
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.params.Functionality
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.callback.FaceCaptureCallback
import com.regula.facesdk.configuration.FaceCaptureConfiguration
import com.regula.facesdk.exception.InitException

abstract class BaseRegulaSdkActivity : AppCompatActivity() {
    protected open var currentScenario: String = Scenario.SCENARIO_CAPTURE
    protected var loadingDialog: AlertDialog? = null
    abstract fun setupFunctionality()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFaceSDK()
        prepareDatabase()
        Utils.setFunctionality(Functionality())
        setupFunctionality()
    }

    protected fun dismissDialog() {
        if (loadingDialog != null) {
            loadingDialog!!.dismiss()
        }
    }

    protected fun showDialog(msg: String?) {
        dismissDialog()
        val builderDialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.simple_dialog, null)
        builderDialog.setTitle(msg)
        builderDialog.setView(dialogView)
        builderDialog.setCancelable(false)
        loadingDialog = builderDialog.show()
    }

    protected fun handler(delay: Long): () -> Unit = lifecycleScope.debounce(delay) {
        Toast.makeText(
            this,
            "Failed to connect to the torch device",
            Toast.LENGTH_SHORT
        ).show()
        dismissDialog()
    }

    private fun prepareDatabase() {
        showDialog("preparing database")
        DocumentReader.Instance()
            .prepareDatabase(//call prepareDatabase not necessary if you have local database at assets/Regula/db.dat
                this,
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
                            Log.d(
                                InputDeviceActivity.TAG,
                                "[DEBUGX] database onPreparedComplete then initializeReader"
                            )
                            initializeReader()
                        } else {
                            dismissDialog()
                            Toast.makeText(
                                this@BaseRegulaSdkActivity,
                                "Prepare DB failed:$error",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
    }

    protected fun initializeReader() {
        val license = Utils.getLicense(this) ?: return
        showDialog("Initializing")

        DocumentReader.Instance()
            .initializeReader(this, DocReaderConfig(license), initCompletion)
    }

    abstract val initCompletion: IDocumentReaderInitCompletion
    abstract val completion: IDocumentReaderCompletion
    abstract val faceCaptureCallback: FaceCaptureCallback

    protected open fun showScanner() {
        Log.d(null, "DEBUGX showScanner: currentscenario $currentScenario")
        val scannerConfig = ScannerConfig.Builder(currentScenario).build()
        DocumentReader.Instance()
            .showScanner(this, scannerConfig, completion)
    }

    protected fun captureFace() {
        val faceCaptureConfiguration: FaceCaptureConfiguration =
            FaceCaptureConfiguration.Builder()
                .registerUiFragmentClass(FaceCameraFragment::class.java)
                .setCloseButtonEnabled(true)
                .setCameraSwitchEnabled(false)
                .build()
        FaceSDK.Instance()
            .presentFaceCaptureActivity(
                this,
                faceCaptureConfiguration, faceCaptureCallback
            )
    }

    protected open fun initFaceSDK() {
        if (!FaceSDK.Instance().isInitialized) {
            FaceSDK.Instance().init(this) { status: Boolean, e: InitException? ->
                if (!status) {
                    Toast.makeText(
                        this,
                        "Init FaceSDK finished with error: " + if (e != null) e.message else "",
                        Toast.LENGTH_LONG
                    ).show()
                    return@init
                }
                Log.d(null, "FaceSDK init completed successfully")
            }
        }
    }
}

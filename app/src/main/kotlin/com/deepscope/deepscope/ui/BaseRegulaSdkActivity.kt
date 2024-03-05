package com.deepscope.deepscope.ui

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.deepscope.deepscope.R
import com.deepscope.deepscope.ui.common.FaceCameraFragment
import com.deepscope.deepscope.util.Empty
import com.deepscope.deepscope.util.Utils
import com.deepscope.deepscope.util.debounce
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.callback.FaceCaptureCallback
import com.regula.facesdk.configuration.FaceCaptureConfiguration
import com.regula.facesdk.exception.InitException
import timber.log.Timber

abstract class BaseRegulaSdkActivity : AppCompatActivity() {
    abstract val initCompletion: IDocumentReaderInitCompletion
    abstract val completion: IDocumentReaderCompletion
    abstract val faceCaptureCallback: FaceCaptureCallback
    protected open val currentScenario: String = Scenario.SCENARIO_CAPTURE
    protected var loadingDialog: AlertDialog? = null
    abstract fun showDialog(msg: String?)
    abstract fun setupFunctionality()

    protected fun dismissDialog() {
        if (loadingDialog != null) {
            loadingDialog!!.dismiss()
        }
    }

    protected fun handler(delay: Long): () -> Unit = lifecycleScope.debounce(delay) {
        showToast(
            getString(R.string.failed_to_connect_to_the_torch_device),
        )
        dismissDialog()
    }

    protected fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    protected fun prepareDatabase() {
        showDialog(getString(R.string.preparing_database))
        DocumentReader.Instance()
            .prepareDatabase(//call prepareDatabase not necessary if you have local database at assets/Regula/db.dat
                this,
                "FullAuth",
                object : IDocumentReaderPrepareCompletion {
                    override fun onPrepareProgressChanged(progress: Int) {
                        if (loadingDialog != null)
                            loadingDialog?.setTitle(
                                getString(
                                    R.string.downloading_database,
                                    progress
                                )
                            )
                    }

                    override fun onPrepareCompleted(
                        status: Boolean,
                        error: DocumentReaderException?
                    ) {
                        if (status) {
                            Timber.d(
                                "[DEBUGX] database onPreparedComplete then initializeReader"
                            )
                            initializeReader()
                        } else {
                            dismissDialog()
                            showToast(
                                getString(R.string.prepare_db_failed, error),
                            )
                        }
                    }
                })
    }

    protected fun initializeReader() {
        val license = Utils.getLicense(this) ?: return
        showDialog(getString(R.string.initializing))

        DocumentReader.Instance()
            .initializeReader(this, DocReaderConfig(license), initCompletion)
    }

    protected open fun showScanner() {
        Timber.d("DEBUGX showScanner: currentscenario $currentScenario")
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
                    showToast(
                        getString(R.string.init_facesdk_finished_with_error) + if (e != null) e.message else String.Empty,
                    )
                    return@init
                }
                Timber.d(getString(R.string.facesdk_init_completed_successfully))
            }
        }
    }
}

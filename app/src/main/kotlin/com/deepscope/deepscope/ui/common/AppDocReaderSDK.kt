package com.deepscope.deepscope.ui.common

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.deepscope.deepscope.R
import com.deepscope.deepscope.util.Utils
import com.deepscope.deepscope.util.debounce
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.results.DocumentReaderResults
import timber.log.Timber

interface IRegulaDocReaderSDK {
    val documentReaderInstance: DocumentReader
    val documentReaderInitCompletion: IDocumentReaderInitCompletion
    val documentReaderCompletion: IDocumentReaderCompletion
    fun showScanner()
    fun startRfid()

    /**
     * Change configuration and prepare for new scanning
     */
    fun setupFunctionality()

    /**
     * A function that was called after DocumentReaderSDK is complete initializing
     */
    fun onDocReaderInitComplete()

    /**
     * A function that was called after DocumentReaderSDK is complete scanning
     */
    fun onDocReaderCompleteScan(results: DocumentReaderResults)
}

abstract class AppDocReaderSDK : Fragment(), IRegulaDocReaderSDK {
    /// RegulaDocumentReaderSDK
    protected open val currentScenario: String = Scenario.SCENARIO_CAPTURE
    override val documentReaderInstance = DocumentReader.Instance()
    override val documentReaderInitCompletion: IDocumentReaderInitCompletion
        get() = IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (result) {
                Timber.d("[DEBUGX] init DocumentReaderSDK is complete")
                onDocReaderInitComplete()
            } else {
                Timber.d("[DEBUGX] init DocumentReaderSDK is failed: $error ")
                showToast("Init failed:$error")
                return@IDocumentReaderInitCompletion
            }
        }

    private fun handler(delay: Long): () -> Unit = lifecycleScope.debounce(delay) {
        showToast(getString(R.string.failed_to_connect_to_the_torch_device))
        dismissDialog()
    }

    private val databaseDocumentReaderPrepareCompletion =
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
                    Timber.d("[DEBUGX] database onPreparedComplete then initializeReader")
                    initializeReader()
                } else {
                    dismissDialog()
                    showToast(
                        getString(R.string.prepare_db_failed, error),
                    )
                }
            }
        }

    override val documentReaderCompletion: IDocumentReaderCompletion
        get() = IDocumentReaderCompletion {
                action: Int,
                results: DocumentReaderResults?,
                error: DocumentReaderException?,
            ->
            dismissDialog()
            when (action) {
                DocReaderAction.COMPLETE,
                DocReaderAction.TIMEOUT -> {
                    if (results != null) {
                        Timber.d("[DEBUGX] DocReaderAction is Timeout: ${action == DocReaderAction.TIMEOUT} | result != null: ${results != null} | action is completed :${action == DocReaderAction.COMPLETE}")
                        onDocReaderCompleteScan(results)
                    } else {
                        Timber.d("[DEBUGX] DocumentReaderResults is null and action is Timeout: ${action == DocReaderAction.TIMEOUT}")
                        showToast("DocumentReaderResults is null")
                    }
                }
                DocReaderAction.CANCEL -> {
                    showToast(getString(R.string.scanning_was_cancelled))
                }
                DocReaderAction.ERROR -> {
                    Timber.e(error.toString())
                    showToast("Error:$error")
                }
                else -> {
                    showToast("Unknown action:$action")
                }
            }

        }

    protected var loadingDialog: AlertDialog? = null
    protected fun showDialog(msg: String?) {
        dismissDialog()
        val builderDialog = AlertDialog.Builder(requireActivity())
        val dialogView = layoutInflater.inflate(R.layout.simple_dialog, null)
        builderDialog.setTitle(msg)
        builderDialog.setView(dialogView)
        builderDialog.setCancelable(false)
        loadingDialog = builderDialog.show()
    }

    protected fun showToast(message: String) {
        Toast.makeText(
            requireActivity(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    protected fun dismissDialog() {
        if (loadingDialog != null) {
            loadingDialog!!.dismiss()
        }
    }

    protected fun prepareDatabase() {
        showDialog(requireActivity().getString(R.string.preparing_database))
        // call prepareDatabase not necessary if you have local database at assets/Regula/db.dat
        documentReaderInstance
            .prepareDatabase(
                requireActivity(),
                REGULA_DATABASE_ID,
                databaseDocumentReaderPrepareCompletion,
            )
    }

    protected fun initializeReader() {
        val license = Utils.getLicense(requireActivity()) ?: return
        showDialog(getString(R.string.initializing))
        documentReaderInstance
            .initializeReader(
                requireActivity(),
                DocReaderConfig(license),
                documentReaderInitCompletion
            )
    }

    override fun showScanner() {
        if (!documentReaderInstance.isReady) {
            initializeReader()
            return
        }
        Timber.d("DEBUGX showScanner: currentscenario $currentScenario")
        val scannerConfig = ScannerConfig.Builder(currentScenario).build()
        documentReaderInstance
            .showScanner(requireActivity(), scannerConfig, documentReaderCompletion)
    }

    override fun startRfid() {
        Timber.d("DEBUGX startRfid")
        documentReaderInstance.startRFIDReader(
            requireActivity(),
            object : IRfidReaderCompletion() {
                override fun onCompleted(
                    rfidAction: Int,
                    resultsRFIDReader: DocumentReaderResults?,
                    error: DocumentReaderException?
                ) {
                    when (rfidAction) {
                        DocReaderAction.COMPLETE -> {
                            //TODO("You should handle RFID Complete Case case")
                        }

                        DocReaderAction.CANCEL, DocReaderAction.ERROR, DocReaderAction.TIMEOUT -> {
                            //TODO("You should handle RFID Cancel/Error/Timeout Case case")
                        }

                        else -> {
                            //TODO("You should handle RFID Unknown Case case")
                        }
                    }
                }

            })
    }

    companion object {
        const val REGULA_DATABASE_ID = "FullAuth"
    }
}

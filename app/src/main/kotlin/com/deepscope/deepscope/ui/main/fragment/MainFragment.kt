package com.deepscope.deepscope.ui.main.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.deepscope.deepscope.databinding.FragmentMainBinding
import com.deepscope.deepscope.ui.common.AppDocReaderSDK
import com.deepscope.deepscope.ui.customerInformation.CustomerInformationFragment
import com.deepscope.deepscope.util.Utils.REGULA_0326
import com.regula.documentreader.api.enums.CaptureMode
import com.regula.documentreader.api.results.DocumentReaderResults
import timber.log.Timber

class MainFragment : AppDocReaderSDK() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!


    override fun onDocReaderCompleteScan(results: DocumentReaderResults) {
        // Set the results to the CustomerInformationFragment
        CustomerInformationFragment.documentResults = results
        Timber.d("DEBUGX onDocReaderCompleteScan : $results")
        goToCustomerInformation()
    }

    private fun setButtonEnable(isEnable: Boolean) {
        with(binding.contentMain) {
            btnVisible.isEnabled = isEnable
            btnInvisible.isEnabled = isEnable
            btnAuto.isEnabled = isEnable
            btnReport.isEnabled = isEnable
        }
    }

    override fun setupFunctionality() {
        documentReaderInstance.processParams().timeout = Double.MAX_VALUE
        documentReaderInstance.processParams().timeoutFromFirstDetect = Double.MAX_VALUE
        documentReaderInstance.processParams().timeoutFromFirstDocType = Double.MAX_VALUE
        documentReaderInstance.functionality().edit()
            .setBtDeviceName(REGULA_0326)
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

    override fun onDocReaderInitComplete() {
        // Enable buttons when the DocumentReaderSDK is ready
        setButtonEnable(documentReaderInstance.isReady)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        prepareDatabase()
        setupFunctionality()
    }

    private fun initViews() {
        with(binding.contentMain) {
            btnVisible.setOnClickListener(::goToCustomerInformation)
            btnInvisible.setOnClickListener(::goToBleDeviceFragmentInvisibleFeature)
            btnAuto.setOnClickListener(::goToBleDeviceFragmentAutoFeature)
            btnReport.setOnClickListener(::goToSearchCustomerInformationFragment)
        }
    }

    private fun goToCustomerInformation() {
        Timber.d("DEBUGX goToCustomerInformation")
        val direction =
            MainFragmentDirections.actionMainFragmentToCustomerInformationFragment(
                customerInformationFeature = 2, // invisible feature
                customerInformationType = 1, // create type
            )
        if (!isResumed) {
            // add navigation to runnable as fragment is not resumed
            runnable = Runnable {
                findNavController().navigate(direction)
                runnable = null
            }
        } else {
            // navigate normally as fragment is already resumed
            findNavController().navigate(direction)
        }

    }

    private var runnable: Runnable? = null // Runnable object to contain the navigation code

    override fun onResume() {
        super.onResume()

        // run any task waiting for this fragment to be resumed
        runnable?.run()
    }

    private fun goToCustomerInformation(view: View) {
        val direction = MainFragmentDirections.actionMainFragmentToCustomerInformationFragment(
            customerInformationId = null,
            customerInformationFeature = 1,
            customerInformationType = 1,
        )
        return findNavController()
            .navigate(direction)
    }

    private fun goToBleDeviceFragmentInvisibleFeature(view: View) {
        val direction = MainFragmentDirections.actionMainFragmentToBleDeviceFragment(2)
        return findNavController()
            .navigate(direction)
    }

    private fun goToBleDeviceFragmentAutoFeature(view: View) {
        val direction = MainFragmentDirections.actionMainFragmentToBleDeviceFragment(
            customerInformationFeature = 3
        )
        return findNavController()
            .navigate(direction)
    }

    private fun goToSearchCustomerInformationFragment(view: View) {
        val direction =
            MainFragmentDirections.actionMainFragmentToSearchCustomerInformationFragment()
        try {
            view.findNavController()
                .navigate(direction)
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            Timber.d("DEBUGX goToSearchCustomerInformationFragment")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
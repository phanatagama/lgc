package com.deepid.lgc.ui.customerInformation.search

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepid.lgc.databinding.ActivitySearchCustomerInformationBinding
import com.deepid.lgc.databinding.RvItemCustomerInformationBinding
import com.deepid.lgc.domain.model.CustomerInformation
import com.deepid.lgc.domain.model.generateCustomerInformation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchCustomerInformationActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchCustomerInformationBinding
    private val searchCustomerInformationViewModel: SearchCustomerInformationViewModel by viewModel()
    private val onItemClickListener: CustomerInformationAdapter.OnItemClickListener =
        object : CustomerInformationAdapter.OnItemClickListener {
            override fun onItemClickListener(
                view: RvItemCustomerInformationBinding,
                customerInformation: CustomerInformation
            ) {
                Toast.makeText(
                    this@SearchCustomerInformationActivity,
                    "${customerInformation.name} Clicked",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    private val rvAdapter by lazy {
        CustomerInformationAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchCustomerInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindViews()
        observe()
    }

    private fun observe() {
        lifecycleScope.launch {
            searchCustomerInformationViewModel.state.flowWithLifecycle(
                lifecycle,
                Lifecycle.State.STARTED
            )
                .collect {
                    showLoading(it.loading)

                    rvAdapter.submitList(it.customerInformation)

                    if (it.message != null) {
                        Toast.makeText(
                            this@SearchCustomerInformationActivity,
                            it.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        searchCustomerInformationViewModel.removeMessage()
                    }
                }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }


    private fun bindViews() {
        with(binding) {
            rvItemCustomerInfo.layoutManager =
                LinearLayoutManager(this@SearchCustomerInformationActivity)
            rvItemCustomerInfo.adapter = rvAdapter
            rvAdapter.listener = onItemClickListener
            rvItemCustomerInfo.addItemDecoration(
                DividerItemDecoration(
                    this@SearchCustomerInformationActivity,
                    LinearLayoutManager.VERTICAL
                )
            )
            rvAdapter.submitList(generateCustomerInformation)

            searchView.setOnQueryTextListener(
                searchQueryListener
            )
        }
    }

    private val searchQueryListener = object :
        SearchView.OnQueryTextListener {
        var debouncePeriod: Long = 500

        private val coroutineScope = lifecycleScope

        private var searchJob: Job? = null

        override fun onQueryTextSubmit(query: String): Boolean {
            return false
        }

        override fun onQueryTextChange(newText: String): Boolean {
            searchJob?.cancel()
            searchJob = coroutineScope.launch {
                newText.let {
                    delay(debouncePeriod)
                    Log.d(null, "onQueryTextChange: $newText")
                    if (it.isEmpty()) {
                        searchCustomerInformationViewModel.getCustomerInformation()
                    } else {
                        searchCustomerInformationViewModel.getCustomerInformation(newText)
                    }
                }
            }

            return true
        }
    }
}
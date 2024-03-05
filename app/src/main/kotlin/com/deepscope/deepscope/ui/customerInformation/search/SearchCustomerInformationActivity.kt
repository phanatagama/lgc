package com.deepscope.deepscope.ui.customerInformation.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepscope.deepscope.databinding.ActivitySearchCustomerInformationBinding
import com.deepscope.deepscope.databinding.RvItemCustomerInformationBinding
import com.deepscope.deepscope.domain.model.CustomerInformation
import com.deepscope.deepscope.ui.customerInformation.CustomerInformationActivity
import com.deepscope.deepscope.util.Utils.DEBOUNCE_PERIOD
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class SearchCustomerInformationActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchCustomerInformationBinding
    private val searchCustomerInformationViewModel: SearchCustomerInformationViewModel by viewModel()
    private val onItemClickListener: CustomerInformationAdapter.OnItemClickListener =
        object : CustomerInformationAdapter.OnItemClickListener {
            override fun onItemClickListener(
                view: RvItemCustomerInformationBinding,
                customerInformation: CustomerInformation
            ) {
                goToCustomerInformationActivity(customerInformation)
            }

            override fun onDeleteItem(customerInformation: CustomerInformation) {
                searchCustomerInformationViewModel.deleteCustomerInformation(customerInformation)
            }
        }

    private fun goToCustomerInformationActivity(customerInformation: CustomerInformation) {
        val visibleIntent =
            Intent(this@SearchCustomerInformationActivity, CustomerInformationActivity::class.java)
        visibleIntent.putExtra(CustomerInformationActivity.CUSTOMER_INFORMATION_TYPE, 2)
        visibleIntent.putExtra(
            CustomerInformationActivity.CUSTOMER_INFORMATION_ID,
            customerInformation.id
        )
        startActivity(visibleIntent)
    }

    private val rvAdapter by lazy {
        CustomerInformationAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchCustomerInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
                        showToast(it.message)
                        searchCustomerInformationViewModel.removeMessage()
                    }
                }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
            searchView.setOnQueryTextListener(
                searchQueryListener
            )
        }
    }

    private val searchQueryListener = object :
        SearchView.OnQueryTextListener {
        var debouncePeriod: Long = DEBOUNCE_PERIOD

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
                    Timber.d("onQueryTextChange: $newText")
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

    companion object {
        const val TAG: String = "SearchCustomerInformationActivity"
    }
}
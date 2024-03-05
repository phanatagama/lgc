package com.deepscope.deepscope.ui.customerInformation.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepscope.deepscope.databinding.FragmentSearchCustomerInformationBinding
import com.deepscope.deepscope.databinding.RvItemCustomerInformationBinding
import com.deepscope.deepscope.domain.model.CustomerInformation
import com.deepscope.deepscope.util.Utils
import com.orhanobut.logger.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchCustomerInformationFragment : Fragment() {
    private var _binding: FragmentSearchCustomerInformationBinding? = null
    private val binding get() = _binding!!
    private val searchCustomerInformationViewModel: SearchCustomerInformationViewModel by viewModel()
    private val rvAdapter by lazy {
        CustomerInformationAdapter()
    }
    private val onItemClickListener: CustomerInformationAdapter.OnItemClickListener =
        object : CustomerInformationAdapter.OnItemClickListener {
            override fun onItemClickListener(
                view: RvItemCustomerInformationBinding,
                customerInformation: CustomerInformation
            ) {
                if (customerInformation.id == null) return
                goToCustomerInformation(customerInformation.id)
            }

            override fun onDeleteItem(customerInformation: CustomerInformation) {
                searchCustomerInformationViewModel.deleteCustomerInformation(customerInformation)
            }
        }

    private fun goToCustomerInformation(customerId: String) {
        val action = SearchCustomerInformationFragmentDirections.actionSearchCustomerInformationFragmentToCustomerInformationFragment(customerInformationId = customerId, customerInformationType = 2)
        findNavController().navigate(action)
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            searchCustomerInformationViewModel.state.flowWithLifecycle(
                viewLifecycleOwner.lifecycle,
                Lifecycle.State.STARTED
            ).collect {
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
        Toast.makeText(
            requireActivity(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun bindViews() {
        with(binding) {
            rvItemCustomerInfo.layoutManager =
                LinearLayoutManager(requireActivity())
            rvItemCustomerInfo.adapter = rvAdapter
            rvAdapter.listener = onItemClickListener
            rvItemCustomerInfo.addItemDecoration(
                DividerItemDecoration(
                    requireActivity(),
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
        private val coroutineScope = lifecycleScope
        private var searchJob: Job? = null

        override fun onQueryTextSubmit(query: String): Boolean {
            return false
        }

        override fun onQueryTextChange(newText: String): Boolean {
            searchJob?.cancel()
            searchJob = coroutineScope.launch {
                newText.let {
                    delay(Utils.DEBOUNCE_PERIOD)
                    Logger.d("onQueryTextChange: $newText")
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSearchCustomerInformationBinding.inflate(inflater, container, false)
        bindViews()
        observe()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
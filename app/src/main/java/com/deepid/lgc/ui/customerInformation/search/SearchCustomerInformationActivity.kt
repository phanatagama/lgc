package com.deepid.lgc.ui.customerInformation.search

import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepid.lgc.databinding.ActivitySearchCustomerInformationBinding
import com.deepid.lgc.domain.model.generateCustomerInformation

class SearchCustomerInformationActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchCustomerInformationBinding
    private val rvAdapter by lazy {
        CustomerInformationAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchCustomerInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindViews()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    private fun bindViews() {
        with(binding) {
            rvItemCustomerInfo.layoutManager =
                LinearLayoutManager(this@SearchCustomerInformationActivity)
            rvItemCustomerInfo.adapter = rvAdapter
            rvItemCustomerInfo.addItemDecoration(
             DividerItemDecoration(
                    this@SearchCustomerInformationActivity,
                    DividerItemDecoration.HORIZONTAL
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
        override fun onQueryTextSubmit(query: String): Boolean {
            return false
        }

        override fun onQueryTextChange(newText: String): Boolean {
            // TODO: filter item here
            Log.d(null, "onQueryTextChange: $newText")
            return true
        }
    }
}
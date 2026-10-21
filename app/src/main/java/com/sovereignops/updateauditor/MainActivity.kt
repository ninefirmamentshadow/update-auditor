package com.sovereignops.updateauditor

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sovereignops.updateauditor.model.PackageOrigin
import com.sovereignops.updateauditor.ui.DeviceHeaderAdapter
import com.sovereignops.updateauditor.ui.EdgeToEdgeInsets
import com.sovereignops.updateauditor.ui.FilterButtons
import com.sovereignops.updateauditor.ui.InventoryViewModel
import com.sovereignops.updateauditor.ui.PackageAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: InventoryViewModel by viewModels { InventoryViewModel.factory(applicationContext) }

    private val deviceAdapter = DeviceHeaderAdapter()
    private val packageAdapter = PackageAdapter { item ->
        startActivity(PackageDetailActivity.intent(this, item.packageName))
    }

    private lateinit var summaryText: TextView
    private lateinit var emptyText: TextView
    private lateinit var refreshButton: Button
    private lateinit var searchInput: EditText
    private lateinit var filterGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        summaryText = findViewById(R.id.summaryText)
        emptyText = findViewById(R.id.emptyText)
        refreshButton = findViewById(R.id.refreshButton)
        searchInput = findViewById(R.id.searchInput)
        filterGroup = findViewById(R.id.filterGroup)
        val list = findViewById<RecyclerView>(R.id.packageList)

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = ConcatAdapter(deviceAdapter, packageAdapter)
        EdgeToEdgeInsets.install(root = findViewById(R.id.root), top = findViewById(R.id.header), bottom = list)

        refreshButton.setOnClickListener { viewModel.refresh() }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = viewModel.setQuery(s?.toString().orEmpty())
        })
        filterGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.setFilter(FilterButtons.filterFor(checkedId))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: InventoryViewModel.UiState) {
        refreshButton.isEnabled = !state.scanning
        val status = when {
            state.scanning -> getString(R.string.status_scanning)
            state.error != null -> getString(R.string.status_failed, state.error)
            else -> getString(R.string.status_done)
        }
        deviceAdapter.submit(status, state.device)

        val visible = state.visible
        packageAdapter.submitList(visible)
        summaryText.text = if (state.packages.isEmpty()) {
            ""
        } else {
            val counts = getString(
                R.string.summary_counts,
                state.packages.size,
                state.count(PackageOrigin.FIRST_PARTY),
                state.count(PackageOrigin.SYSTEM),
                state.count(PackageOrigin.USER),
                state.count(PackageOrigin.UNVERIFIED),
            )
            if (visible.size == state.packages.size) counts
            else counts + "\n" + getString(R.string.summary_showing, visible.size, state.packages.size)
        }

        val showEmpty = !state.scanning && visible.isEmpty()
        emptyText.visibility = if (showEmpty) View.VISIBLE else View.GONE
        emptyText.text = getString(if (state.packages.isEmpty()) R.string.empty_no_packages else R.string.empty_no_match)

        // Keep the chip selection in step with the model (e.g. after process restoration).
        val wanted = FilterButtons.buttonFor(state.filter)
        if (filterGroup.checkedRadioButtonId != wanted) filterGroup.check(wanted)
    }
}

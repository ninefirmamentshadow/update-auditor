package com.sovereignops.updateauditor

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sovereignops.updateauditor.model.PackageState
import com.sovereignops.updateauditor.ui.DetailBuilder
import com.sovereignops.updateauditor.ui.DetailViewModel
import com.sovereignops.updateauditor.ui.EdgeToEdgeInsets
import kotlinx.coroutines.launch
import java.text.DateFormat

/**
 * Everything the app can observe about one package: signer digests, the install-source
 * chain, state flags and SDK levels. Display only: there is deliberately no copy, share
 * or export action, and the text is not selectable.
 */
class PackageDetailActivity : AppCompatActivity() {
    private val viewModel: DetailViewModel by viewModels {
        DetailViewModel.factory(applicationContext, intent.getStringExtra(EXTRA_PACKAGE).orEmpty())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        EdgeToEdgeInsets.install(
            root = findViewById(R.id.root),
            top = findViewById(R.id.detailHeader),
            bottom = findViewById(R.id.detailScroll),
        )
        findViewById<Button>(R.id.backButton).setOnClickListener { finish() }
        val title = findViewById<TextView>(R.id.detailTitle)
        val content = findViewById<LinearLayout>(R.id.detailContent)

        title.text = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it, title, content) }
            }
        }
    }

    private fun render(state: DetailViewModel.UiState, title: TextView, content: LinearLayout) {
        content.removeAllViews()
        when (state) {
            DetailViewModel.UiState.Loading -> addMessage(content, getString(R.string.detail_loading))
            DetailViewModel.UiState.NotFound -> addMessage(content, getString(R.string.detail_not_found))
            is DetailViewModel.UiState.Failed -> addMessage(content, getString(R.string.detail_error, state.errorType))
            is DetailViewModel.UiState.Loaded -> show(state.state, title, content)
        }
    }

    private fun addMessage(content: LinearLayout, text: String) {
        content.addView(TextView(this).apply { this.text = text; setPadding(0, 16, 0, 0) })
    }

    private fun show(item: PackageState, title: TextView, content: LinearLayout) {
        title.text = item.label
        val inflater = LayoutInflater.from(this)
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        DetailBuilder.build(resources, item, dateFormat).forEach { section ->
            val heading = inflater.inflate(R.layout.item_detail_section, content, false) as TextView
            heading.text = section.title
            content.addView(heading)
            section.rows.forEach { row ->
                val view = inflater.inflate(R.layout.item_detail_row, content, false)
                view.findViewById<TextView>(R.id.rowLabel).text = row.label
                view.findViewById<TextView>(R.id.rowValue).apply {
                    text = row.value
                    if (row.monospace) typeface = Typeface.MONOSPACE
                }
                content.addView(view)
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE = "com.sovereignops.updateauditor.extra.PACKAGE"

        fun intent(context: Context, packageName: String): Intent =
            Intent(context, PackageDetailActivity::class.java).putExtra(EXTRA_PACKAGE, packageName)
    }
}

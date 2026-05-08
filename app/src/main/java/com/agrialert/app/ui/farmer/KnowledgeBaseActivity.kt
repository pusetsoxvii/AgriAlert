package com.agrialert.app.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agrialert.app.data.AgriAlertRepository
import com.agrialert.app.data.Article
import com.agrialert.app.databinding.ActivityKnowledgeBaseBinding
import com.agrialert.app.ui.adapter.ListItem
import com.agrialert.app.ui.adapter.UniversalAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KnowledgeBaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKnowledgeBaseBinding
    private lateinit var repository: AgriAlertRepository
    private lateinit var adapter: UniversalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKnowledgeBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AgriAlertRepository.build(this)

        setupRecyclerView()
        setupListeners()
        observeArticles()
    }

    private fun setupRecyclerView() {
        adapter = UniversalAdapter(onItemClick = { item ->
            if (item is ListItem.ArticleItem) {
                val intent = Intent(this, ArticleDetailActivity::class.java)
                intent.putExtra("article_id", item.article.id)
                startActivity(intent)
            }
        })
        binding.rvArticles.layoutManager = LinearLayoutManager(this)
        binding.rvArticles.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchArticles(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchArticles(newText ?: "")
                return true
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            observeArticles()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeArticles() {
        lifecycleScope.launch {
            val db = com.agrialert.app.data.AgriAlertDatabase.get(this@KnowledgeBaseActivity)
            db.articleDao().getAll().collectLatest { articles ->
                updateList(articles)
            }
        }
    }

    private fun searchArticles(query: String) {
        lifecycleScope.launch {
            val db = com.agrialert.app.data.AgriAlertDatabase.get(this@KnowledgeBaseActivity)
            db.articleDao().search("%$query%").collectLatest { articles ->
                updateList(articles)
            }
        }
    }

    private fun updateList(articles: List<Article>) {
        if (articles.isEmpty()) {
            binding.rvArticles.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvArticles.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
            adapter.updateArticles(articles)
        }
    }
}

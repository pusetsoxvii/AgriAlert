package com.agrialert.app.ui.farmer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agrialert.app.data.AgriAlertDatabase
import com.agrialert.app.data.Article
import com.agrialert.app.databinding.ActivityArticleDetailBinding
import com.agrialert.app.ui.adapter.toDisplayDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val articleId = intent.getIntExtra("article_id", -1)
        if (articleId == -1) {
            finish()
            return
        }

        setupListeners()
        loadArticle(articleId)
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadArticle(id: Int) {
        lifecycleScope.launch {
            val db = AgriAlertDatabase.get(this@ArticleDetailActivity)
            val article = withContext(Dispatchers.IO) {
                db.articleDao().getById(id)
            }

            if (article != null) {
                populateUI(article)
            } else {
                Toast.makeText(this@ArticleDetailActivity, "Article not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun populateUI(article: Article) {
        binding.toolbar.title = article.title
        binding.tvTitle.text = article.title
        binding.tvCategory.text = article.category.uppercase()
        binding.tvLastUpdated.text = "Last updated: ${article.lastUpdated.toDisplayDate()}"
        binding.tvContent.text = article.content
    }
}

package com.example.fitfeast

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NewsArticleAdapter(private val articles: List<Article>) : RecyclerView.Adapter<NewsArticleAdapter.ArticleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news_article, parent, false)
        return ArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]
        holder.bind(article)
    }



    override fun getItemCount(): Int = articles.size

    class ArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.newsTitle)
        private val thumbnailView: ImageView = itemView.findViewById(R.id.newsThumbnail)

        fun bind(article: Article) {
            titleView.text = article.title

            Glide.with(itemView.context)
                .load(article.urlToImage)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(thumbnailView)

            itemView.setOnClickListener {
                // Create an intent to start WebViewActivity with the article's URL
                val intent = Intent(itemView.context, WebViewActivity::class.java).apply {
                    putExtra("EXTRA_URL", article.url)
                }
                itemView.context.startActivity(intent)
            }

            Log.d("ArticleViewHolder", "Article bound: ${article.title}")
        }
    }



}

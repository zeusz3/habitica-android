package com.habitrpg.android.habitica.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.habitrpg.android.habitica.R
import com.habitrpg.android.habitica.extensions.notNull
import com.habitrpg.android.habitica.models.FAQArticle
import com.habitrpg.android.habitica.ui.activities.MainActivity
import com.habitrpg.android.habitica.ui.fragments.faq.FAQDetailFragment
import com.habitrpg.android.habitica.ui.helpers.bindView
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class FAQOverviewRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var activity: MainActivity? = null
    private var articles: List<FAQArticle> = emptyList()

    private val resetWalkthroughEvents = PublishSubject.create<String>()

    fun setArticles(articles: List<FAQArticle>) {
        this.articles = articles
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_JUSTIN) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.button_list_item, parent, false)
            ResetWalkthroughViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.plain_list_item, parent, false)
            FAQArticleViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_FAQ) {
            (holder as FAQArticleViewHolder).bind(articles[position - 1])
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            VIEW_TYPE_JUSTIN
        } else {
            VIEW_TYPE_FAQ
        }
    }

    override fun getItemCount(): Int {
        return this.articles.size + 1
    }

    fun getResetWalkthroughEvents(): Flowable<String> {
        return resetWalkthroughEvents.toFlowable(BackpressureStrategy.DROP)
    }

    internal inner class FAQArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private val textView: TextView by bindView(itemView, R.id.textView)

        private var article: FAQArticle? = null

        init {
            textView.setOnClickListener(this)
        }

        fun bind(article: FAQArticle) {
            this.article = article
            this.textView.text = this.article?.question
        }

        override fun onClick(v: View) {
            val fragment = FAQDetailFragment()
            article.notNull {
                fragment.setArticle(it)
            }
            activity?.displayFragment(fragment)
        }
    }

    private inner class ResetWalkthroughViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            val button = itemView as Button
            button.text = itemView.getContext().getString(R.string.reset_walkthrough)
            button.setOnClickListener { resetWalkthroughEvents.onNext("") }
        }
    }

    companion object {

        private const val VIEW_TYPE_JUSTIN = 0
        private const val VIEW_TYPE_FAQ = 1
    }
}

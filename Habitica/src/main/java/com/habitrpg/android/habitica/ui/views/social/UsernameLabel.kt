package com.habitrpg.android.habitica.ui.views.social

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.habitrpg.android.habitica.R
import com.habitrpg.android.habitica.models.members.PlayerTier
import com.habitrpg.android.habitica.ui.views.HabiticaIconsHelper

class UsernameLabel(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val textView = TextView(context)
    private val tierIconView = ImageView(context)

    var username: String? = ""
    set(value) {
        textView.text = value
    }

    var tier: Int = 0
    set(value) {
        field = value
        textView.setTextColor(PlayerTier.getColorForTier(context, value))
        when (value) {
            0 -> tierIconView.visibility = View.GONE
            else -> {
                tierIconView.visibility = View.VISIBLE
                tierIconView.setImageBitmap(HabiticaIconsHelper.imageOfContributorBadge(value.toFloat(), false))
            }
        }
    }

    init {
        val params = LinearLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER_VERTICAL
        addView(textView, params)
        val padding = context?.resources?.getDimension(R.dimen.spacing_small)?.toInt() ?: 0
        textView.setPadding(0, 0, padding, 0)
        addView(tierIconView, params)
    }
}
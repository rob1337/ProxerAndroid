package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
object DividerPrototype : BBPrototype {

    override val startRegex = Regex(" *hr *", REGEX_OPTIONS)
    override val endRegex = Regex("/ *hr *", REGEX_OPTIONS)

    override val canHaveChildren get() = false

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        return listOf(View(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, dip(2))

            setBackgroundColor(ContextCompat.getColor(context, R.color.divider))
        })
    }
}

package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.view.Gravity.CENTER
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.VERTICAL
import me.proxer.app.ui.view.bbcode.BBTree
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
object UnorderedListPrototype : BBPrototype, AutoClosingPrototype {

    override val startRegex = Regex(" *ul( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *ul *", BBPrototype.REGEX_OPTIONS)

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.filter { it.prototype == ListItemPrototype }.flatMap { it.makeViews(context) }

        return listOf(LinearLayout(context).apply {
            val eightDip = dip(8)

            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            orientation = VERTICAL

            childViews.forEach {
                addView(LinearLayout(context).apply {
                    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    orientation = HORIZONTAL

                    addView(TextPrototype.makeView(context, "\u2022").apply {
                        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                            setMargins(0, 0, eightDip, 0)

                            gravity = CENTER
                        }
                    })

                    addView(it)
                })
            }
        })
    }
}

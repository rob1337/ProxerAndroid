package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.view.View
import me.proxer.app.ui.view.bbcode.BBSpoilerView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object SpoilerPrototype : AutoClosingPrototype {

    private val ATTRIBUTE_REGEX = Regex("spoiler *= *(.+?)$", REGEX_OPTIONS)
    private const val TITLE_ARGUMENT = "title"

    override val startRegex = Regex(" *spoiler( *=\"?.+?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *spoiler *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val title = BBUtils.cutAttribute(code, ATTRIBUTE_REGEX)

        return BBTree(this, parent, args = mutableMapOf(TITLE_ARGUMENT to title))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = super.makeViews(context, children, args)
        val title = args[TITLE_ARGUMENT] as String?

        return when (childViews.isEmpty()) {
            true -> childViews
            false -> listOf(BBSpoilerView(context).apply {
                spoilerTitle = title

                childViews.forEach { addView(it) }
            })
        }
    }
}

package me.proxer.app.ui.view.bbcode.prototype

import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.UrlClickableSpan
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
object FacebookPrototype : TextMutatorPrototype, AutoClosingPrototype {

    override val startRegex = Regex(" *facebook_link( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *facebook_link *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: Map<String, Any?>): SpannableStringBuilder {
        val url = text.trim().toString()
        val parsedUrl = HttpUrl.parse(url)

        return when (parsedUrl) {
            null -> text
            else -> globalContext.getString(R.string.view_bbcode_facebook_link).toSpannableStringBuilder().apply {
                setSpan(UrlClickableSpan(parsedUrl), 0, length, SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}

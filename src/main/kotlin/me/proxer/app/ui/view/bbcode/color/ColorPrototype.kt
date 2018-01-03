package me.proxer.app.ui.view.bbcode.color

import android.graphics.Color
import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils

/**
 * @author Ruben Gees
 */
object ColorPrototype : BBPrototype {

    private val availableColors = arrayOf(
            "black" to Color.parseColor("#000000"),
            "orange" to Color.parseColor("#f8a523"),
            "red" to Color.parseColor("#f50022"),
            "blue" to Color.parseColor("#3000fd"),
            "purple" to Color.parseColor("#7c007f"),
            "green" to Color.parseColor("#238107"),
            "white" to Color.parseColor("#ffffff"),
            "gray" to Color.parseColor("#7f7f7f")
    )

    private val availableColorsForRegex = availableColors.joinToString("|") { it.first }
    private val colorsForRegex = "(#[A-Fa-f0-9]{6}|#[A-Fa-f0-9]{8}|$availableColorsForRegex)"

    override val startRegex = Regex(" *color=\"?$colorsForRegex\"?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *color *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val value = BBUtils.cutAttribute(code, "color=") ?: ""

        val color = when (value.startsWith("#")) {
            true -> Color.parseColor(value)
            false -> availableColors.find { it.first == value }?.second
                    ?: throw IllegalArgumentException("Unknown color: $value")
        }

        return ColorTree(color, parent)
    }
}

package me.proxer.app.helper

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.support.v7.app.AppCompatDelegate
import me.proxer.app.helper.MaterialDrawerHelper

/**
 * A helper class, which gives access to the [SharedPreferences].

 * @author Ruben Gees
 */
object PreferenceHelper {

    const val PREFERENCE_HENTAI = "pref_hentai"
    const val PREFERENCE_START_PAGE = "pref_start_page"
    const val PREFERENCE_NEWS_NOTIFICATIONS = "pref_news_notifications"
    const val PREFERENCE_NEWS_NOTIFICATIONS_INTERVAL = "pref_news_notifications_interval"
    const val PREFERENCE_NIGHT_MODE = "pref_theme"

    fun isHentaiAllowed(context: Context) = getDefaultSharedPreferences(context).getBoolean(PREFERENCE_HENTAI, false)

    fun getStartPage(context: Context) = MaterialDrawerHelper.DrawerItem
            .fromOrDefault(getDefaultSharedPreferences(context).getString(PREFERENCE_START_PAGE, "0").toLongOrNull())

    fun setHentaiAllowed(context: Context, isAllowed: Boolean) = getDefaultSharedPreferences(context).edit()
            .putBoolean(PREFERENCE_HENTAI, isAllowed).apply()

    @AppCompatDelegate.NightMode
    fun getNightMode(context: Context): Int {
        return when (getDefaultSharedPreferences(context).getString(PREFERENCE_NIGHT_MODE, "0")) {
            "0" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            "1" -> AppCompatDelegate.MODE_NIGHT_AUTO
            "2" -> AppCompatDelegate.MODE_NIGHT_YES
            "3" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> throw RuntimeException("Invalid value")
        }
    }
}
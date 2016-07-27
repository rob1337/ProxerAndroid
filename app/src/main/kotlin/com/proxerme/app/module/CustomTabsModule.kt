package com.proxerme.app.module

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import com.proxerme.app.R
import com.proxerme.app.customtabs.CustomTabActivityHelper
import com.proxerme.app.customtabs.CustomTabsHelper
import com.proxerme.app.customtabs.WebviewFallback

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
interface CustomTabsModule {

    val customTabActivityHelper: CustomTabActivityHelper

    fun setLikelyUrl(url: String) {
        customTabActivityHelper.mayLaunchUrl(Uri.parse(url), Bundle(), listOf())
    }

    fun showPage(url: String) {
        val self = this as Activity

        val customTabsIntent = CustomTabsIntent.Builder(customTabActivityHelper.session)
                .setToolbarColor(ContextCompat.getColor(self, R.color.colorPrimary))
                .setSecondaryToolbarColor(ContextCompat.getColor(self,
                        R.color.colorPrimaryDark))
                .enableUrlBarHiding()
                .setShowTitle(true)
                .build()

        CustomTabsHelper.addKeepAliveExtra(self, customTabsIntent.intent)
        CustomTabActivityHelper.openCustomTab(
                self, customTabsIntent, Uri.parse(url), WebviewFallback())
    }

}
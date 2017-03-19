package me.proxer.app.helper

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.crossfader.Crossfader
import com.mikepenz.crossfader.view.GmailStyleCrossFadeSlidingPaneLayout
import com.mikepenz.materialdrawer.*
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.proxerme.library.util.ProxerUrls
import me.proxer.app.R
import me.proxer.app.util.CrossfadeWrapper
import me.proxer.app.util.DeviceUtils

/**
 * @author Ruben Gees
 */
class MaterialDrawerHelper(context: Activity, toolbar: Toolbar, savedInstanceState: Bundle?,
                           private val itemClickCallback: (id: DrawerItem) -> Boolean = { false },
                           private val accountClickCallback: (id: AccountItem) -> Boolean = { false }) {

    companion object {
        private const val CROSSFADER_FIRST_DRAWER_DP: Float = 300f
        private const val CROSSFADER_SECOND_DRAWER_DP: Float = 72f
        private const val STATE_CURRENT_DRAWER_ITEM_ID = "material_drawer_helper_current_id"
    }

    private val header: AccountHeader
    private val drawer: Drawer

    private var miniDrawer: MiniDrawer?
    private var crossfader: Crossfader<*>?

    private var currentItem: DrawerItem? = null

    init {
        header = buildAccountHeader(context, savedInstanceState)
        drawer = buildDrawer(context, toolbar, header, savedInstanceState)

        val tabletDrawer = buildTabletDrawer(context, drawer, savedInstanceState)
        miniDrawer = tabletDrawer.first
        crossfader = tabletDrawer.second

        currentItem = DrawerItem.fromOrNull(savedInstanceState?.getLong(STATE_CURRENT_DRAWER_ITEM_ID))
    }

    fun onBackPressed(): Boolean {
        if (crossfader?.isCrossFaded() ?: false) {
            crossfader?.crossFade()

            return true
        } else if (isDrawerOpen()) {
            drawer.closeDrawer()

            return true
        } else {
            val context = when {
                drawer.drawerLayout != null -> drawer.drawerLayout.context
                crossfader != null -> crossfader?.getCrossFadeSlidingPaneLayout()?.context
                else -> null
            } ?: return false

            val startPage = PreferenceHelper.getStartPage(context)

            if (currentItem != startPage) {
                select(startPage)

                return true
            } else {
                return false
            }
        }
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putLong(STATE_CURRENT_DRAWER_ITEM_ID, currentItem?.id ?: DrawerItem.NEWS.id)

        header.saveInstanceState(outState)
        drawer.saveInstanceState(outState)
        crossfader?.saveInstanceState(outState)
    }

    fun isDrawerOpen(): Boolean {
        return drawer.isDrawerOpen
    }

    fun select(item: DrawerItem) {
        drawer.setSelection(item.id)
        miniDrawer?.setSelection(item.id)
    }

    fun refreshHeader(context: Activity) {
        header.profiles = generateAccountItems(context)
        drawer.recyclerView.adapter.notifyDataSetChanged()
        miniDrawer?.createItems()
    }

    private fun buildAccountHeader(context: Activity, savedInstanceState: Bundle?): AccountHeader {
        return AccountHeaderBuilder()
                .withActivity(context)
                .withCompactStyle(true)
                .withHeaderBackground(R.color.colorPrimary)
                .withOnAccountHeaderListener { view, profile, current ->
                    onAccountItemClick(view, profile, current)
                }
                .withSavedInstance(savedInstanceState)
                .withProfiles(generateAccountItems(context))
                .build()
    }

    private fun generateAccountItems(context: Activity): List<IProfile<*>> {
        val loginToken = StorageHelper.loginToken
        val user = StorageHelper.user

        when (loginToken) {
            null -> return arrayListOf(
                    ProfileDrawerItem()
                            .withName(context.getString(R.string.section_guest))
                            .withIcon(R.mipmap.ic_launcher)
                            .withSelectedTextColorRes(R.color.colorAccent)
                            .withIdentifier(AccountItem.GUEST.id),
                    ProfileSettingDrawerItem()
                            .withName(context.getString(R.string.section_login))
                            .withIcon(CommunityMaterial.Icon.cmd_account_key)
                            .withIconTinted(true)
                            .withIdentifier(AccountItem.LOGIN.id))
            else -> return arrayListOf(
                    ProfileDrawerItem()
                            .withName(user?.name)
                            .withEmail(context.getString(R.string.section_user_subtitle))
                            .withIcon(ProxerUrls.userImage(user?.image ?: "").toString())
                            .withSelectedTextColorRes(R.color.colorAccent)
                            .withIdentifier(AccountItem.USER.id),
                    ProfileSettingDrawerItem()
                            .withName(context.getString(R.string.section_ucp))
                            .withIcon(CommunityMaterial.Icon.cmd_account_key)
                            .withIconTinted(true)
                            .withIdentifier(AccountItem.UCP.id),
                    ProfileSettingDrawerItem()
                            .withName(context.getString(R.string.section_logout))
                            .withIcon(CommunityMaterial.Icon.cmd_account_remove)
                            .withIconTinted(true)
                            .withIdentifier(AccountItem.LOGOUT.id)
            )
        }
    }

    private fun buildDrawer(context: Activity, toolbar: Toolbar, accountHeader: AccountHeader,
                            savedInstanceState: Bundle?): Drawer {
        val builder = DrawerBuilder(context)
                .withToolbar(toolbar)
                .withAccountHeader(accountHeader)
                .withDrawerItems(generateDrawerItems())
                .withStickyDrawerItems(generateStickyDrawerItems())
                .withOnDrawerItemClickListener { view, id, item ->
                    onDrawerItemClick(view, id, item)
                }
                .withShowDrawerOnFirstLaunch(true)
                .withTranslucentStatusBar(true)
                .withGenerateMiniDrawer(DeviceUtils.isTablet(context))
                .withSavedInstance(savedInstanceState)

        return if (DeviceUtils.isTablet(context)) builder.buildView() else builder.build()
    }

    private fun buildTabletDrawer(context: Activity, drawer: Drawer, savedInstanceState: Bundle?):
            Pair<MiniDrawer?, Crossfader<*>?> {
        if (DeviceUtils.isTablet(context)) {
            val miniDrawer = drawer.miniDrawer.withIncludeSecondaryDrawerItems(true)
            val crossfader = buildCrossfader(context, drawer, miniDrawer, savedInstanceState)

            miniDrawer.withCrossFader(CrossfadeWrapper(crossfader))
            crossfader.getCrossFadeSlidingPaneLayout()?.setShadowResourceLeft(R.drawable.material_drawer_shadow_left)

            return miniDrawer to crossfader
        }

        return null to null
    }

    private fun buildCrossfader(context: Activity, drawer: Drawer, miniDrawer: MiniDrawer,
                                savedInstanceState: Bundle?): Crossfader<*> {
        return Crossfader<GmailStyleCrossFadeSlidingPaneLayout>()
                .withContent(context.findViewById(R.id.root))
                .withFirst(drawer.slider, DeviceUtils.convertDpToPx(context, CROSSFADER_FIRST_DRAWER_DP))
                .withSecond(miniDrawer.build(context), DeviceUtils.convertDpToPx(context, CROSSFADER_SECOND_DRAWER_DP))
                .withSavedInstance(savedInstanceState)
                .build()
    }

    private fun generateDrawerItems(): List<IDrawerItem<*, *>> {
        return arrayListOf(
                PrimaryDrawerItem()
                        .withName(R.string.section_news)
                        .withIcon(CommunityMaterial.Icon.cmd_newspaper)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withBadgeStyle(BadgeStyle()
                                .withColorRes(R.color.colorAccent)
                                .withTextColorRes(android.R.color.white))
                        .withIdentifier(DrawerItem.NEWS.id),
                PrimaryDrawerItem()
                        .withName(R.string.section_chat)
                        .withIcon(CommunityMaterial.Icon.cmd_message_text)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withBadgeStyle(BadgeStyle()
                                .withColorRes(R.color.colorAccent)
                                .withTextColorRes(android.R.color.white))
                        .withIdentifier(DrawerItem.CHAT.id),
                PrimaryDrawerItem()
                        .withName(R.string.section_bookmarks)
                        .withIcon(CommunityMaterial.Icon.cmd_bookmark)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withBadgeStyle(BadgeStyle()
                                .withColorRes(R.color.colorAccent)
                                .withTextColorRes(android.R.color.white))
                        .withIdentifier(DrawerItem.BOOKMARKS.id),
                PrimaryDrawerItem()
                        .withName(R.string.section_anime)
                        .withIcon(CommunityMaterial.Icon.cmd_television)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withBadgeStyle(BadgeStyle()
                                .withColorRes(R.color.colorAccent)
                                .withTextColorRes(android.R.color.white))
                        .withIdentifier(DrawerItem.ANIME.id),
                PrimaryDrawerItem()
                        .withName(R.string.section_manga)
                        .withIcon(CommunityMaterial.Icon.cmd_book_open_variant)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withBadgeStyle(BadgeStyle()
                                .withColorRes(R.color.colorAccent)
                                .withTextColorRes(android.R.color.white))
                        .withIdentifier(DrawerItem.MANGA.id)
        )
    }

    private fun generateStickyDrawerItems(): ArrayList<IDrawerItem<*, *>> {
        return arrayListOf(
                PrimaryDrawerItem()
                        .withName(R.string.section_info)
                        .withIcon(CommunityMaterial.Icon.cmd_information_outline)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withIdentifier(DrawerItem.INFO.id),
                PrimaryDrawerItem()
                        .withName(R.string.section_donate)
                        .withIcon(CommunityMaterial.Icon.cmd_gift)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withSelectable(false)
                        .withIdentifier(DrawerItem.DONATE.id),
                PrimaryDrawerItem()
                        .withName(R.string.section_settings)
                        .withIcon(CommunityMaterial.Icon.cmd_settings)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withIdentifier(DrawerItem.SETTINGS.id))
    }

    private fun getStickyItemIds(): Array<DrawerItem> {
        return generateStickyDrawerItems()
                .mapNotNull { DrawerItem.fromOrNull(it.identifier) }
                .toTypedArray()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onDrawerItemClick(view: View?, id: Int, item: IDrawerItem<*, *>): Boolean {
        if (item.identifier != currentItem?.id) {
            val newItem = DrawerItem.fromOrDefault(item.identifier)

            if (item.isSelectable) {
                currentItem = newItem

                if (miniDrawer != null && newItem in getStickyItemIds()) {
                    miniDrawer?.adapter?.deselect()
                }
            }

            return itemClickCallback.invoke(newItem)
        }

        return true
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onAccountItemClick(view: View?, profile: IProfile<*>, current: Boolean): Boolean {
        return accountClickCallback.invoke(AccountItem.fromOrDefault(profile.identifier))
    }

    enum class DrawerItem(val id: Long) {
        NEWS(0L),
        CHAT(1L),
        BOOKMARKS(2L),
        ANIME(3L),
        MANGA(4L),
        INFO(10L),
        DONATE(11L),
        SETTINGS(12L);

        companion object {
            fun fromOrNull(id: Long?) = values().firstOrNull { it.id == id }
            fun fromOrDefault(id: Long?) = fromOrNull(id) ?: NEWS
        }
    }

    enum class AccountItem(val id: Long) {
        GUEST(100L),
        LOGIN(101L),
        USER(102L),
        LOGOUT(103L),
        UCP(104L);

        companion object {
            fun fromOrNull(id: Long?) = values().firstOrNull { it.id == id }
            fun fromOrDefault(id: Long?) = fromOrNull(id) ?: USER
        }
    }
}
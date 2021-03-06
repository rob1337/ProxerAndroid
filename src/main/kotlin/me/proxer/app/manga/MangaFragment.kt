package me.proxer.app.manga

import android.arch.lifecycle.Observer
import android.os.Build
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import android.support.design.widget.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.github.rubensousa.gravitysnaphelper.GravityPagerSnapHelper
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import io.reactivex.Observable
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter.ContainerPositionResolver
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.info.translatorgroup.TranslatorGroupActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.ui.view.MediaControlView
import me.proxer.app.ui.view.MediaControlView.SimpleTranslatorGroup
import me.proxer.app.ui.view.MediaControlView.Uploader
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.convertToDateTime
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.enums.Language
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MangaFragment : BaseContentFragment<MangaChapterInfo>() {

    companion object {
        fun newInstance() = MangaFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { MangaViewModelProvider.get(this, id, language, episode) }

    override val hostingActivity: MangaActivity
        get() = activity as MangaActivity

    private val id: String
        get() = hostingActivity.id

    private var episode: Int
        get() = hostingActivity.episode
        set(value) {
            hostingActivity.episode = value

            viewModel.setEpisode(value)
        }

    private val language: Language
        get() = hostingActivity.language

    private var chapterTitle: String?
        get() = hostingActivity.chapterTitle
        set(value) {
            hostingActivity.chapterTitle = value
        }

    private var name: String?
        get() = hostingActivity.name
        set(value) {
            hostingActivity.name = value
        }

    private var episodeAmount: Int?
        get() = hostingActivity.episodeAmount
        set(value) {
            hostingActivity.episodeAmount = value
        }

    private var isVertical by Delegates.notNull<Boolean>()

    private val mediaControlTextResolver = object : MediaControlView.TextResourceResolver {
        override fun next() = requireContext().getString(R.string.fragment_manga_next_chapter)
        override fun previous() = requireContext().getString(R.string.fragment_manga_previous_chapter)
        override fun bookmarkThis() = requireContext().getString(R.string.fragment_manga_bookmark_this_chapter)
        override fun bookmarkNext() = requireContext().getString(R.string.fragment_manga_bookmark_next_chapter)
    }

    private var innerAdapter by Delegates.notNull<MangaAdapter>()
    private var adapter by Delegates.notNull<EasyHeaderFooterAdapter>()

    private var header by Delegates.notNull<MediaControlView>()
    private var footer by Delegates.notNull<MediaControlView>()

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val activityRoot by unsafeLazy { requireActivity().findViewById<ViewGroup>(R.id.root) }
    private val toolbar by unsafeLazy { requireActivity().findViewById<Toolbar>(R.id.toolbar) }
    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isVertical = PreferenceHelper.isVerticalReaderEnabled(requireContext())

        innerAdapter = MangaAdapter(isVertical)
        adapter = EasyHeaderFooterAdapter(innerAdapter)

        innerAdapter.positionResolver = ContainerPositionResolver(adapter)

        innerAdapter.clickSubject
            .autoDispose(this)
            .subscribeAndLogErrors { recyclerView.smoothScrollToPosition(it + 2) }

        viewModel.setEpisode(episode, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val horizontalMargin = DeviceUtils.getHorizontalMargin(requireContext(), true)
        val verticalMargin = DeviceUtils.getVerticalMargin(requireContext(), true)

        header = (inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView).apply {
            textResolver = mediaControlTextResolver

            (layoutParams as ViewGroup.MarginLayoutParams).setMargins(horizontalMargin, verticalMargin,
                horizontalMargin, verticalMargin)
        }

        footer = (inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView).apply {
            textResolver = mediaControlTextResolver

            (layoutParams as ViewGroup.MarginLayoutParams).setMargins(horizontalMargin, verticalMargin,
                horizontalMargin, verticalMargin)
        }

        Observable.merge(header.uploaderClickSubject, footer.uploaderClickSubject)
            .autoDispose(this)
            .subscribe { ProfileActivity.navigateTo(requireActivity(), it.id, it.name) }

        Observable.merge(header.translatorGroupClickSubject, footer.translatorGroupClickSubject)
            .autoDispose(this)
            .subscribe { TranslatorGroupActivity.navigateTo(requireActivity(), it.id, it.name) }

        Observable.merge(header.episodeSwitchSubject, footer.episodeSwitchSubject)
            .autoDispose(this)
            .subscribe { episode = it }

        Observable.merge(header.bookmarkSetSubject, footer.bookmarkSetSubject)
            .autoDispose(this)
            .subscribe { viewModel.bookmark(it) }

        Observable.merge(header.finishClickSubject, footer.finishClickSubject)
            .autoDispose(this)
            .subscribe { viewModel.markAsFinished() }

        return inflater.inflate(R.layout.fragment_manga, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            requireActivity().window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == View.VISIBLE) {
                    (activity as AppCompatActivity).supportActionBar?.show()
                } else {
                    (activity as AppCompatActivity).supportActionBar?.hide()
                }
            }
        }

        innerAdapter.glide = GlideApp.with(this)

        viewModel.bookmarkData.observe(this, Observer {
            it?.let {
                snackbar(activityRoot, R.string.fragment_set_user_info_success)
            }
        })

        viewModel.bookmarkError.observe(this, Observer {
            it?.let {
                multilineSnackbar(root, getString(R.string.error_set_user_info, getString(it.message)),
                    Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction?.toClickListener(hostingActivity))
            }
        })

        if (isVertical) {
            recyclerView.layoutManager = LinearLayoutManager(context)
        } else {
            recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            GravityPagerSnapHelper(Gravity.END).attachToRecyclerView(recyclerView)
        }

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun showData(data: MangaChapterInfo) {
        super.showData(data)

        if (isVertical) {
            (toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
                scrollFlags = SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS
            }
        }

        chapterTitle = data.chapter.title
        episodeAmount = data.episodeAmount
        name = data.name

        showHeaderAndFooter(data)

        innerAdapter.server = data.chapter.server
        innerAdapter.entryId = data.chapter.entryId
        innerAdapter.id = data.chapter.id

        innerAdapter.swapDataAndNotifyWithDiffing(data.chapter.pages)

        requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
    }

    override fun hideData() {
        if (isVertical) {
            (toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
                scrollFlags = SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS
            }
        }

        innerAdapter.swapDataAndNotifyWithDiffing(emptyList())
        adapter.header = null
        adapter.footer = null

        super.hideData()
    }

    override fun showError(action: ErrorUtils.ErrorAction) {
        super.showError(action)

        chapterTitle = null

        action.partialData?.let {
            if (it is EntryCore) {
                episodeAmount = it.episodeAmount
                name = it.name

                header.setEpisodeInfo(it.episodeAmount, episode)
                header.setTranslatorGroup(null)
                header.setUploader(null)
                header.setDateTime(null)

                adapter.header = header
            }
        }

        if (adapter.header != null) {
            if (!isVertical) {
                header.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                footer.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }

            contentContainer.visibility = View.VISIBLE
            errorContainer.visibility = View.INVISIBLE

            errorInnerContainer.post {
                val newCenter = root.height / 2f + header.height / 2f
                val containerCenterCorrection = errorInnerContainer.height / 2f

                errorInnerContainer.y = newCenter - containerCenterCorrection
                errorContainer.visibility = View.VISIBLE
            }
        } else {
            errorContainer.translationY = 0f
        }
    }

    private fun showHeaderAndFooter(data: MangaChapterInfo) {
        if (!isVertical) {
            header.layoutParams.height = MATCH_PARENT
            footer.layoutParams.height = MATCH_PARENT
        }

        header.setEpisodeInfo(data.episodeAmount, episode)
        header.setDateTime(data.chapter.date.convertToDateTime())
        header.setUploader(Uploader(data.chapter.uploaderId, data.chapter.uploaderName))

        footer.setEpisodeInfo(data.episodeAmount, episode)

        data.chapter.scanGroupId?.let { id ->
            data.chapter.scanGroupName?.let { name ->
                header.setTranslatorGroup(SimpleTranslatorGroup(id, name))
            }
        }

        adapter.header = header
        adapter.footer = footer
    }
}

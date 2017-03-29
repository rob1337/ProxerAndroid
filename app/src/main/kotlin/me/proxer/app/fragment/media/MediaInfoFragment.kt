package me.proxer.app.fragment.media

import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.flexbox.FlexboxLayout
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.operation.ValidatingTask
import com.rubengees.ktask.util.TaskBuilder
import fisk.chipcloud.ChipCloud
import fisk.chipcloud.ChipCloudConfig
import me.proxer.app.R
import me.proxer.app.activity.MainActivity
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.EnumMapper
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.snackbar
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.info.Entry
import me.proxer.library.enums.Category
import me.proxer.library.enums.SynonymType
import me.proxer.library.util.ProxerUrls
import me.proxer.library.util.ProxerUtils
import org.jetbrains.anko.childrenSequence

/**
 * @author Ruben Gees
 */
class MediaInfoFragment : LoadingFragment<ProxerCall<Entry>, Entry>() {

    companion object {
        private const val SHOW_UNRATED_TAGS_ARGUMENT = "show_unrated_tags"
        private const val SHOW_SPOILER_TAGS_ARGUMENT = "show_spoiler_tags"

        fun newInstance(): MediaInfoFragment {
            return MediaInfoFragment().apply {
                arguments = Bundle()
            }
        }
    }

    private val mediaActivity
        get() = activity as me.proxer.app.activity.MediaActivity

    private val id: String
        get() = mediaActivity.id

    private var name: String?
        get() = mediaActivity.name
        set(value) {
            mediaActivity.name = value
        }

    private var category: Category
        get() = mediaActivity.category
        set(value) {
            mediaActivity.category = value
        }

    private var showUnratedTags: Boolean
        get() = arguments.getBoolean(SHOW_UNRATED_TAGS_ARGUMENT, false)
        set(value) = arguments.putBoolean(SHOW_UNRATED_TAGS_ARGUMENT, value)

    private var showSpoilerTags: Boolean
        get() = arguments.getBoolean(SHOW_SPOILER_TAGS_ARGUMENT, false)
        set(value) = arguments.putBoolean(SHOW_SPOILER_TAGS_ARGUMENT, value)

    private lateinit var userInfoTask: ValidatingTask<ProxerCall<Void?>, Void?>

    private val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
    private val rating: RatingBar by bindView(R.id.rating)
    private val ratingAmount: TextView by bindView(R.id.ratingAmount)

    private val infoTable: TableLayout by bindView(R.id.infoTable)

    private val genresTitle: TextView by bindView(R.id.genresTitle)
    private val genres: FlexboxLayout by bindView(R.id.genres)
    private val tagsTitle: TextView by bindView(R.id.tagsTitle)
    private val tags: FlexboxLayout by bindView(R.id.tags)
    private val unratedTags: Button by bindView(R.id.unratedTags)
    private val spoilerTags: Button by bindView(R.id.spoilerTags)
    private val fskConstraintsTitle: TextView  by bindView(R.id.fskConstraintsTitle)
    private val fskConstraints: FlexboxLayout by bindView(R.id.fskConstraints)
    private val translatorGroupsTitle: TextView by bindView(R.id.translatorGroupsTitle)
    private val translatorGroups: FlexboxLayout by bindView(R.id.translatorGroups)
    private val industriesTitle: TextView by bindView(R.id.industriesTitle)
    private val industries: FlexboxLayout by bindView(R.id.industries)

    private val noteContainer: ViewGroup by bindView(R.id.noteContainer)
    private val note: ImageView by bindView(R.id.note)
    private val favorContainer: ViewGroup by bindView(R.id.favorContainer)
    private val favor: ImageView by bindView(R.id.favor)
    private val finishContainer: ViewGroup by bindView(R.id.finishContainer)
    private val finish: ImageView by bindView(R.id.finish)

    private val description: TextView by bindView(R.id.description)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userInfoTask = TaskBuilder.asyncProxerTask<Void?>()
                .bindToLifecycle(this)
                .validateBefore {
                    Validators.validateLogin()
                }
                .onSuccess {
                    snackbar(root, R.string.fragment_media_info_set_user_info_success)
                }
                .onError {
                    ErrorUtils.handle(context as MainActivity, it).let {
                        multilineSnackbar(root,
                                getString(R.string.fragment_media_info_set_user_info_error, getString(it.message)),
                                Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction)
                    }
                }
                .build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media_info, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        note.setImageDrawable(IconicsDrawable(context, CommunityMaterial.Icon.cmd_clock)
                .sizeDp(24)
                .colorRes(R.color.icon))

        favor.setImageDrawable(IconicsDrawable(context, CommunityMaterial.Icon.cmd_star)
                .sizeDp(24)
                .colorRes(R.color.icon))

        finish.setImageDrawable(IconicsDrawable(context, CommunityMaterial.Icon.cmd_check)
                .sizeDp(24)
                .colorRes(R.color.icon))

        noteContainer.setOnClickListener {
            userInfoTask.forceExecute(api.info().note(id).build())
        }

        favorContainer.setOnClickListener {
            userInfoTask.forceExecute(api.info().markAsFavorite(id).build())
        }

        finishContainer.setOnClickListener {
            userInfoTask.forceExecute(api.info().markAsFinished(id).build())
        }

        unratedTags.setOnClickListener {
            showUnratedTags = !showUnratedTags

            tags.removeAllViews()
            state.data?.let { bindTags(it) }
        }

        spoilerTags.setOnClickListener {
            showSpoilerTags = !showSpoilerTags

            tags.removeAllViews()
            state.data?.let { bindTags(it) }
        }

        updateUnratedButton()
        updateSpoilerButton()
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<Entry>().build()
    override fun constructInput() = api.info().entry(id).build()

    override fun onSuccess(result: Entry) {
        super.onSuccess(result)

        name = result.name
        category = result.category

        bindRating(result)
        bindSynonyms(result)
        bindSeasons(result)
        bindStatus(result)
        bindLicense(result)
        bindGenres(result)
        bindTags(result)
        bindFskConstraints(result)
        bindTranslatorGroups(result)
        bindIndustries(result)

        description.text = result.description
    }

    private fun bindRating(result: Entry) {
        if (result.rating > 0) {
            ratingContainer.visibility = View.VISIBLE
            rating.rating = result.rating / 2.0f
            ratingAmount.visibility = View.VISIBLE
            ratingAmount.text = getString(R.string.fragment_media_info_rate_count, result.ratingAmount)
        } else {
            ratingContainer.visibility = View.GONE
            ratingAmount.visibility = View.GONE
        }
    }

    private fun bindSynonyms(result: Entry) {
        result.synonyms.forEach {
            val title = context.getString(when (it.type) {
                SynonymType.ORIGINAL -> R.string.fragment_media_info_original_title
                SynonymType.ENGLISH -> R.string.fragment_media_info_english_title
                SynonymType.GERMAN -> R.string.fragment_media_info_german_title
                SynonymType.JAPANESE -> R.string.fragment_media_info_japanese_title
                SynonymType.ORIGINAL_ALTERNATIVE -> R.string.fragment_media_info_alternative_title
            })

            infoTable.addView(constructInfoTableRow(title, it.name))
        }
    }

    private fun bindSeasons(result: Entry) {
        val seasons = result.seasons

        if (seasons.isNotEmpty()) {
            val tableRow = LayoutInflater.from(context).inflate(R.layout.layout_media_info_seasons_row, infoTable, false)
            val seasonStartView = tableRow.findViewById(R.id.seasonStart) as TextView
            val seasonEndView = tableRow.findViewById(R.id.seasonEnd) as TextView

            seasonStartView.text = EnumMapper.seasonStartToString(context, seasons[0])

            if (seasons.size >= 2) {
                seasonEndView.text = EnumMapper.seasonEndToString(context, seasons[1])
            } else {
                seasonEndView.visibility = View.GONE
            }

            infoTable.addView(tableRow)
        }
    }

    private fun bindStatus(result: Entry) {
        infoTable.addView(constructInfoTableRow(context.getString(R.string.fragment_media_info_status_title),
                EnumMapper.mediaStateToString(context, result.state)))
    }

    private fun bindLicense(result: Entry) {
        infoTable.addView(constructInfoTableRow(context.getString(R.string.fragment_media_info_license_title),
                EnumMapper.licenseToString(context, result.license)))
    }

    private fun constructInfoTableRow(title: String, content: String): View {
        val tableRow = LayoutInflater.from(context).inflate(R.layout.layout_media_info_row, infoTable, false)
        val titleView = tableRow.findViewById(R.id.title) as TextView
        val contentView = tableRow.findViewById(R.id.content) as TextView

        titleView.text = title
        contentView.text = content

        return tableRow
    }

    private fun bindGenres(result: Entry) {
        if (result.genres.isEmpty()) {
            genresTitle.visibility = View.GONE
            genres.visibility = View.GONE

            return
        }

        bindChips(genres, result.genres.toList(), mapFunction = {
            ProxerUtils.getApiEnumName(it)
        }, onClick = {
            showPage(ProxerUrls.wikiWeb(ProxerUtils.getApiEnumName(it)))
        })
    }

    private fun bindTags(result: Entry) {
        if (result.tags.isEmpty()) {
            tagsTitle.visibility = View.GONE
            unratedTags.visibility = View.GONE
            spoilerTags.visibility = View.GONE
        } else {
            updateSpoilerButton()
            updateUnratedButton()
        }

        val filteredTags = result.tags.filter {
            when (it.isRated) {
                true -> when (it.isSpoiler) {
                    true -> showSpoilerTags
                    false -> true
                }
                false -> when (showUnratedTags) {
                    true -> when (it.isSpoiler) {
                        true -> showSpoilerTags
                        false -> true
                    }
                    false -> false
                }
            }
        }

        bindChips(tags, filteredTags, mapFunction = {
            it.name
        }, onClick = {
            multilineSnackbar(root, it.description)
        })
    }

    private fun updateUnratedButton() {
        unratedTags.text = getString(when (showUnratedTags) {
            true -> R.string.fragment_media_info_tags_unrated_hide
            false -> R.string.fragment_media_info_tags_unrated_show
        })
    }

    private fun updateSpoilerButton() {
        spoilerTags.text = getString(when (showSpoilerTags) {
            true -> R.string.fragment_media_info_tags_spoiler_hide
            false -> R.string.fragment_media_info_tags_spoiler_show
        })
    }

    private fun bindFskConstraints(result: Entry) {
        if (result.fskConstraints.isEmpty()) {
            fskConstraintsTitle.visibility = View.GONE
            fskConstraints.visibility = View.GONE
        } else {
            result.fskConstraints.forEach { constraint ->
                val image = LayoutInflater.from(context)
                        .inflate(R.layout.layout_image, fskConstraints, false) as ImageView

                image.setImageDrawable(EnumMapper.fskConstraintToDrawable(context, constraint))
                image.setOnClickListener {
                    multilineSnackbar(root, EnumMapper.fskConstraintToString(context, constraint))
                }

                fskConstraints.addView(image)
            }
        }
    }

    private fun bindTranslatorGroups(result: Entry) {
        if (result.translatorGroups.isEmpty()) {
            translatorGroupsTitle.visibility = View.GONE
            translatorGroups.visibility = View.GONE
        } else {
            bindChips(translatorGroups, result.translatorGroups, mapFunction = {
                it.name
            }, onClick = {
                // TranslatorGroupActivity.navigateTo(activity, translatorGroup.id, translatorGroup.name)
            })
        }
    }

    private fun bindIndustries(result: Entry) {
        if (result.industries.isEmpty()) {
            industriesTitle.visibility = View.GONE
            industries.visibility = View.GONE
        } else {
            bindChips(industries, result.industries, mapFunction = {
                "${it.name} (${ProxerUtils.getApiEnumName(it.type).replace("_", " ").split(" ")
                        .map(String::capitalize).joinToString(separator = " ")})"
            }, onClick = {
                // IndustryActivity.navigateTo(activity, industry.id, industry.name)
            })
        }
    }

    private fun <T> bindChips(layout: FlexboxLayout, items: List<T>, mapFunction: (T) -> String = { it.toString() },
                              onClick: ((T) -> Unit)? = null) {
        ChipCloud(context, layout, ChipCloudConfig()
                .uncheckedChipColor(ContextCompat.getColor(context, R.color.colorAccent))
                .uncheckedTextColor(ContextCompat.getColor(context, android.R.color.white))
                .selectMode(ChipCloud.SelectMode.none)
                .typeface(Typeface.DEFAULT_BOLD)
                .useInsetPadding(true))
                .apply { addChips(items.map(mapFunction)) }
                .also {
                    if (onClick != null) {
                        layout.childrenSequence().forEachIndexed { index, view ->
                            view.setOnClickListener {
                                onClick.invoke(items[index])
                            }
                        }
                    }
                }
    }
}
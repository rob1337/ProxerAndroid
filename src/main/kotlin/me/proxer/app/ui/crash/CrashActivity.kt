package me.proxer.app.ui.crash

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.jakewharton.rxbinding2.view.clicks
import com.klinker.android.link_builder.TouchableMovementMethod
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.chat.Participant
import me.proxer.app.chat.create.CreateChatActivity
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.setIconicsImage

/**
 * @author Ruben Gees
 */
class CrashActivity : BaseActivity() {

    private companion object {
        private const val DEVELOPER_PROXER_NAME = "RubyGee"
    }

    private val config: CaocConfig
        get() = CustomActivityOnCrash.getConfigFromIntent(intent) ?: CaocConfig()

    private val errorDetails: String
        get() = CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, intent)

    private val stacktrace: String
        get() = CustomActivityOnCrash.getStackTraceFromIntent(intent) ?: ""

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val image: ImageView by bindView(R.id.image)
    private val text: TextView by bindView(R.id.text)
    private val report: Button by bindView(R.id.report)
    private val restart: Button by bindView(R.id.restart)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_crash)
        setSupportActionBar(toolbar)
        title = getString(R.string.section_crash)

        image.setIconicsImage(CommunityMaterial.Icon.cmd_bug, 200, 12, R.color.primary)

        report.clicks()
            .autoDispose(this)
            .subscribe { CrashDialog.show(this, errorDetails, stacktrace) }

        restart.clicks()
            .autoDispose(this)
            .subscribe { CustomActivityOnCrash.restartApplication(this, config) }

        text.movementMethod = TouchableMovementMethod.instance
        text.text = Utils.buildClickableText(this, getString(R.string.activity_crash_text),
            onMentionsClickListener = {
                CustomActivityOnCrash.restartApplicationWithIntent(this,
                    CreateChatActivity.getIntent(this, false, Participant(DEVELOPER_PROXER_NAME)), config)
            })
    }
}

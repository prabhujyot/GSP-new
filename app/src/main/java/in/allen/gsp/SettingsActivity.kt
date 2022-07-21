package `in`.allen.gsp

import `in`.allen.gsp.utils.AppPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.firebase.messaging.FirebaseMessaging
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class SettingsActivity : AppCompatActivity(), DIAware {

    override val di: DI by lazy { (applicationContext as DIAware).di }
    private val preferences: AppPreferences by instance()


    private var is_admin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        window.statusBarColor = ContextCompat.getColor(this,R.color.blue)

        is_admin = intent.getBooleanExtra("is_admin",false)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment(is_admin,preferences))
                .commit()
        }

        supportActionBar?.title = getString(R.string.lblSettings)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.arrow_previous)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.gradiant_blue))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    class SettingsFragment(private val is_admin: Boolean, val preferences: AppPreferences) : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val pref: SwitchPreferenceCompat? = preferenceManager.findPreference("previewMode")
            pref?.isVisible = is_admin

            val pref2: SwitchPreferenceCompat? =
                preferenceManager.findPreference("appNotification")
            pref2?.isChecked = preferences.getPref("Notification").equals("true",true)
            pref2?.setOnPreferenceChangeListener { preference, newValue ->
                if (preference is SwitchPreferenceCompat) {
                    val value = newValue as Boolean
                    if (value) {
                        FirebaseMessaging.getInstance().subscribeToTopic("Notification")
                        preferences.setPref("Notification","true")
                    } else {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("Notification")
                        preferences.setPref("Notification","false")
                    }
                }
                true
            }
        }


    }
}
package `in`.allen.gsp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

class SettingsActivity : AppCompatActivity() {
    private var is_admin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        window.statusBarColor = ContextCompat.getColor(this,R.color.blue)

        is_admin = intent.getBooleanExtra("is_admin",false)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment(is_admin))
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

    class SettingsFragment(private val is_admin: Boolean) : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val pref: SwitchPreferenceCompat? = preferenceManager.findPreference("previewMode")
            pref?.isVisible = is_admin
        }
    }
}
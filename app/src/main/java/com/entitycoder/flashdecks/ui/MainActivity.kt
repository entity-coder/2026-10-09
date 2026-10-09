package com.entitycoder.flashdecks.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.entitycoder.flashdecks.R
import com.entitycoder.flashdecks.ui.decks.DeckListFragment
import com.entitycoder.flashdecks.ui.theme.ThemeManager

/**
 * Single-activity host. Navigation is done manually: fragments are swapped
 * into R.id.fragment_container with showFragment(), pushing onto the fragment
 * back stack so the system Back button (and the toolbar Up button) work
 * without the Navigation Component.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            showFragment(DeckListFragment(), addToBackStack = false)
        }

        // Show the Up button only when there is somewhere to go back to.
        supportFragmentManager.addOnBackStackChangedListener { updateUpButton() }
        updateUpButton()
    }

    /**
     * Replaces the current fragment. Callers that represent a "deeper" screen
     * (detail, quiz) pass addToBackStack = true; the root list does not.
     */
    fun showFragment(fragment: Fragment, tag: String? = null, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
        if (addToBackStack) {
            transaction.addToBackStack(tag)
        }
        transaction.commit()
    }

    private fun updateUpButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(
            supportFragmentManager.backStackEntryCount > 0
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Tick the currently active theme mode.
        when (ThemeManager.getThemeMode(this)) {
            ThemeManager.MODE_LIGHT -> menu.findItem(R.id.action_theme_light)?.isChecked = true
            ThemeManager.MODE_DARK -> menu.findItem(R.id.action_theme_dark)?.isChecked = true
            else -> menu.findItem(R.id.action_theme_system)?.isChecked = true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // AppCompatDelegate recreates the activity, so the ticked item refreshes.
        when (item.itemId) {
            R.id.action_theme_system -> {
                ThemeManager.setThemeMode(this, ThemeManager.MODE_SYSTEM)
                return true
            }
            R.id.action_theme_light -> {
                ThemeManager.setThemeMode(this, ThemeManager.MODE_LIGHT)
                return true
            }
            R.id.action_theme_dark -> {
                ThemeManager.setThemeMode(this, ThemeManager.MODE_DARK)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

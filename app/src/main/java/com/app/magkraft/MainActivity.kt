package com.app.magkraft

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.app.magkraft.ui.fragments.EmployeeFragment
import com.app.magkraft.ui.fragments.GroupFragment
import com.app.magkraft.ui.fragments.HomeFragment
import com.app.magkraft.ui.fragments.LocationFragment
import com.app.magkraft.ui.fragments.ReportFragment
import com.app.magkraft.ui.fragments.SetLocationFragment
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val toolbarMain = findViewById<Toolbar>(R.id.toolbarMain)


        setSupportActionBar(toolbarMain)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = ContextCompat.getColor(
            this,
            R.color.mid_color
        )
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Default screen
        loadFragment(HomeFragment())
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_group -> loadFragment(GroupFragment())
            R.id.menu_location -> loadFragment(LocationFragment())
            R.id.menu_set_location -> loadFragment(SetLocationFragment())
            R.id.menu_employee -> loadFragment(EmployeeFragment())
            R.id.menu_report -> loadFragment(ReportFragment())
        }
        return true
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
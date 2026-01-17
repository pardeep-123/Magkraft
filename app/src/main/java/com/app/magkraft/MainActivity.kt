package com.app.magkraft

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.Toast
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
        toolbarMain.overflowIcon =
            ContextCompat.getDrawable(this, R.drawable.more)?.apply {
                setTint(ContextCompat.getColor(this@MainActivity, android.R.color.white))
            }

        setSupportActionBar(toolbarMain)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = ContextCompat.getColor(
            this,
            R.color.mid_color
        )
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setupLoader()
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

    private var loaderDialog: Dialog? = null

    private fun setupLoader() {
        loaderDialog = Dialog(this)
        loaderDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        loaderDialog?.setContentView(R.layout.dialog_loader)
        loaderDialog?.setCancelable(false)
        loaderDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun showLoader() {
        if (loaderDialog?.isShowing == false) {
            loaderDialog?.show()
        }
    }

    fun hideLoader() {
        if (loaderDialog?.isShowing == true) {
            loaderDialog?.dismiss()
        }
    }

    fun showToast(ctx: Context, msg:String){
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    }
}
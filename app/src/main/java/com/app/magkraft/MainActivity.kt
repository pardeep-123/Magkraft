package com.app.magkraft

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.app.magkraft.ui.AttendanceActivity
import com.app.magkraft.ui.fragments.EmployeeFragment
import com.app.magkraft.ui.fragments.GroupFragment
import com.app.magkraft.ui.fragments.HomeFragment
import com.app.magkraft.ui.fragments.LocationFragment
import com.app.magkraft.ui.fragments.ManualAttendanceFragment
import com.app.magkraft.ui.fragments.ReportFragment
import com.app.magkraft.ui.fragments.SetLocationFragment
import com.app.magkraft.utils.AuthPref
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {
    var authPref: AuthPref? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        authPref = AuthPref(this)
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

        loadHomeFragment()
        onBackPressedDispatcher.addCallback(this, object :
            OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                val fm = supportFragmentManager

                if (fm.backStackEntryCount > 0) {
                    // Clear all fragments and go to Home
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    loadHomeFragment()
                } else {
                    // We are already on HomeFragment
                    finish()
                }
            }

        }
        )
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (authPref?.get("userType") == "1") {
            menuInflater.inflate(R.menu.menu_dashboard, menu)
        } else if (authPref?.get("userType") == "2") {
            menuInflater.inflate(R.menu.menu_dashboard_user, menu)
        }

        else {
            menuInflater.inflate(R.menu.menu_supervisor, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_group -> loadFragment(GroupFragment(), "GROUP")
            R.id.menu_location -> loadFragment(LocationFragment(), "Location")
            R.id.menu_set_location -> loadFragment(SetLocationFragment(), "SetLocation")
            R.id.menu_employee -> loadFragment(EmployeeFragment(), "Employee")
            R.id.menu_report -> loadFragment(ReportFragment(), "Reports")
            R.id.mark_attendance -> loadFragment(ManualAttendanceFragment(), "Manual Attendance")
            R.id.menu_logout -> showLogoutDialog(this)
        }
        return true
    }

    private fun loadHomeFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment(), "HOME")
            .commit()
    }

    private fun loadFragment(fragment: Fragment, tag: String) {

        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer)

        // Prevent reloading same fragment
        if (currentFragment?.tag == tag) return

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .addToBackStack(tag)
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

    fun showToast(ctx: Context, msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    }

    fun showLogoutDialog(
        context: Context
    ) {
        AlertDialog.Builder(context)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setCancelable(false)
            .setPositiveButton("Logout") { dialog, _ ->
                dialog.dismiss()
                // Clear token & user data
                authPref?.logout()
                finish()
                // Navigate to Login
//                val intent = Intent(this, AttendanceActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}
package com.app.magkraft.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.magkraft.R
import org.json.JSONObject
import retrofit2.Response

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupLoader()
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

     fun getErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            val jsonObject = JSONObject(errorBody ?: "")
            jsonObject.optString("error", "Something went wrong")
        } catch (e: Exception) {
            "Something went wrong"
        }
    }


}

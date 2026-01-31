package com.app.magkraft.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.magkraft.MainActivity
import com.app.magkraft.R
import com.app.magkraft.model.LoginSignupModel
import com.app.magkraft.network.ApiClient
import com.app.magkraft.utils.AuthPref
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : BaseActivity() {

    private lateinit var loginBtn: AppCompatButton
    private lateinit var emailET: TextInputEditText
    private lateinit var passwordET: TextInputEditText

    var authPref: AuthPref? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loginBtn = findViewById(R.id.loginBT)
        emailET = findViewById(R.id.emailET)
        passwordET = findViewById(R.id.passwordET)
        emailET.setText("Admin")
        passwordET.setText("Admin@409")
        loginBtn.setOnClickListener {

            if (emailET.text.toString().isEmpty() && passwordET.text.toString().isEmpty()) {
                showToast(this@LoginActivity, "enter credentials")
            } else {
                login(emailET.text.toString(), passwordET.text.toString())
            }

        }
        authPref = AuthPref(this)
    }


    private fun login(email: String, password: String) {

        showLoader()

        val call = ApiClient.apiService.loginUser(email, password)

        call.enqueue(object : Callback<LoginSignupModel> {

            override fun onResponse(
                call: Call<LoginSignupModel>,
                response: Response<LoginSignupModel>
            ) {
                hideLoader()

                if (response.isSuccessful && response.body() != null) {

                    val json = response.body()!!
                    Log.d("LOGIN_SUCCESS", json.toString())

                    if (response.body()?.status == "1") {
                        authPref?.saveToken(response.body()?.token.toString())
                        authPref?.put("userType", response.body()?.data?.UserType.toString())
                        authPref?.put("fullName", response.body()?.data?.FullName.toString())
                        authPref?.put("groupId", response.body()?.data?.GroupId.toString())

                        startActivity(
                            Intent(this@LoginActivity, MainActivity::class.java)
                        )
                        finish()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            response.body()?.error.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {
                    val errorMessage = getErrorMessage(response)
                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginSignupModel>, t: Throwable) {
                hideLoader()
                Toast.makeText(this@LoginActivity, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

}
package com.example.officeapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.confidential_screen.*
import kotlinx.android.synthetic.main.first_login.*

class MainActivity : AppCompatActivity() {

    var username=""
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        if(auth.currentUser!=null)
            username= ""+auth.currentUser!!.displayName

        if(username=="")
        {
            LoginUI()
        }
        else
        {
            MainUI(auth.currentUser)
        }
    }

    private fun loginActivity()
    {

        if(!validateForm()){
            return
        }

        val username=username_field.text.toString().trim()+"@idrbt.ac.in"
        val pass=password_field.text.toString()

        auth.signInWithEmailAndPassword(username,pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    MainUI(auth.currentUser)

                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(baseContext, "Authentication failed.",Toast.LENGTH_SHORT).show()
                }

            }
    }

    private fun validateForm(): Boolean {
        var valid = true


        val email = username_field.text.toString()
        if (TextUtils.isEmpty(email)) {
            username_field.error= "Required."
            valid = false
        } else {
            username_field.error = null
        }

        val password = password_field.text.toString()
        if (TextUtils.isEmpty(password)) {
            password_field.error = "Required."
            valid = false
        } else {
            password_field.error = null
        }

        return valid
    }

    private fun logoutActivity() {
        FirebaseAuth.getInstance().signOut()
        username=""
        auth= FirebaseAuth.getInstance()
        setContentView(R.layout.first_login)
    }

    private fun LoginUI()
    {
        setContentView(R.layout.first_login)
        login_button.setOnClickListener { loginActivity() }
    }
    private fun MainUI(usr: FirebaseUser?)
    {
        if (usr != null) {
            setContentView(R.layout.activity_main)
            logout_button.setOnClickListener { logoutActivity() }
            welcomeUser.text="Welcome "+ usr.email!!.removeSuffix("@idrbt.ac.in")
            restrictedsec_button.setOnClickListener { checkLocationLogin() }
        }
    }

    private fun checkLocationLogin() {
        setContentView(R.layout.confidential_screen)
        back_button.setOnClickListener { MainUI(auth.currentUser) }
        logout_res.setOnClickListener { logoutActivity() }
    }

}

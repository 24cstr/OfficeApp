package com.example.officeapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.confidential_screen.*
import kotlinx.android.synthetic.main.first_login.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var rl:MyLocation

    companion object{
        var username=""
        lateinit var context: Context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context=this.applicationContext
        rl=MyLocation()
        auth = FirebaseAuth.getInstance()

        if(auth.currentUser!=null)
            username= ""+auth.currentUser!!.email!!.removeSuffix("@idrbt.ac.in")

        if(username=="")
        {
            LoginUI()
        }
        else
        {
            MainUI(auth.currentUser)
        }
    }

    //Start of app, turns on Bluetooth support and location permission
    override fun onStart()
    {
        super.onStart()

        getLocationPermissions()

        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (mBluetoothAdapter == null)
        {
            Toast.makeText(applicationContext, "Bluetooth Not Supported", Toast.LENGTH_LONG).show()
        }
        else
        {
            // if bluetooth is supported but not enabled then enable it
            if (!mBluetoothAdapter.isEnabled)
            {
                val bluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(bluetoothIntent)
            }
        }
    }

    //Actions performed on Login
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

                    rl.recordLocation()

                    MainUI(auth.currentUser)

                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(baseContext, "Authentication failed.",Toast.LENGTH_SHORT).show()
                }

            }
    }

    //Validating Login Form
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

    //Actions Performed on Logout
    private fun logoutActivity()
    {
        rl.stopRecording()
        FirebaseAuth.getInstance().signOut()
        username=""
        auth= FirebaseAuth.getInstance()
        finishActivity(0)
        setContentView(R.layout.first_login)
    }

    //Starts LoginUI screen
    private fun LoginUI()
    {
        setContentView(R.layout.first_login)
        login_button.setOnClickListener { loginActivity() }
    }

    //Starts MainUI screen
    private fun MainUI(usr: FirebaseUser?)
    {
        if (usr != null)
        {
            setContentView(R.layout.activity_main)
            restrictedsec_button.setOnClickListener { checkLocationLogin() }
            logout_button.setOnClickListener { logoutActivity() }
            welcomeUser.text="Welcome "+ usr.email!!.removeSuffix("@idrbt.ac.in")

        }
    }

    //Checks Current Location before allowing access
    private fun checkLocationLogin()
    {
        setContentView(R.layout.confidential_screen)
        back_button.setOnClickListener { MainUI(auth.currentUser) }
        logout_res.setOnClickListener { logoutActivity() }
    }

    //Checks is permission are available and asks for permission not allowed yet
    private fun getLocationPermissions()
    {
        val hasForegroundLocationPermission = ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasForegroundLocationPermission) {
            val hasBackgroundLocationPermission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasBackgroundLocationPermission) {
                Log.d("Permissions: ", "LOCATION PERMISSION Present")
            }
            else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1
                )
            }
        }

        else {

            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ), 2)
        }
    }

}

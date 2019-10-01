package com.nopoint.midpoint

import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.activity_entry.*
import android.widget.TextView
import com.nopoint.midpoint.fragments.LoginFragment
import com.nopoint.midpoint.fragments.SignUpFragment
import com.nopoint.midpoint.models.CurrentUser


class EntryActivity : AppCompatActivity() {

    private lateinit var fTransaction: FragmentTransaction
    private lateinit var fManager: FragmentManager
    private lateinit var loginFragment: LoginFragment
    private lateinit var signUpFragment: SignUpFragment

    private  var isLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        val strokeParams = entry_stroke.layoutParams as RelativeLayout.LayoutParams
        if(checkValidSession()){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        fManager = supportFragmentManager
        fTransaction = fManager.beginTransaction()

        loginFragment = LoginFragment()
        signUpFragment = SignUpFragment()

        getLoginFragment()

        isLogin = true
        toggleTextStyle(textbutton_show_login, textbutton_show_sign_up)

        textbutton_show_login.setOnClickListener {
            isLogin = true

            getLoginFragment()

            toggleTextStyle(textbutton_show_login, textbutton_show_sign_up)
            strokeParams.addRule(RelativeLayout.ALIGN_START, R.id.textbutton_show_login)
            strokeParams.addRule(RelativeLayout.ALIGN_END, R.id.textbutton_show_login)
        }

        textbutton_show_sign_up.setOnClickListener {
            isLogin = false

            getSignUpFragment()

            toggleTextStyle(textbutton_show_sign_up, textbutton_show_login)
            strokeParams.addRule(RelativeLayout.ALIGN_START, R.id.textbutton_show_sign_up)
            strokeParams.addRule(RelativeLayout.ALIGN_END, R.id.textbutton_show_sign_up)
            entry_stroke.layoutParams = strokeParams
        }
    }

    private fun checkValidSession(): Boolean {
        return (CurrentUser.getLocalUser(this) != null)
    }

    private fun toggleTextStyle(active: TextView, inactive: TextView) {
        active.typeface = Typeface.DEFAULT_BOLD
        inactive.typeface = Typeface.DEFAULT
    }

    fun getLoginFragment() {
        fTransaction = fManager.beginTransaction()
        fTransaction.replace(R.id.entry_fragment_container, loginFragment)
        fTransaction.commit()
    }

    private fun getSignUpFragment() {
        fTransaction = fManager.beginTransaction()
        fTransaction.replace(R.id.entry_fragment_container, signUpFragment)
        fTransaction.commit()
    }

}

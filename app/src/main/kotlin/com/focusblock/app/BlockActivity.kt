package com.focusblock.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Full-screen message shown in place of a blocked app. */
class BlockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)
        findViewById<TextView>(R.id.reasonText).text =
            intent.getStringExtra(EXTRA_REASON) ?: "This app is blocked."
        findViewById<Button>(R.id.homeButton).setOnClickListener { goHome() }
    }

    override fun onBackPressed() { goHome() }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    companion object {
        const val EXTRA_REASON = "reason"
    }
}

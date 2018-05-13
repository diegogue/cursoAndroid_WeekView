package com.alamkanak.weekview.sample

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

/**
 * The launcher activity of the sample app. It contains the links to visit all the example screens.
 * Created by Raquib-ul-Alam Kanak on 7/21/2014.
 * Website: http://alamkanak.github.io
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        buttonBasic.setOnClickListener {
            startActivity(Intent(this@MainActivity, BasicActivity::class.java))
        }
        buttonAsynchronous.setOnClickListener {
            startActivity(Intent(this@MainActivity, AsynchronousActivity::class.java))
        }
        buttonWholeViewSnap.setOnClickListener {
            startActivity(Intent(this@MainActivity, WholeViewSnappingActivity::class.java))
        }
        startActivity(Intent(this@MainActivity, WholeViewSnappingActivity::class.java))
        finish()
    }
}

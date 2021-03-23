package com.jcoronado.minimalbitcoinwidget

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    // var data will hold the information received from the HTTP Request
    var data = Data()

    // val TAG for Log.i() calls
    private val TAG = "Main Activity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // follow system theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setContentView(R.layout.activity_main)

        // inflate toolbar
        val mToolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.m_toolbar)
        setSupportActionBar(mToolbar)

        // create TextView objects that contain reference to layout objects
        val priceTv: TextView = findViewById(R.id.main_price_text)
        val changeTv: TextView = findViewById(R.id.main_day_change)

        // set TextView's text to loading string
        priceTv.text = getString(R.string.loading_text)
        changeTv.text = getString(R.string.loading_text)

        // initial HTTP GET request
        fetchData()

        // when update button gets pressed
        val updateButton: ImageButton = findViewById(R.id.main_refresh_button)
        updateButton.setOnClickListener {
            Log.i(TAG, "Refresh button pressed.")

            // temp changes to show price is loading
            runOnUiThread {
                changeTv.setTextColor(priceTv.currentTextColor)
                priceTv.text = getString(R.string.loading_text)
                changeTv.text = getString(R.string.loading_text)
            }
            // make HTTP GET request
            fetchData()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.version_info -> {
                val intent = Intent(this, InfoActivity::class.java)
                startActivity(intent)
            }

            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        return true
    }

    // function called from background thread to update main_activity layout
    private fun updateLayout(values: Data) {

        // create TextView objects that contain reference to layout objects
        val priceTv: TextView = findViewById(R.id.main_price_text)
        val changeTv: TextView = findViewById(R.id.main_day_change)

        runOnUiThread {
            // update the layout with new data
            priceTv.text = values.price()
            changeTv.text = values.change24h()

            // check for positive or negative change to set color accordingly
            if (values.change24h() == "0.0%") {
                // no change
                changeTv.setTextColor(priceTv.currentTextColor)
            } else if (values.change24h().contains('+')) {
                // green color
                changeTv.setTextColor(ContextCompat.getColor(this, R.color.positive_green))
            } else {
                // red color
                changeTv.setTextColor(ContextCompat.getColor(this, R.color.negative_red))
            }

        }
    }

    // HTTP GET request using OkHttp library
    private fun fetchData() {

        // set up shared preferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)

        // store current currency selection
        val currency = prefs.getString("currency", "usd")

        // build API url string with selected currency
        val url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=$currency&ids=bitcoin"
        val request = Request.Builder().url(url).build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                Log.i(TAG, "GET request successful.")

                // converts response into string
                val body = response.body?.string()

                // extracts object from JSON
                val tempList: Array<Data> = Gson().fromJson(body, Array<Data>::class.java)
                data = tempList[0]
                // update the activity_main layout with the new price data
                updateLayout(data)
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.i(TAG, "Failed to execute GET request.")
            }
        })
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // HTTP GET the new data using new currency selection
        fetchData()
    }
}
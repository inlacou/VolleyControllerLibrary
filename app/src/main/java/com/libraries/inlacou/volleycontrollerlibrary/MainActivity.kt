package com.libraries.inlacou.volleycontrollerlibrary

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import com.android.volley.NoConnectionError

import com.google.gson.Gson
import com.libraries.inlacou.volleycontroller.InternetCall
import com.libraries.inlacou.volleycontroller.VolleyController
import com.libraries.inlacou.volleycontroller.errorMessage
import org.json.JSONObject
import timber.log.Timber

import java.util.HashMap

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
	private var textView: TextView? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		val toolbar = findViewById<Toolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)
		
		ApplicationController.instance
		
		VolleyController.onCall(InternetCall()
				.setUrl("https://ws.microcanales.com/allow-access/")
				.setMethod(InternetCall.Method.GET)
				.setCancelTag(this)
				.addSuccessCallback { response, code ->
					textView?.text = response.data
				}
				.addErrorCallback { error, code ->
					textView?.text = error.errorMessage
				})

		val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
		val toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
		drawer.setDrawerListener(toggle)
		toggle.syncState()

		val navigationView = findViewById<NavigationView>(R.id.nav_view)
		navigationView.setNavigationItemSelectedListener(this)

		textView = findViewById(R.id.textView)
	}

	override fun onBackPressed() {
		val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START)
		} else {
			super.onBackPressed()
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.main, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		val id = item.itemId

		return if (id == R.id.action_settings) {
			true
		} else super.onOptionsItemSelected(item)

	}

	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		// Handle navigation view item clicks here.
		val id = item.itemId

		when (id) {
			R.id.nav_GET -> VolleyController.onCall(InternetCall()
					.setUrl("https://boiling-castle-90818.herokuapp.com/muscleGroups")
					.setMethod(InternetCall.Method.GET)
					.setCode("code")
					.addSuccessCallback { response, code ->
						textView?.text = response.data
					}
					.addErrorCallback { error, code ->
						textView?.text = if(error is NoConnectionError) "No connection error" else error.errorMessage
					})
			R.id.nav_POST -> VolleyController.onCall(InternetCall()
					.setUrl("http://jsonplaceholder.typicode.com/posts")
					.setMethod(InternetCall.Method.POST)
					.putParam("title", "foo")
					.putParam("body", "bar")
					.putParam("userId", "5")
					.putParam("null", null)
					.putParam("notNull", "something")
					.setCode("code_create_posts")
					.addSuccessCallback { response, code ->
						textView?.text = response.data
					}
					.addErrorCallback { error, code ->
						textView?.text = error.errorMessage
					})
			R.id.nav_POST_2 -> VolleyController.onCall(InternetCall()
					.setUrl("http://jsonplaceholder.typicode.com/posts")
					.setMethod(InternetCall.Method.POST)
					.setRawBody("{ \"title\": \"foo\" }")
					.setCode("code_create_posts")
					.addSuccessCallback { response, code ->
						textView?.text = response.data
					}
					.addErrorCallback { error, code ->
						textView?.text = error.errorMessage
					})
			R.id.nav_PUT -> {
				val params = HashMap<String, String>()
				params["id"] = "1"
				params["title"] = "foo"
				params["notNull"] = "something"
				params["body"] = "bar"
				params["userId"] = "1"
				VolleyController.onCall(InternetCall()
						.setUrl("http://jsonplaceholder.typicode.com/posts/1")
						.setMethod(InternetCall.Method.PUT)
						.putParams(params)
						.setCode("code_modify_post")
						.addSuccessCallback { response, code ->
							textView?.text = response.data
						}
						.addErrorCallback { error, code ->
							textView?.text = error.errorMessage
						})
			}
			R.id.nav_DELETE -> VolleyController.onCall(InternetCall()
					.setUrl("http://jsonplaceholder.typicode.com/posts/1")
					.setMethod(InternetCall.Method.DELETE)
					.setCode("code_delete_post")
					.addSuccessCallback { response, code ->
						textView?.text = response.data
					}
					.addErrorCallback { error, code ->
						textView?.text = error.errorMessage
					})
			R.id.nav_no_response -> VolleyController.onCall(InternetCall()
					.setUrl("http://neosalut-quiz-api.pre.tak.es/answer-load/9")
					.setMethod(InternetCall.Method.GET)
					.setCode("code_get_no_response")
					.addSuccessCallback { response, code ->
						textView?.text = response.data
					}
					.addErrorCallback { error, code ->
						textView?.text = error.errorMessage
					})
			R.id.nav_GET_ssl -> VolleyController.onCall(InternetCall()
					.setUrl("https://178.62.73.124:3000/api/profile")
					.setMethod(InternetCall.Method.GET)
					.setCode("code_get_no_response")
					.putHeader("deviceId", "13")
					.putHeader("Authorization", "Bearer HIjHZmMgXAWlBM3NuycRFUmf3vR8fPZb0gGAVkiE")
					.addSuccessCallback { response, code ->
						textView?.text = response.data
					}
					.addErrorCallback { error, code ->
						textView?.text = error.errorMessage
					})
			R.id.nav_GET_activity_destroyed -> {
				VolleyController.onCall(InternetCall()
						.setUrl("http://playground.byvapps.com/api/search?offset=0&limit=1000000")
						.setMethod(InternetCall.Method.GET)
						.setCancelTag(this)
						.addSuccessCallback { response, code ->
							textView?.text = response.data
						}
						.addErrorCallback { error, code ->
							textView?.text = error.errorMessage
						})
				textView = null
				VolleyController.cancelRequest(this)
				Toast.makeText(this, "Should not give any response. If it gives, it's an app breaking one", Toast.LENGTH_SHORT).show()
			}
			R.id.nav_GET_mirror -> VolleyController.onCall(InternetCall()
					.setUrl("http://178.62.73.124:3000/api/mirror?id=1&id=2&id=3&offset=0&limit=1000000")
					.setMethod(InternetCall.Method.GET)
					.setCancelTag(this)
					.addSuccessCallback { response, code ->
						textView?.text = response.data
					}
					.addErrorCallback { error, code ->
						textView?.text = error.errorMessage
					})
			R.id.nav_test -> VolleyController.onCall(InternetCall()
					.setUrl("http://10.10.0.221:3000/ping")
					.setMethod(InternetCall.Method.GET)
					.setRawBody(JSONObject().apply {
						put("idTransaccion", "530")
						put("pinTransaccion", "1234")
					})
					.setCode("code-manual-test-call")
					.setCancelTag(this)
					.addSuccessCallback { response, code ->
						textView?.text = response.data
					}
					.addErrorCallback { error, code ->
						textView?.text = error.errorMessage
					})
		}

		val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
		drawer.closeDrawer(GravityCompat.START)
		return true
	}
}

package com.libraries.inlacou.volleycontrollerlibrary;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.libraries.inlacou.volleycontroller.CustomResponse;
import com.libraries.inlacou.volleycontroller.InternetCall;
import com.libraries.inlacou.volleycontroller.VolleyController;

import java.util.HashMap;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener {

	private static final String DEBUG_TAG = MainActivity.class.getName();
	private TextView textView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
						.setAction("Action", null).show();
			}
		});

		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.setDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		textView = findViewById(R.id.textView);
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();

		if (id == R.id.nav_GET) {
			VolleyController.INSTANCE.onCall(new InternetCall()
					.setUrl("http://playground.byvapps.com/api/search?offset=0&limit=100")
					.setMethod(InternetCall.Method.GET)
					.setCode("code")
					.addSuccessCallback(new Function2<CustomResponse, String, Unit>() {
						@Override
						public Unit invoke(CustomResponse response, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | CustomResponse: " + new Gson().toJson(response));
							textView.setText(response.getData());
							Log.d(DEBUG_TAG, "CustomResponse.headers: " + new Gson().toJson(response.getHeaders()));
							Log.d(DEBUG_TAG, "CustomResponse.data: " + response.getData());
							return null;
						}
					})
					.addErrorCallback(new Function2<VolleyError, String, Unit>() {
						@Override
						public Unit invoke(VolleyError error, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | VolleyError: " + error);
							textView.setText(VolleyController.INSTANCE.getMessage(error));
							return null;
						}
					}));
		} else if (id == R.id.nav_POST) {
			VolleyController.INSTANCE.onCall(new InternetCall()
					.setUrl("http://jsonplaceholder.typicode.com/posts")
					.setMethod(InternetCall.Method.POST)
					.putParam("title", "foo")
					.putParam("body", "bar")
					.putParam("userId", "5")
					.putParam("null", null)
					.putParam("notNull", "something")
					.setCode("code_create_posts")
					.addSuccessCallback(new Function2<CustomResponse, String, Unit>() {
						@Override
						public Unit invoke(CustomResponse response, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | CustomResponse: " + response);
							textView.setText(response.getData());
							return null;
						}
					})
					.addErrorCallback(new Function2<VolleyError, String, Unit>() {
						@Override
						public Unit invoke(VolleyError error, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | VolleyError: " + error);
							textView.setText(VolleyController.INSTANCE.getMessage(error));
							return null;
						}
					}));
		} else if (id == R.id.nav_POST_2) {
			VolleyController.INSTANCE.onCall(new InternetCall()
					.setUrl("http://jsonplaceholder.typicode.com/posts")
					.setMethod(InternetCall.Method.POST)
					.setRawBody("{ \"title\": \"foo\" }")
					.setCode("code_create_posts")
					.addSuccessCallback(new Function2<CustomResponse, String, Unit>() {
						@Override
						public Unit invoke(CustomResponse response, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | CustomResponse: " + response);
							textView.setText(response.getData());
							return null;
						}
					})
					.addErrorCallback(new Function2<VolleyError, String, Unit>() {
						@Override
						public Unit invoke(VolleyError error, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | VolleyError: " + error);
							textView.setText(VolleyController.INSTANCE.getMessage(error));
							return null;
						}
					}));
		} else if (id == R.id.nav_PUT) {
			HashMap<String, String> params = new HashMap<>();
			params.put("id", "1");
			params.put("title", "foo");
			params.put("null", null);
			params.put("notNull", "something");
			params.put("body", "bar");
			params.put("userId", "1");
			VolleyController.INSTANCE.onCall(new InternetCall()
					.setUrl("http://jsonplaceholder.typicode.com/posts/1")
					.setMethod(InternetCall.Method.PUT)
					.putParams(params)
					.setCode("code_modify_post")
					.addSuccessCallback(new Function2<CustomResponse, String, Unit>() {
						@Override
						public Unit invoke(CustomResponse response, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | CustomResponse: " + response);
							textView.setText(response.getData());
							return null;
						}
					})
					.addErrorCallback(new Function2<VolleyError, String, Unit>() {
						@Override
						public Unit invoke(VolleyError error, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | VolleyError: " + error);
							textView.setText(VolleyController.INSTANCE.getMessage(error));
							return null;
						}
					}));
		} else if (id == R.id.nav_DELETE) {
			VolleyController.INSTANCE.onCall(new InternetCall()
					.setUrl("http://jsonplaceholder.typicode.com/posts/1")
					.setMethod(InternetCall.Method.DELETE)
					.setCode("code_delete_post")
					.addSuccessCallback(new Function2<CustomResponse, String, Unit>() {
						@Override
						public Unit invoke(CustomResponse response, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | CustomResponse: " + response);
							textView.setText(response.getData());
							return null;
						}
					})
					.addErrorCallback(new Function2<VolleyError, String, Unit>() {
						@Override
						public Unit invoke(VolleyError error, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | VolleyError: " + error);
							textView.setText(VolleyController.INSTANCE.getMessage(error));
							return null;
						}
					}));
		} else if (id == R.id.nav_no_response) {
			VolleyController.INSTANCE.onCall(new InternetCall()
					.setUrl("http://neosalut-quiz-api.pre.tak.es/answer-load/9")
					.setMethod(InternetCall.Method.GET)
					.setCode("code_get_no_response")
					.addSuccessCallback(new Function2<CustomResponse, String, Unit>() {
						@Override
						public Unit invoke(CustomResponse response, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | CustomResponse: " + response);
							textView.setText(response.getData());
							return null;
						}
					})
					.addErrorCallback(new Function2<VolleyError, String, Unit>() {
						@Override
						public Unit invoke(VolleyError error, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | VolleyError: " + error);
							textView.setText(VolleyController.INSTANCE.getMessage(error));
							return null;
						}
					}));
		} else if (id == R.id.nav_GET_ssl) {
			VolleyController.INSTANCE.onCall(new InternetCall()
					.setUrl("https://178.62.73.124:3000/api/profile")
					.setMethod(InternetCall.Method.GET)
					.setCode("code_get_no_response")
					.putHeader("deviceId", "13")
					.putHeader("Authorization", "Bearer HIjHZmMgXAWlBM3NuycRFUmf3vR8fPZb0gGAVkiE")
					.addSuccessCallback(new Function2<CustomResponse, String, Unit>() {
						@Override
						public Unit invoke(CustomResponse response, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | CustomResponse: " + response);
							textView.setText(response.getData());
							return null;
						}
					})
					.addErrorCallback(new Function2<VolleyError, String, Unit>() {
						@Override
						public Unit invoke(VolleyError error, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | VolleyError: " + error);
							textView.setText(VolleyController.INSTANCE.getMessage(error));
							return null;
						}
					}));
		} else if (id == R.id.nav_GET_activity_destroyed) {
			VolleyController.INSTANCE.onCall(new InternetCall()
					.setUrl("http://playground.byvapps.com/api/search?offset=0&limit=1000000")
					.setMethod(InternetCall.Method.GET)
					.setCancelTag(this)
					.addSuccessCallback(new Function2<CustomResponse, String, Unit>() {
						@Override
						public Unit invoke(CustomResponse response, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | CustomResponse: " + response);
							textView.setText(response.getData());
							return null;
						}
					})
					.addErrorCallback(new Function2<VolleyError, String, Unit>() {
						@Override
						public Unit invoke(VolleyError error, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | VolleyError: " + error);
							textView.setText(VolleyController.INSTANCE.getMessage(error));
							return null;
						}
					}));
			textView = null;
			VolleyController.INSTANCE.cancelRequest(this);
			Toast.makeText(this, "Should not give any response. If it gives, it's an app broking one", Toast.LENGTH_SHORT).show();
		} else if (id == R.id.nav_GET_mirror) {
			VolleyController.INSTANCE.onCall(new InternetCall()
					.setUrl("http://178.62.73.124:3000/api/mirror?id=1&id=2&id=3&offset=0&limit=1000000")
					.setMethod(InternetCall.Method.GET)
					.setCancelTag(this)
					.addSuccessCallback(new Function2<CustomResponse, String, Unit>() {
						@Override
						public Unit invoke(CustomResponse response, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | CustomResponse: " + response);
							textView.setText(response.getData());
							return null;
						}
					})
					.addErrorCallback(new Function2<VolleyError, String, Unit>() {
						@Override
						public Unit invoke(VolleyError error, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | VolleyError: " + error);
							textView.setText(VolleyController.INSTANCE.getMessage(error));
							return null;
						}
					}));
		}

		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}
}

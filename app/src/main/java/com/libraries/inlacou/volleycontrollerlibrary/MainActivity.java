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

import com.android.volley.VolleyError;
import com.libraries.inlacou.volleycontroller.InternetCall;
import com.libraries.inlacou.volleycontroller.VolleyController;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener {

	private static final String DEBUG_TAG = MainActivity.class.getName();
	private TextView textView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
						.setAction("Action", null).show();
			}
		});

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.setDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		textView = (TextView) findViewById(R.id.textView);
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
			VolleyController.getInstance().onCall(new InternetCall()
					.setUrl("https://jsonplaceholder.typicode.com/posts")
					.setMethod(InternetCall.Method.GET)
					.setCode("code_get_posts")
					.addCallback(new VolleyController.IOCallbacks() {
						@Override
						public void onResponse(String response, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | Response: " + response);
							textView.setText(response);
						}

						@Override
						public void onResponseError(VolleyError error, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | Response: " + error);
							textView.setText(VolleyController.getInstance().getMessage(error));
						}
					}));
		} else if (id == R.id.nav_POST) {
			VolleyController.getInstance().onCall(new InternetCall()
					.setUrl("http://jsonplaceholder.typicode.com/posts")
					.setMethod(InternetCall.Method.POST)
					.putParam("title", "foo")
					.putParam("body", "bar")
					.putParam("userId", "1")
					.setCode("code_create_posts")
					.addCallback(new VolleyController.IOCallbacks() {
						@Override
						public void onResponse(String response, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | Response: " + response);
							textView.setText(response);
						}

						@Override
						public void onResponseError(VolleyError error, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | Response: " + error);
							textView.setText(VolleyController.getInstance().getMessage(error));
						}
					}));
		} else if (id == R.id.nav_PUT) {
			VolleyController.getInstance().onCall(new InternetCall()
					.setUrl("http://jsonplaceholder.typicode.com/posts/1")
					.setMethod(InternetCall.Method.PUT)
					.putParam("id", "1")
					.putParam("title", "foo")
					.putParam("body", "bar")
					.putParam("userId", "1")
					.setCode("code_modify_post")
					.addCallback(new VolleyController.IOCallbacks() {
						@Override
						public void onResponse(String response, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | Response: " + response);
							textView.setText(response);
						}

						@Override
						public void onResponseError(VolleyError error, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | Response: " + error);
							textView.setText(VolleyController.getInstance().getMessage(error));
						}
					}));
		} else if (id == R.id.nav_DELETE) {

			VolleyController.getInstance().onCall(new InternetCall()
					.setUrl("http://jsonplaceholder.typicode.com/posts/1")
					.setMethod(InternetCall.Method.DELETE)
					.setCode("code_delete_post")
					.addCallback(new VolleyController.IOCallbacks() {
						@Override
						public void onResponse(String response, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | Response: " + response);
							textView.setText(response);
						}

						@Override
						public void onResponseError(VolleyError error, String code) {
							Log.d(DEBUG_TAG, "Code: " + code + " | Response: " + error);
							textView.setText(VolleyController.getInstance().getMessage(error));
						}
					}));
		} else if (id == R.id.nav_share) {

		} else if (id == R.id.nav_send) {

		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}
}

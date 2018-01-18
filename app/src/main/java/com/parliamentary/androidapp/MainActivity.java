package com.parliamentary.androidapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private ListView lv;
    private ArrayList<HashMap<String, String>> contactList;
    private BottomNavigationView navigation;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            NavigationHelper navigationHelper = new NavigationHelper(MainActivity.this);
            navigationHelper.onBottomNavigationViewClick(item);
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contactList = new ArrayList<>();
        lv = (ListView) findViewById(R.id.list);
        navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.getMenu().getItem(0).setChecked(true);

        new GetContacts().execute();
    }

    private class GetContacts extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MainActivity.this, "Json Data is downloading", Toast.LENGTH_LONG).show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();
            int pageSize = 10;
            String url = "http://lda.data.parliament.uk/commonsdivisions.json?_view=Commons+Divisions&_pageSize=" + pageSize + "&_page=0";
            String jsonStr = sh.makeServiceCall(url);

            Log.e(TAG, "Response from url: " + jsonStr);
            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    JSONObject result = jsonObj.getJSONObject("result");
                    JSONArray items = result.getJSONArray("items");

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        String title = item.getString("title");
                        JSONObject dateObj = item.getJSONObject("date");
                        String date = dateObj.getString("_value");

                        String _about = item.getString("_about");
                        String[] aboutSplit = _about.split("/");
                        String divisionUrl = "http://lda.data.parliament.uk/commonsdivisions/id/" + aboutSplit[aboutSplit.length - 1] + ".json";
                        jsonStr = sh.makeServiceCall(divisionUrl);
                        jsonObj = new JSONObject(jsonStr);
                        result = jsonObj.getJSONObject("result");
                        JSONObject primaryTopic = result.getJSONObject("primaryTopic");

                        JSONArray ayesCountArr = primaryTopic.getJSONArray("AyesCount");
                        JSONObject ayesCountObj = ayesCountArr.getJSONObject(0);
                        String ayesCount = "Ayes: " + ayesCountObj.getString("_value");

                        JSONArray noesCountArr = primaryTopic.getJSONArray("Noesvotecount");
                        JSONObject noesCountObj = noesCountArr.getJSONObject(0);
                        String noesCount = "Noes: " + noesCountObj.getString("_value");

                        // tmp hash map for single contact
                        HashMap<String, String> contact = new HashMap<>();

                        // adding each child node to HashMap key => value
                        contact.put("title", title);
                        contact.put("date", date);
                        contact.put("ayes", ayesCount);
                        contact.put("noes", noesCount);

                        // adding contact to contact list
                        contactList.add(contact);
                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            ListAdapter adapter = new SimpleAdapter(MainActivity.this, contactList,
                    R.layout.list_item, new String[]{"title", "date", "ayes", "noes"},
                    new int[]{R.id.bill_title, R.id.bill_date, R.id.vote_ayes, R.id.vote_noes});
            lv.setAdapter(adapter);
        }
    }
}
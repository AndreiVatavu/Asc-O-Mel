package com.example.andre.ascomel;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener {

    private GoogleAccount googleAccount;
    private ProgressDialog mProgress;
    private List<SearchResult> responseItems = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(this);

        loadFragment(new HomeFragment());

        googleAccount = GoogleAccount.getGoogleAccount();

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling YouTube Data API ...");
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;

        switch (item.getItemId()) {
            case R.id.navigation_home:
                fragment = new HomeFragment();
                break;

            case R.id.navigation_dashboard:
                fragment = new DashboardFragment();
                break;

            case R.id.navigation_search:
                fragment = new SearchFragment();
                break;
        }

        loadFragment(fragment);

        return true;
    }

    List<String> searchList = new ArrayList<>();
    ListView simpleList;
    ArrayAdapter<String> adapter;

    public void searchByPattern(View view) {
        AutoCompleteTextView editText = (AutoCompleteTextView) findViewById(R.id.searh_box);
        String searchPattern = editText.getText().toString();
        new MakeRequestTask(googleAccount.getmCredential()).execute(searchPattern);
    }

    /**
     * An asynchronous task that handles the YouTube Data API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<String, Void, SearchListAdapter> {
        private com.google.api.services.youtube.YouTube mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Asc O Mel")
                    .build();
        }

        /**
         * Background task to call YouTube Data API.
         *
         * @param pattern search pattern
         */
        @Override
        protected SearchListAdapter doInBackground(String... pattern) {
            try {
                return getDataFromApi(pattern[0]);
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch information about the "GoogleDevelopers" YouTube channel.
         *
         * @return List of Strings containing information about search.
         * @throws IOException
         */
        private SearchListAdapter getDataFromApi(String pattern) throws IOException {
            List<Pair<String, String>> result = new ArrayList<>();
            try {
                HashMap<String, String> parameters = new HashMap<>();
                parameters.put("part", "snippet");
                parameters.put("maxResults", "25");
                parameters.put("q", pattern);
                parameters.put("type", "");

                YouTube.Search.List searchListByKeywordRequest = mService.search().list(parameters.get("part"));
                if (parameters.containsKey("maxResults")) {
                    searchListByKeywordRequest.setMaxResults(Long.parseLong(parameters.get("maxResults")));
                }

                if (parameters.containsKey("q") && !parameters.get("q").equals("")) {
                    searchListByKeywordRequest.setQ(parameters.get("q"));
                }

                if (parameters.containsKey("type") && !parameters.get("type").equals("")) {
                    searchListByKeywordRequest.setType(parameters.get("type"));
                }

                SearchListResponse response = searchListByKeywordRequest.execute();
                responseItems = new ArrayList<>();
                for (SearchResult item : response.getItems()) {
                    String title = item.getSnippet().getTitle();
                    String imageUrl = item.getSnippet().getThumbnails().getDefault().getUrl();
                    if (title != null) {
                        result.add(new Pair<>(title, imageUrl));
                        responseItems.add(item);
                    }
                }
            } catch (GoogleJsonResponseException e) {
                e.printStackTrace();
                System.err.println("There was a service error: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
            } catch (Throwable t) {
                t.printStackTrace();
            }

            SearchListAdapter adapter = null;

            if (result == null || result.size() == 0) {
//                mOutputText.setText("No results returned.");
            } else {
//                searchList = output;
                simpleList = findViewById(R.id.searc_list);
//                adapter = new ArrayAdapter<>(MainActivity.this, R.layout.activity_listview, R.id.textView, output);
                adapter = new SearchListAdapter(getApplicationContext(), result);
//                output.add(0, "Data retrieved using the YouTube Data API:");
//                mOutputText.setText(TextUtils.join("\n", output));
            }
            return adapter;
        }


        @Override
        protected void onPreExecute() {
//            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(SearchListAdapter adapter) {
            mProgress.hide();
            simpleList.setAdapter(adapter);
            simpleList.setOnItemClickListener(new ItemList());
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            LoginActivity.REQUEST_AUTHORIZATION);
                } else {
//                    mOutputText.setText("The following error occurred:\n"
//                            + mLastError.getMessage());
                }
            } else {
//                mOutputText.setText("Request cancelled.");
            }
        }
    }

    class ItemList implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            TextView textView = view.findViewById(R.id.video_name);
            Intent intent = new Intent(MainActivity.this, MediaActivity.class);
            intent.putExtra("searchPattern", responseItems.get(position).getId().getVideoId());
            startActivity(intent);
        }
    }
}

package com.example.andre.ascomel;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchFragment extends Fragment {

    private View searchFragmentView;
    private AutoCompleteTextView searchBox;
    private ArrayAdapter<String> searchSuggestionsAdapter;
    private Set<String> stringSet = null;
    private ProgressDialog mProgress;
    private FragmentActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, null);
    }

    @CallSuper
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mProgress = new ProgressDialog(getContext());
        mProgress.setMessage("Searching for music ...");

        // Get main activity
        mainActivity = getActivity();
        assert mainActivity != null;

        // Get search view
        searchFragmentView = getView();
        assert searchFragmentView != null;

        // Search suggestions
        searchBox = searchFragmentView.findViewById(R.id.searh_box);
        stringSet = new HashSet<>();
        searchSuggestionsAdapter = new ArrayAdapter<>(mainActivity, android.R.layout.simple_list_item_1, new ArrayList<String>());
        searchBox.setAdapter(searchSuggestionsAdapter);

        final String BASE_URL = "http://suggestqueries.google.com/complete/search?";

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client", "youtube");
        parameters.put("hl", "ro");
        parameters.put("gl", "ro");
        parameters.put("gs_ri", "youtube");
        parameters.put("ds", "yt");

        final String BASE_URL2 = buildURLRequest(BASE_URL, parameters);
        searchBox.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (s != null) {
                    String querry = "&q=" + s.toString();
                    String newURL = BASE_URL2 + querry;
                    new PageScraper().execute(newURL);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                /*This method is c alled to notify you that, within s, the count characters beginning at start are about to be replaced by new text with length after. It is an error to attempt to make changes to s from this callback.*/
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        // Search button onClick action
        Button searchButton = searchFragmentView.findViewById(R.id.search_button1);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchPattern = searchBox.getText().toString();
                new MakeRequestTask(GoogleAccount.getGoogleAccount().getCredential()).execute(searchPattern);
            }
        });
    }

    public String buildURLRequest(String baseURL, HashMap<String, String> parameters) {
        StringBuilder url = new StringBuilder(baseURL);
        for (final Map.Entry<String, String> e : parameters.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            url.append("&").append(key).append("=").append(value);
        }
        return url.toString();
    }

    private class PageScraper extends AsyncTask<String, Void, List<String>> {

        @Override
        protected List<String> doInBackground(String... url) {
            String mydata = "";

            try {
                mydata = Jsoup.connect(url[0]).get().html();
            } catch (IOException e) {
                e.printStackTrace();
            }

            List<String> searchSuggestions = new ArrayList<>();
            int start = mydata.indexOf("[[");
            int end = mydata.lastIndexOf("]]") + 2;
            if (start == -1 || end == -1) {
                cancel(true);
            }
            mydata = mydata.substring(start, end);
            try {
                List<List<Object>> rawData = new ObjectMapper().readValue(mydata, List.class);
                for (List<Object> it : rawData) {
                    searchSuggestions.add((String)it.get(0));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            List<String> newSearchSuggestions = new ArrayList<>();

            for (String it : searchSuggestions) {
                if (!stringSet.contains(it)) {
                    newSearchSuggestions.add(it);
                }
            }

            stringSet.addAll(newSearchSuggestions);
            return newSearchSuggestions;
        }

        @Override
        protected void onPostExecute(List<String> newSearchSuggestions) {
            if (newSearchSuggestions.isEmpty()) {
                return;
            }
            searchSuggestionsAdapter.addAll(newSearchSuggestions);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    /**
     * An asynchronous task that handles the YouTube Data API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */

    private List<SearchResult> responseItems = null;
    private class MakeRequestTask extends AsyncTask<String, Void, SearchListAdapter> {
        private com.google.api.services.youtube.YouTube mService = null;
        private Exception mLastError = null;
        private ListView simpleList;

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

        private boolean isDeviceOnline() {
            ConnectivityManager connMgr =
                    (ConnectivityManager) mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
        }

        /**
         * Fetch information about the "GoogleDevelopers" YouTube channel.
         *
         * @return List of Strings containing information about search.
         * @throws IOException
         */
        private SearchListAdapter getDataFromApi(String pattern) throws IOException {
            if (! isDeviceOnline()) {
                return null;
            }
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

            if (result.size() != 0) {
                simpleList = searchFragmentView.findViewById(R.id.searc_list);
                adapter = new SearchListAdapter(mainActivity.getApplicationContext(), result);
            }
            return adapter;
        }


        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(SearchListAdapter adapter) {
            mProgress.hide();
            if (adapter != null) {
                simpleList.setAdapter(adapter);
                simpleList.setOnItemClickListener(new ItemList());
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            LoginActivity.REQUEST_AUTHORIZATION);
                }
            }
        }
    }

    class ItemList implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            TextView textView = view.findViewById(R.id.video_name);
            Intent intent = new Intent(getContext(), MediaActivity.class);
            intent.putExtra("searchPattern", responseItems.get(position).getId().getVideoId());
            startActivity(intent);
        }
    }
}

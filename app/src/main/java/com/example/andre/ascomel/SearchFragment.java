package com.example.andre.ascomel;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    private PageScraper scraper = null;
    private ArrayAdapter<String> adapter2;
    private List<String> searchSuggestions = null;
    private Set<String> stringSet = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, null);
    }

    @CallSuper
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Get search view
        searchFragmentView = getView();
        assert searchFragmentView != null;

        searchBox = searchFragmentView.findViewById(R.id.searh_box);;
        searchSuggestions = new ArrayList<>();
        stringSet = new HashSet<>();
        adapter2 = new ArrayAdapter<>(searchFragmentView, android.R.layout.simple_list_item_1, searchSuggestions);
        searchBox.setAdapter(adapter2);

        final String baseURL = "http://suggestqueries.google.com/complete/search?";

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client", "youtube");
        parameters.put("hl", "ro");
        parameters.put("gl", "ro");
        parameters.put("gs_ri", "youtube");
        parameters.put("ds", "yt");

        final String baseURL2 = makeRequestURL(baseURL, parameters);
        searchBox.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (scraper != null) {
//                    scraper.cancel(true);
                }
                if (!s.toString().isEmpty()) {
                    String querry = "&q=" + s.toString();
                    String newURL = baseURL2 + querry;
                    scraper = new PageScraper();
                    scraper.execute(newURL);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                /*This method is c alled to notify you that, within s, the count characters beginning at start are about to be replaced by new text with length after. It is an error to attempt to make changes to s from this callback.*/
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });
    }

    String makeRequestURL(String baseURL, HashMap<String, String> parameters) {
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
        protected List<String> doInBackground(String... url2) {
            String mydata = "";

            try {
                mydata = Jsoup.connect(url2[0]).get().normalise().html();
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
                    if (isCancelled()) {
                        return null;
                    }
                    searchSuggestions.add((String)it.get(0));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return searchSuggestions;
        }

        @Override
        protected void onPostExecute(List<String> newSearchSuggestions) {
            if (newSearchSuggestions == null) {
                return;
            }
            searchSuggestions.addAll(newSearchSuggestions);
            adapter2.clear();
            adapter2.addAll(newSearchSuggestions);
            adapter2.notifyDataSetChanged();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }
}

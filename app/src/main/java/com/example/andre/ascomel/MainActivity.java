package com.example.andre.ascomel;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(this);

        loadFragment(new HomeFragment());
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

        return loadFragment(fragment);
    }

    List<String> countries = new ArrayList<>();
    ListView simpleList;
    ArrayAdapter<String> adapter;

    public void searchByPattern(View view) {
        simpleList = findViewById(R.id.searc_list);
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        countries.add("Romania");
        adapter = new ArrayAdapter<>(this, R.layout.activity_listview, R.id.textView, countries);
        simpleList.setAdapter(adapter);
    }
}

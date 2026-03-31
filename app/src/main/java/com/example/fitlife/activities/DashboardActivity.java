package com.example.fitlife.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.fitlife.R;
import com.example.fitlife.fragments.CalendarFragment;
import com.example.fitlife.fragments.HomeFragment;
import com.example.fitlife.fragments.ProfileFragment;
import com.example.fitlife.fragments.StatsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_ITEM_ID = "selectedItemId";
    private static final String TAG_HOME = "home";
    private static final String TAG_STATS = "stats";
    private static final String TAG_CALENDAR = "calendar";
    private static final String TAG_PROFILE = "profile";

    private BottomNavigationView bottomNavigation;
    private Fragment homeFragment, statsFragment, calendarFragment, profileFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        
        initFragments();
        setupBottomNavigation();

        int selectedItemId = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_SELECTED_ITEM_ID, R.id.nav_home)
                : R.id.nav_home;
        bottomNavigation.setSelectedItemId(selectedItemId);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNavigation != null) {
            outState.putInt(KEY_SELECTED_ITEM_ID, bottomNavigation.getSelectedItemId());
        }
    }

    private void initFragments() {
        FragmentManager fm = getSupportFragmentManager();

        homeFragment = fm.findFragmentByTag(TAG_HOME);
        statsFragment = fm.findFragmentByTag(TAG_STATS);
        calendarFragment = fm.findFragmentByTag(TAG_CALENDAR);
        profileFragment = fm.findFragmentByTag(TAG_PROFILE);

        boolean needCommit = false;
        androidx.fragment.app.FragmentTransaction tx = fm.beginTransaction();

        if (homeFragment == null) {
            homeFragment = new HomeFragment();
            tx.add(R.id.fragment_container, homeFragment, TAG_HOME);
            needCommit = true;
        }
        if (statsFragment == null) {
            statsFragment = new StatsFragment();
            tx.add(R.id.fragment_container, statsFragment, TAG_STATS).hide(statsFragment);
            needCommit = true;
        }
        if (calendarFragment == null) {
            calendarFragment = new CalendarFragment();
            tx.add(R.id.fragment_container, calendarFragment, TAG_CALENDAR).hide(calendarFragment);
            needCommit = true;
        }
        if (profileFragment == null) {
            profileFragment = new ProfileFragment();
            tx.add(R.id.fragment_container, profileFragment, TAG_PROFILE).hide(profileFragment);
            needCommit = true;
        }

        if (needCommit) {
            tx.commit();
        }

        activeFragment = findVisibleFragment();
        if (activeFragment == null) {
            activeFragment = homeFragment;
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                showFragment(homeFragment);
                return true;
            } else if (itemId == R.id.nav_stats) {
                showFragment(statsFragment);
                return true;
            } else if (itemId == R.id.nav_calendar) {
                showFragment(calendarFragment);
                return true;
            } else if (itemId == R.id.nav_profile) {
                showFragment(profileFragment);
                return true;
            }
            return false;
        });
    }

    private void showFragment(Fragment fragmentToShow) {
        if (fragmentToShow == null) return;
        FragmentManager fm = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction tx = fm.beginTransaction();

        if (homeFragment != null && homeFragment.isAdded() && homeFragment != fragmentToShow) tx.hide(homeFragment);
        if (statsFragment != null && statsFragment.isAdded() && statsFragment != fragmentToShow) tx.hide(statsFragment);
        if (calendarFragment != null && calendarFragment.isAdded() && calendarFragment != fragmentToShow) tx.hide(calendarFragment);
        if (profileFragment != null && profileFragment.isAdded() && profileFragment != fragmentToShow) tx.hide(profileFragment);

        if (fragmentToShow.isAdded()) {
            tx.show(fragmentToShow);
        }
        tx.commit();
        activeFragment = fragmentToShow;
    }

    private Fragment findVisibleFragment() {
        if (homeFragment != null && homeFragment.isAdded() && homeFragment.isVisible()) return homeFragment;
        if (statsFragment != null && statsFragment.isAdded() && statsFragment.isVisible()) return statsFragment;
        if (calendarFragment != null && calendarFragment.isAdded() && calendarFragment.isVisible()) return calendarFragment;
        if (profileFragment != null && profileFragment.isAdded() && profileFragment.isVisible()) return profileFragment;
        return null;
    }
}

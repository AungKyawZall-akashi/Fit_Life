package com.example.fitlife.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.fitlife.R;
import com.example.fitlife.fragments.CalendarFragment;
import com.example.fitlife.fragments.HomeFragment;
import com.example.fitlife.fragments.ProfileFragment;
import com.example.fitlife.fragments.StatsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

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

        // Load HomeFragment by default only if not already restored
        if (savedInstanceState == null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }

    private void initFragments() {
        homeFragment = new HomeFragment();
        statsFragment = new StatsFragment();
        calendarFragment = new CalendarFragment();
        profileFragment = new ProfileFragment();

        // Add all fragments but hide others
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, profileFragment, "4").hide(profileFragment).commit();
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, calendarFragment, "3").hide(calendarFragment).commit();
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, statsFragment, "2").hide(statsFragment).commit();
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, homeFragment, "1").commit();
        
        activeFragment = homeFragment;
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                getSupportFragmentManager().beginTransaction().hide(activeFragment).show(homeFragment).commit();
                activeFragment = homeFragment;
                return true;
            } else if (itemId == R.id.nav_stats) {
                getSupportFragmentManager().beginTransaction().hide(activeFragment).show(statsFragment).commit();
                activeFragment = statsFragment;
                return true;
            } else if (itemId == R.id.nav_calendar) {
                getSupportFragmentManager().beginTransaction().hide(activeFragment).show(calendarFragment).commit();
                activeFragment = calendarFragment;
                return true;
            } else if (itemId == R.id.nav_profile) {
                getSupportFragmentManager().beginTransaction().hide(activeFragment).show(profileFragment).commit();
                activeFragment = profileFragment;
                return true;
            }
            return false;
        });
    }
}
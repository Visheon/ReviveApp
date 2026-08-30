package com.example.reviveapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.reviveapp.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

@Suppress("OVERRIDE_DEPRECATION")
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var fragmentManager: FragmentManager //declares fragment manager
    private lateinit var binding: ActivityMainBinding // layouts using binding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar) // toolbar is the apps action bar
        val toggle = ActionBarDrawerToggle(this,binding.drawerLayout,binding.toolbar,R.string.nav_open, R.string.nav_close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationDrawer.setNavigationItemSelectedListener(this) // this listens to navigation items

        binding.bottomNavigation.background = null
        binding.bottomNavigation.setOnItemSelectedListener{ item -> // this listens to the BOTTOM navigation items
            when(item.itemId){
                //replace fragment based on selected items
                R.id.bottom_home -> openFragment(HomeFragment())
                R.id.bottom_foods -> openFragment(ItemFragment())
                R.id.bottom_meals -> openFragment(MealFragment())
            }
            true
        }
        fragmentManager = supportFragmentManager
        openFragment(HomeFragment()) //the default fragment will be the home one

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            // selects which fragment to open over the top
            R.id.nav_profile -> openFragment(ProfileFragment())
            R.id.nav_personalinfo -> openFragment(PersonalInfoFragment())
            R.id.nav_notifications -> openFragment(NotificationsFragment())
            R.id.nav_calculator -> openFragment(CalorieCalculatorFragment())
            R.id.nav_dailygoals -> openFragment(DailyGoalsFragment())
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true

    }
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)){
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else{
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun openFragment(fragment: Fragment){ //Function to open a specific fragment by replacing the current fragment
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container,fragment)
        fragmentTransaction.commit()
    }
}
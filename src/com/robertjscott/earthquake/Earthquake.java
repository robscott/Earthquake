package com.robertjscott.earthquake;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

public class Earthquake extends Activity {
  
  static final private int MENU_PREFERENCES = Menu.FIRST+1;
  static final private int MENU_UPDATE = Menu.FIRST+2;
  
  private static final int SHOW_PREFERENCES = 1;
  
  public int minimumMagnitude = 0;
  public boolean autoUpdateChecked = false;
  public int updateFreq = 0;

  public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
    
    private Fragment fragment;
    private Activity activity;
    private Class<T> fragmentClass;
    
    private int fragmentContainer;
    
    public TabListener(Activity activity, int fragmentContainer, Class<T> fragmentClass) {
      this.activity = activity;
      this.fragmentContainer = fragmentContainer;
      this.fragmentClass = fragmentClass;
    }
    
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
      if (fragment == null) {
        String fragmentName = fragmentClass.getName();
        fragment = Fragment.instantiate(activity, fragmentName);
        ft.add(fragmentContainer, fragment, fragmentName);
      } else {
        ft.attach(fragment);
      }
    }

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
      if (fragment != null) ft.detach(fragment);
    }

    
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
      if (fragment != null) ft.attach(fragment);
    }
  }
  
  TabListener<EarthquakeListFragment> listTabListener;
  TabListener<EarthquakeMapFragment> mapTabListener;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_earthquake);
    
    updateFromPreferences();
    
    SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
    SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
    
    SearchView searchView = (SearchView)findViewById(R.id.searchView);
    searchView.setSearchableInfo(searchableInfo);
    
    ActionBar actionBar = getActionBar();
    
    View fragmentContainer = findViewById(R.id.EarthquakeFragmentContainer);
    
    boolean tabletLayout = (fragmentContainer == null);
    
    if (!tabletLayout) {
      actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
      actionBar.setDisplayShowTitleEnabled(false);
      
      Tab listTab = actionBar.newTab();
      listTabListener = new TabListener<EarthquakeListFragment>
        (this, R.id.EarthquakeFragmentContainer, EarthquakeListFragment.class);
      
      listTab.setText("List")
             .setContentDescription("List of earthquakes")
             .setTabListener(listTabListener);

      actionBar.addTab(listTab);
      
      Tab mapTab = actionBar.newTab();
      mapTabListener = new TabListener<EarthquakeMapFragment>
        (this, R.id.EarthquakeFragmentContainer, EarthquakeMapFragment.class);
      
      mapTab.setText("Map")
            .setContentDescription("Map of earthquakes")
            .setTabListener(mapTabListener);
      
      actionBar.addTab(mapTab);
    }
  }
  
  private static String ACTION_BAR_INDEX = "ACTION_BAR_INDEX";
  
  @Override
  public void onSaveInstanceState(Bundle outState) {
    View fragmentContainer = findViewById(R.id.EarthquakeFragmentContainer);
    boolean tabletLayout = (fragmentContainer == null);
    
    if (!tabletLayout) {
      int actionBarIndex = getActionBar().getSelectedTab().getPosition();
      SharedPreferences.Editor editor = getPreferences(Activity.MODE_PRIVATE).edit();
      editor.putInt(ACTION_BAR_INDEX, actionBarIndex);
      editor.apply();
      
      FragmentTransaction ft = getFragmentManager().beginTransaction();
      if (mapTabListener.fragment != null) ft.detach(mapTabListener.fragment);
      if (listTabListener.fragment != null) ft.detach(listTabListener.fragment);
      ft.commit();
    }
    
    super.onSaveInstanceState(outState);
  }
  
  @Override
  public void onRestoreInstanceState(Bundle outState) {
    super.onRestoreInstanceState(outState);

    View fragmentContainer = findViewById(R.id.EarthquakeFragmentContainer);
    boolean tabletLayout = (fragmentContainer == null);
    
    if (!tabletLayout) {
      listTabListener.fragment = getFragmentManager().findFragmentByTag(EarthquakeListFragment.class.getName());
      mapTabListener.fragment = getFragmentManager().findFragmentByTag(EarthquakeMapFragment.class.getName());
      
      SharedPreferences sp = getPreferences(Activity.MODE_PRIVATE);
      int actionBarIndex = sp.getInt(ACTION_BAR_INDEX, 0);
      getActionBar().setSelectedNavigationItem(actionBarIndex);
    }
  }


  @Override
  public void onResume() {
    super.onResume();

    View fragmentContainer = findViewById(R.id.EarthquakeFragmentContainer);
    boolean tabletLayout = (fragmentContainer == null);
    
    if (!tabletLayout) {
      SharedPreferences sp = getPreferences(Activity.MODE_PRIVATE);
      int actionBarIndex = sp.getInt(ACTION_BAR_INDEX, 0);
      getActionBar().setSelectedNavigationItem(actionBarIndex);
    }
  }

  

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    
    menu.add(0, MENU_PREFERENCES, Menu.NONE, R.string.menu_preferences);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case (MENU_PREFERENCES): {
        Class c = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ? 
          PreferencesActivity.class : FragmentPreferences.class;
        
        Intent i = new Intent(this, c);
        startActivityForResult(i, SHOW_PREFERENCES);
        return true;
      }
    }
    return false;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    
    if (requestCode == SHOW_PREFERENCES) {
      updateFromPreferences();
      startService(new Intent(this, EarthquakeUpdateService.class));
    }
  }
  
  
  private void updateFromPreferences() {
    Context context = getApplicationContext();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

    minimumMagnitude = Integer.parseInt(prefs.getString(PreferencesActivity.PREF_MIN_MAG, "3"));
    updateFreq = Integer.parseInt(prefs.getString(PreferencesActivity.PREF_UPDATE_FREQ, "60"));

    autoUpdateChecked = prefs.getBoolean(PreferencesActivity.PREF_AUTO_UPDATE, false);    
  }
}

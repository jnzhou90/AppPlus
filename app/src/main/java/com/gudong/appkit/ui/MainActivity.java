package com.gudong.appkit.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.afollestad.materialdialogs.ThemeSingleton;
import com.gudong.appkit.R;
import com.gudong.appkit.adapter.AppPageListAdapter;
import com.gudong.appkit.dao.AppInfoEngine;
import com.gudong.appkit.entity.AppEntity;
import com.gudong.appkit.ui.base.BaseActivity;
import com.gudong.appkit.ui.fragment.AppListFragment;
import com.gudong.appkit.ui.fragment.ChangelogDialog;
import com.gudong.appkit.utils.ThemeUtils;
import com.gudong.appkit.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {
    Toolbar mToolbar;
    AppBarLayout mAppBar;
    DrawerLayout mDrawerLayout;
    TabLayout mTabLayout;
    ViewPager mViewPager;
    FrameLayout mFlSearchResult;
    AppListFragment mSearchResultFragment;
    List<AppEntity>mListInstalled;
    AppPageListAdapter mFragmentAdapter;
    private static int[]mTitles = new int[]{R.string.tab_recent,R.string.tab_installed};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            checkPermission();
        }

        mAppBar = (AppBarLayout) findViewById(R.id.appbar);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.mipmap.ic_menu);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
//        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        if (mViewPager != null) {
            setupViewPager(mViewPager);
        }

        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        if (mTabLayout != null) {
            mTabLayout.setupWithViewPager(mViewPager);
        }

        mFlSearchResult = (FrameLayout) findViewById(R.id.fl_contain_search_result);

        initSearchContent();

        versionCheck();
    }

    private void initSearchContent(){
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        mSearchResultFragment = AppListFragment.getInstance(-1);
        fragmentTransaction.add(R.id.fl_contain_search_result,mSearchResultFragment);
        fragmentTransaction.commit();
    }

    private void versionCheck() {
        String currentVersion = getAppVersion();
        String localVersionName = Utils.getLocalVersion(this);
        if(!localVersionName.equals(currentVersion)){
            showVersionLogView();
            Utils.setCurrentVersion(this,currentVersion);
        }
    }

    private String getAppVersion(){
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    /*
      Check if permission enabled
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void checkPermission(){
        AppInfoEngine engine = new AppInfoEngine(this);
        if (engine.getUsageStatsList().isEmpty()){
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        mFragmentAdapter = new AppPageListAdapter(getSupportFragmentManager(),this,mTitles);
        viewPager.setAdapter(mFragmentAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_app_hint));
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mTabLayout.setVisibility(View.GONE);
                    mViewPager.setVisibility(View.GONE);
                    mFlSearchResult.setVisibility(View.VISIBLE);
                    //设置toolbar的scrollFlag 让他不响应RecycleView的滑动事件
                    AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
                    params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
                } else {
                    mTabLayout.setVisibility(View.VISIBLE);
                    mViewPager.setVisibility(View.VISIBLE);
                    mFlSearchResult.setVisibility(View.GONE);
                    mSearchResultFragment.cleatData();

                    AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
                    params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS| AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
                }
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                List<AppEntity>result = searchApp(getAllInstalledApp(),newText);
                mSearchResultFragment.setData(result);
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this,SettingsActivity.class));
                break;
        }
        return true;
    }


    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.menu_drawer_theme:
                                boolean isDark = ThemeUtils.isDarkMode(MainActivity.this);
                                ThemeUtils.setTheme(MainActivity.this, !isDark);
                                recreate();
                                break;
                            case R.id.menu_drawer_setting:
                                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                                break;
                            case R.id.menu_drawer_opinion:
                                sendOpinion();
                                break;
                        }
                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();
                        return true;
                    }
                });
    }


    private void sendOpinion(){
        Intent localIntent = new Intent("android.intent.action.SENDTO", Uri.parse("mailto:" + "1252768410@qq.com"));
        localIntent.putExtra("android.intent.extra.SUBJECT", getString(R.string.title_email_opinion));
        localIntent.putExtra("android.intent.extra.TEXT", Utils.getLog(this));
        startActivity(localIntent);
    }

    private void showVersionLogView() {
        int accentColor = ThemeSingleton.get().widgetColor;
        if (accentColor == 0)
            accentColor = getResources().getColor(R.color.colorAccent);

        ChangelogDialog.create(false, accentColor)
                .show(getSupportFragmentManager(), "changelog");
    }

    private List<AppEntity>getAllInstalledApp(){
        if(mListInstalled == null){
            AppInfoEngine mEngine = new AppInfoEngine(this);
            mListInstalled = mEngine.getInstalledAppList();
        }
        return mListInstalled;
    }

    /**
     * 根据关键字搜索App
     * @param list
     * @param key
     * @return
     */
    private List<AppEntity>searchApp(List<AppEntity>list,String key){
        if(TextUtils.isEmpty(key)){
            return list;
        }
        List<AppEntity>resultList = new ArrayList<>();
        for(AppEntity entity:list){
            String appName = entity.getAppName();
            if(!TextUtils.isEmpty(appName) && appName.contains(key)){
                resultList.add(entity);
            }
        }
        return resultList;
    }
}

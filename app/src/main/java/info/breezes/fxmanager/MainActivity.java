package info.breezes.fxmanager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

import info.breezes.PreferenceUtil;
import info.breezes.fxmanager.model.DrawerMenu;
import info.breezes.toolkit.ui.LayoutViewHelper;
import info.breezes.toolkit.ui.annotation.LayoutView;


public class MainActivity extends ActionBarActivity implements MenuAdapter.OnItemClickListener, MediaFragment.OnOpenFolderListener {

    @LayoutView(R.id.rootView)
    private DrawerLayout rootView;
    @LayoutView(R.id.toolbar)
    private Toolbar toolbar;
    @LayoutView(R.id.menuList)
    private RecyclerView menuList;
    @LayoutView(R.id.viewPager)
    private ViewPager viewPager;

    private ActionBarDrawerToggle drawerToggle;
    private MenuAdapter menuAdapter;
    private FolderPagerAdapter folderPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LayoutViewHelper.initLayout(this);
        setSupportActionBar(toolbar);
        toolbar.setSubtitleTextAppearance(this, R.style.SubTitle);
        drawerToggle = new ActionBarDrawerToggle(this, rootView, toolbar, R.string.app_name, R.string.app_name);
        drawerToggle.setDrawerIndicatorEnabled(true);
        rootView.setDrawerListener(drawerToggle);
        menuList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        menuList.setHasFixedSize(true);//do not change size of recycler view when adapter change
        DrawerMenu sdMenu = new DrawerMenu(getString(R.string.menu_external_storage), Environment.getExternalStorageDirectory().getAbsolutePath(), getResources().getDrawable(R.drawable.ic_sd_storage));
        ArrayList<DrawerMenu> menus = new ArrayList<>();
        menus.add(sdMenu);
        if (PreferenceUtil.findPreference(this, R.string.pref_key_show_root, false)) {
            menus.add(new DrawerMenu(getString(R.string.menu_root_directory), "/", getResources().getDrawable(R.drawable.ic_storage)));
        }
        if (PreferenceUtil.findPreference(this, R.string.pref_key_show_dir_pic, true)) {
            menus.add(new DrawerMenu(getString(R.string.menu_picture), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(), getResources().getDrawable(R.drawable.ic_storage)));
        }
        if (PreferenceUtil.findPreference(this, R.string.pref_key_show_dir_pic, true)) {
            menus.add(new DrawerMenu(getString(R.string.menu_music), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath(), getResources().getDrawable(R.drawable.ic_storage)));
        }
        if (PreferenceUtil.findPreference(this, R.string.pref_key_show_dir_pic, true)) {
            menus.add(new DrawerMenu(getString(R.string.menu_movie), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath(), getResources().getDrawable(R.drawable.ic_storage)));
        }
        if (PreferenceUtil.findPreference(this, R.string.pref_key_show_dir_pic, true)) {
            menus.add(new DrawerMenu(getString(R.string.menu_document), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(), getResources().getDrawable(R.drawable.ic_storage)));
        }
        if (PreferenceUtil.findPreference(this, R.string.pref_key_show_dir_pic, true)) {
            menus.add(new DrawerMenu(getString(R.string.menu_camera), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath(), getResources().getDrawable(R.drawable.ic_storage)));
        }
        if (PreferenceUtil.findPreference(this, R.string.pref_key_show_dir_pic, true)) {
            menus.add(new DrawerMenu(getString(R.string.menu_download), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), getResources().getDrawable(R.drawable.ic_storage)));
        }
        menus.add(new DrawerMenu(R.id.menu_settings, getString(R.string.menu_settings), null, getResources().getDrawable(R.drawable.ic_settings)));
        menus.add(new DrawerMenu(R.id.menu_about, getString(R.string.menu_help_feedback), null, getResources().getDrawable(R.drawable.ic_settings)));
        menuList.setAdapter((menuAdapter = new MenuAdapter(this, menus.toArray(new DrawerMenu[menus.size()]))));
        menuAdapter.setOnItemClickListener(this);
        viewPager.setAdapter((folderPagerAdapter = new FolderPagerAdapter(getSupportFragmentManager())));
        String title = getIntent().getStringExtra(MediaFragment.EXTRA_DIR_NAME);
        String path = getIntent().getStringExtra(MediaFragment.EXTRA_INIT_DIR);
        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(path)) {
            openMedia(new DrawerMenu(title, path));
        } else {
            openMedia(sdMenu);
        }
    }

    @Override
    public void onItemClick(DrawerMenu item) {
        switch (item.id) {
            case R.id.menu_about:
                return;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return;
        }
        rootView.closeDrawer(Gravity.LEFT);
        getSupportActionBar().setSubtitle(item.path);
        openMedia(item);
    }

    private void openMedia(final DrawerMenu item) {
        viewPager.setCurrentItem(folderPagerAdapter.addMedia(item));
    }

    @Override
    public void onBackPressed() {
        MediaFragment fragment = (MediaFragment) getSupportFragmentManager().findFragmentById(R.id.viewPager);
        if (fragment != null) {
            if (!fragment.back()) {
                if (folderPagerAdapter.getCount() > 1) {
                    folderPagerAdapter.remove(fragment.getDrawerMenu());
                    return;
                }
            } else {
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onOpenFolder(String path) {
        getSupportActionBar().setSubtitle(path);
    }

    class FolderPagerAdapter extends FragmentStatePagerAdapter {
        private ArrayList<DrawerMenu> folders = new ArrayList<>();

        public FolderPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public MediaFragment getItem(int position) {
            return MediaFragment.newInstance(folders.get(position));
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return folders.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return folders.get(position).title;
        }

        public int addMedia(DrawerMenu item) {
            if (folders.contains(item)) {
                return folders.indexOf(item);
            }
            folders.add(item);
            notifyDataSetChanged();
            return folders.size();
        }

        public void remove(DrawerMenu menu) {
            folders.remove(menu);
            notifyDataSetChanged();
        }
    }
}

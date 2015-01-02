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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

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
        DrawerMenu sdMenu = new DrawerMenu("内部存储", Environment.getExternalStorageDirectory().getAbsolutePath(), getResources().getDrawable(R.drawable.ic_sd_storage));
        menuList.setAdapter((menuAdapter = new MenuAdapter(this, new DrawerMenu[]{
                //new DrawerMenu("根目录", "/", getResources().getDrawable(R.drawable.ic_storage)),
                sdMenu,
                new DrawerMenu("应用程序", "", getResources().getDrawable(R.drawable.ic_storage)),
                new DrawerMenu("图片", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(), getResources().getDrawable(R.drawable.ic_storage)),
                new DrawerMenu("音乐", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath(), getResources().getDrawable(R.drawable.ic_storage)),
                new DrawerMenu("电影", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath(), getResources().getDrawable(R.drawable.ic_storage)),
                new DrawerMenu("文档", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(), getResources().getDrawable(R.drawable.ic_storage)),
                new DrawerMenu("相机", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath(), getResources().getDrawable(R.drawable.ic_storage)),
                new DrawerMenu("下载", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), getResources().getDrawable(R.drawable.ic_storage)),
                new DrawerMenu(R.id.menu_settings, "设置", null, getResources().getDrawable(R.drawable.ic_settings)),
                new DrawerMenu(R.id.menu_about, "帮助和反馈", null, getResources().getDrawable(R.drawable.ic_settings)),
        })));
        menuAdapter.setOnItemClickListener(this);
        viewPager.setAdapter((folderPagerAdapter = new FolderPagerAdapter(getSupportFragmentManager())));
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {

            }
        });
        openMedia(sdMenu);
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

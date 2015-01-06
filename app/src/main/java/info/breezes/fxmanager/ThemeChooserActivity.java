package info.breezes.fxmanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;

import info.breezes.PreferenceUtil;
import info.breezes.SecretPreferenceUtil;
import info.breezes.fxmanager.countly.CountlyActivity;
import info.breezes.toolkit.ui.LayoutViewHelper;
import info.breezes.toolkit.ui.annotation.LayoutView;


public class ThemeChooserActivity extends CountlyActivity {

    @LayoutView(R.id.recyclerView)
    private RecyclerView recyclerView;
    @LayoutView(R.id.toolbar)
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_chooser);
        LayoutViewHelper.initLayout(this);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        layoutManager.setOrientation(GridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        setupActionBar();
        ArrayList<ThemeItem> themes = new ArrayList<>();
        themes.add(new ThemeItem(getString(R.string.red), R.drawable.theme_p_r, R.style.AppTheme_Red));
        themes.add(new ThemeItem(getString(R.string.blue), R.drawable.theme_p_b, R.style.AppTheme_Blue));
        themes.add(new ThemeItem(getString(R.string.purple), R.drawable.theme_p_p, R.style.AppTheme_Purple));
        themes.add(new ThemeItem(getString(R.string.green), R.drawable.theme_p_g, R.style.AppTheme_Green));
        themes.add(new ThemeItem(getString(R.string.yellow), R.drawable.theme_p_y, R.style.AppTheme_Yellow));
        recyclerView.setAdapter(new ThemesAdapter(this, themes));
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    class ThemeItem {
        public int preview;
        public int type;
        public String title;

        public ThemeItem(String title, int preview, int type) {
            this.title = title;
            this.preview = preview;
            this.type = type;
        }
    }

    class ThemesAdapter extends RecyclerView.Adapter<ThemesAdapter.ThemeItemHolder> {
        private Context context;
        private ArrayList<ThemeItem> themes;
        private int checkedType;

        public ThemesAdapter(Context context, ArrayList<ThemeItem> themes) {
            this.context = context;
            this.themes = themes;
            checkedType = PreferenceUtil.findPreference(context, R.string.pref_key_theme, R.style.AppTheme_Blue);
        }

        @Override
        public ThemeItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ThemeItemHolder(LayoutInflater.from(context).inflate(R.layout.theme_item, parent, false));
        }

        @Override
        public int getItemCount() {
            return themes == null ? 0 : themes.size();
        }

        @Override
        public void onBindViewHolder(ThemeItemHolder holder, int position) {
            holder.setThemeItem(themes.get(position));
        }

        class ThemeItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            private ThemeItem themeItem;

            @LayoutView(R.id.imageView)
            private ImageView previewImage;
            @LayoutView(R.id.checkImageView)
            private ImageView checkImage;

            public ThemeItemHolder(View itemView) {
                super(itemView);
                LayoutViewHelper.initLayout(itemView, this);
                itemView.setOnClickListener(this);
            }

            public void setThemeItem(ThemeItem themeItem) {
                this.themeItem = themeItem;
                checkImage.setVisibility(themeItem.type == checkedType ? View.VISIBLE : View.GONE);
                previewImage.setImageResource(themeItem.preview);
            }

            @Override
            public void onClick(View v) {
                if (checkedType != themeItem.type) {
                    checkedType = themeItem.type;
                    ThemesAdapter.this.notifyDataSetChanged();
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt(getString(R.string.pref_key_theme), themeItem.type);
                    editor.commit();
                    sendBroadcast(new Intent(ACTION_THEME_CHANGED));
                }
            }
        }
    }
}

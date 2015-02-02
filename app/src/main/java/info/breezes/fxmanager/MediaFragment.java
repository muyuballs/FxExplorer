/*
 * Copyright 2015. Qiao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.breezes.fxmanager;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import info.breezes.ComputerUnitUtils;
import info.breezes.PreferenceUtil;
import info.breezes.fxapi.MediaItem;
import info.breezes.fxapi.MediaItemViewer;
import info.breezes.fxapi.MediaProvider;
import info.breezes.fxapi.countly.CountlyEvent;
import info.breezes.fxapi.countly.CountlyFragment;
import info.breezes.fxapi.countly.CountlyUtils;
import info.breezes.fxmanager.model.DrawerMenu;
import info.breezes.toolkit.log.Log;
import info.breezes.toolkit.ui.Toast;

public class MediaFragment extends CountlyFragment implements ActionMode.Callback, MediaItemViewer {
    private static final String ARG_DRAWER_MENU = "mediaItems";
    private static final String State_Path_Stack = "_path_stack_";

    private static Executor executor = Executors.newFixedThreadPool(10);

    private OnOpenFolderListener onOpenFolderListener;

    private RecyclerView recyclerView;
    private DrawerMenu drawerMenu;

    private MediaAdapter mAdapter;

    private Stack<MediaItem> paths;

    private MediaProvider mediaProvider;
    private String currentPath;

    private ActionMode currentActionMode;

    private boolean showHiddenFiles;
    private boolean showRealPath;
    private boolean showPath;


    public static MediaFragment newInstance(DrawerMenu drawerMenu) {
        MediaFragment fragment = new MediaFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DRAWER_MENU, drawerMenu);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        drawerMenu = (DrawerMenu) getArguments().getSerializable(ARG_DRAWER_MENU);
        paths = new Stack<>();
        if (savedInstanceState != null) {
            Log.d(null, "restore saved state ," + drawerMenu.path);
            List<MediaItem> savedPaths = (List<MediaItem>) savedInstanceState.getSerializable(State_Path_Stack);
            if (savedPaths != null) {
                paths.addAll(savedPaths);
            }
        }
        if (paths.empty()) {
            currentPath = drawerMenu.path;
        } else {
            currentPath = paths.peek().path;
        }
        if (TextUtils.isEmpty(drawerMenu.mediaProvider)) {
            mediaProvider = new LocalFileSystemProvider(getActivity());
        } else {
            try {
                mediaProvider = (MediaProvider) Class.forName(drawerMenu.mediaProvider).getConstructors()[0].newInstance(getActivity());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(State_Path_Stack, paths);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_medias, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager gridManager = new GridLayoutManager(getActivity(), 4, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(gridManager);
        //RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        //recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter((mAdapter = new MediaAdapter(getActivity())));
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(MediaItem item) {
                if (currentActionMode != null) {
                    mAdapter.setItemSelected(item, !mAdapter.isSelected(item));
                    currentActionMode.setTitle(String.format("%d", mAdapter.getSelectedCount()));
                    if (mAdapter.getSelectedCount() < 1) {
                        currentActionMode.finish();
                    } else {
                        resetActionMenus();
                    }
                } else {
                    if (item.type == MediaItem.MediaType.Folder) {
                        loadFileTree(item);
                    } else if (item.type == MediaItem.MediaType.File) {
                        mediaProvider.launch(getActivity(), item);
                    }
                }
            }

            @Override
            public boolean onItemLongClick(MediaItem item) {
                CountlyUtils.addEvent(CountlyEvent.LONG_PRESS, "");
                if (currentActionMode == null) {
                    mAdapter.setItemSelected(item, true);
                    ((ActionBarActivity) getActivity()).startSupportActionMode(MediaFragment.this);
                    return true;
                }
                return false;
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        showHiddenFiles = PreferenceUtil.findPreference(getActivity(), R.string.pref_key_show_hidden, false);
        showPath = PreferenceUtil.findPreference(getActivity(), R.string.pref_key_show_path_on_title, false);
        showRealPath = PreferenceUtil.findPreference(getActivity(), R.string.pref_key_show_real_path, false);
        recyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                reloadMediaList();
            }
        }, 100);
    }


    /* implements from ActionMode.Callback */
    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        currentActionMode = actionMode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        resetActionMenus();
        currentActionMode.setTitle(String.valueOf(1));
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        return mediaProvider.onActionItemClicked(getActivity(), this, menuItem);
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        actionMode.getMenu().clear();
        currentActionMode = null;
        mAdapter.clearSelection();
    }
    /* end implement ActionMode.Callback*/

    /* implements from MediaItemViewer */
    @Override
    public void setSelectAll() {
        if (currentActionMode != null && mAdapter.getSelectedCount() != mAdapter.getItemCount()) {
            mAdapter.selectAll();
            currentActionMode.setTitle(String.format("%d", mAdapter.getSelectedCount()));
            resetActionMenus();
        }
    }

    @Override
    public int getSelectedCount() {
        return mAdapter.getSelectedCount();
    }

    @Override
    public List<MediaItem> getSelectedItems() {
        return mAdapter.getSelectedItems();
    }

    @Override
    public void resetActionMenus() {
        if (currentActionMode != null) {
            currentActionMode.getMenu().clear();
            mediaProvider.loadActionMenu(currentActionMode.getMenuInflater(), currentActionMode.getMenu(), mAdapter.getSelectedItems());
        }
    }

    @Override
    public void reloadMediaList() {
        if (currentActionMode != null) {
            currentActionMode.finish();
        }
        loadMediaFromPath(currentPath);
    }

    @Override
    public String getCurrentPath() {
        return currentPath;
    }

    /* end implements MediaItemViewer */


    private MenuItem listModeItem;
    private MenuItem gridModeItem;
    private LayoutMode layoutMode = LayoutMode.Grid;

    enum LayoutMode {
        List, Grid
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_media_fragment, menu);
        listModeItem = menu.findItem(R.id.action_list_mode);
        gridModeItem = menu.findItem(R.id.action_grid_mode);
        gridModeItem.setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            CountlyUtils.addEvent(CountlyEvent.REFRESH, "");
            reloadMediaList();
            return true;
        } else if (item.getItemId() == R.id.action_list_mode) {
            layoutMode = LayoutMode.List;
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
            recyclerView.setLayoutManager(layoutManager);
            listModeItem.setVisible(false);
            gridModeItem.setVisible(true);
            reloadMediaList();
        } else if (item.getItemId() == R.id.action_grid_mode) {
            layoutMode = LayoutMode.Grid;
            RecyclerView.LayoutManager gridManager = new GridLayoutManager(getActivity(), 4, LinearLayoutManager.VERTICAL, false);
            recyclerView.setLayoutManager(gridManager);
            listModeItem.setVisible(true);
            gridModeItem.setVisible(false);
            reloadMediaList();
        }
        return super.onOptionsItemSelected(item);
    }


    private void loadRoot() {
        currentPath = drawerMenu.path;
        loadMediaFromPath(currentPath);
    }

    private void loadFileTree(final MediaItem fileItem) {
        paths.push(fileItem);
        currentPath = fileItem.path;
        loadMediaFromPath(currentPath);
    }

    private void loadMediaFromPath(final String path) {
        if (onOpenFolderListener != null) {
            onOpenFolderListener.onOpenFolder(getCurrentRelativePath());
        }
        new AsyncTask<Void, Void, Void>() {
            private List<MediaItem> mediaList;
            private String msg;

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mediaList = mediaProvider.loadMedia(path, showHiddenFiles);
//                    Collections.sort(mediaList, new Comparator<MediaItem>() {
//                        @Override
//                        public int compare(MediaItem lhs, MediaItem rhs) {
//                            return lhs.type == rhs.type ? 0 : lhs.type == MediaItem.MediaType.Folder ? 1 : -1;
//                        }
//                    });
                } catch (Exception exp) {
                    msg = exp.getMessage();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (!TextUtils.isEmpty(msg)) {
                    Toast.showText(getActivity(), msg);
                }
                if (path.equals(currentPath)) {
                    mAdapter.updateMedias(mediaList);
                }
            }
        }.executeOnExecutor(executor);
    }

    private void loadIcon(final MediaAdapter.MediaItemHolder mediaItemHolder) {
        new AsyncTask<Void, Void, Void>() {
            private Drawable icon;
            private String msg;
            private MediaItem item = mediaItemHolder.getMediaItem();

            @Override
            protected void onPreExecute() {
                if (item.type == MediaItem.MediaType.Folder) {
                    mediaItemHolder.iconView.setImageResource(R.drawable.ic_action_collection);
                } else {
                    mediaItemHolder.iconView.setImageResource(R.drawable.ic_action_file);
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    icon = mediaProvider.loadMediaIcon(item);
                } catch (Exception exp) {
                    msg = exp.getMessage();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (!TextUtils.isEmpty(msg)) {
                    Toast.showText(getActivity(), msg);
                }
                if (item.equals(mediaItemHolder.getMediaItem())) {
                    if (icon != null) {
                        mediaItemHolder.iconView.setImageDrawable(icon);
                    }
                }
            }
        }.executeOnExecutor(executor);
    }

    public boolean back() {
        if (currentActionMode != null) {
            currentActionMode.finish();
            return true;
        }
        if (paths.size() > 1) {
            paths.pop();
            loadFileTree(paths.pop());
            return true;
        } else if (paths.isEmpty()) {
            return false;
        } else {
            paths.pop();
            loadRoot();
            return true;
        }
    }

    public DrawerMenu getDrawerMenu() {
        return drawerMenu;
    }

    @Override
    public void onAttach(Activity activity) {
        if (activity instanceof OnOpenFolderListener) {
            this.onOpenFolderListener = (OnOpenFolderListener) activity;
        }
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        this.onOpenFolderListener = null;
        super.onDetach();
    }

    public String getCurrentRelativePath() {
        if (showPath) {
            String path = currentPath;
            if (!showRealPath) {
                path = path.replaceFirst(drawerMenu.path, "");
            }
            if (TextUtils.isEmpty(path)) {
                return "/";
            } else {
                return path;
            }
        }
        return "";
    }

    class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaItemHolder> {

        class MediaItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
            private final MediaAdapter adapter;
            private final View rootView;
            private final ImageView iconView;
            private final CheckedTextView titleView;
            private final TextView descriptionView;
            private MediaItem mediaItem;


            public MediaItemHolder(View itemView, MediaAdapter adapter) {
                super(itemView);
                this.adapter = adapter;
                this.rootView = itemView;
                iconView = (ImageView) rootView.findViewById(R.id.icon);
                titleView = (CheckedTextView) rootView.findViewById(R.id.title);
                descriptionView = (TextView) rootView.findViewById(R.id.description);
                rootView.setOnClickListener(this);
                rootView.setOnLongClickListener(this);
            }

            public MediaItem getMediaItem() {
                return mediaItem;
            }

            public void setMediaItem(MediaItem mediaItem, boolean selected) {
                this.titleView.setChecked(selected);
                if (!mediaItem.equals(this.mediaItem)) {
                    this.mediaItem = mediaItem;
                    this.titleView.setText(mediaItem.title);
                    if (mediaItem.type == MediaItem.MediaType.File) {
                        this.descriptionView.setText(ComputerUnitUtils.toReadFriendly(mediaItem.length));
                    } else {
                        this.descriptionView.setText(String.format(getString(R.string.description_item_count), mediaItem.childCount));
                    }
                    loadIcon(this);
                }
            }

            @Override
            public void onClick(View v) {
                if (adapter != null && adapter.getOnItemClickListener() != null) {
                    adapter.getOnItemClickListener().onItemClick(getMediaItem());
                }
            }

            @Override
            public boolean onLongClick(View v) {
                return adapter != null && adapter.getOnItemClickListener() != null && adapter.getOnItemClickListener().onItemLongClick(getMediaItem());
            }
        }

        private final Context context;
        private List<MediaItem> mediaList;
        private OnItemClickListener onItemClickListener;
        private ArrayList<MediaItem> selections;

        private HashMap<MediaItem, MediaItemHolder> holderHashMap;

        public MediaAdapter(Context context) {
            this.context = context;
            mediaList = new ArrayList<>();
            selections = new ArrayList<>();
            holderHashMap = new HashMap<>();
        }

        @Override
        public MediaItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(layoutMode == LayoutMode.Grid ? R.layout.media_item_grid : R.layout.media_item_list, parent, false);
            return new MediaItemHolder(view, this);
        }

        @Override
        public void onBindViewHolder(MediaItemHolder holder, int position) {
            holder.setMediaItem(mediaList.get(position), selections.contains(mediaList.get(position)));
            if (holderHashMap.containsValue(holder)) {
                MediaItem oldItem = null;
                for (Map.Entry<MediaItem, MediaItemHolder> entry : holderHashMap.entrySet()) {
                    if (holder.equals(entry.getValue())) {
                        oldItem = entry.getKey();
                        break;
                    }
                }
                if (oldItem != null) {
                    holderHashMap.remove(oldItem);
                }
            }
            holderHashMap.put(mediaList.get(position), holder);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return mediaList.size();
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
        }

        public OnItemClickListener getOnItemClickListener() {
            return onItemClickListener;
        }

        public void updateMedias(List<MediaItem> mediaList) {
            this.mediaList.clear();
            this.selections.clear();
            if (mediaList != null) {
                this.mediaList.addAll(mediaList);
            }
            notifyDataSetChanged();
        }

        public int getSelectedCount() {
            return selections.size();
        }

        public List<MediaItem> getSelectedItems() {
            return selections;
        }

        public void clearSelection() {
            for (MediaItem item : selections) {
                MediaItemHolder holder = holderHashMap.get(item);
                if (holder != null) {
                    holder.setMediaItem(item, false);
                }
            }
            selections.clear();
        }

        public void selectAll() {
            for (MediaItem item : mediaList) {
                if (!selections.contains(item)) {
                    MediaItemHolder holder = holderHashMap.get(item);
                    if (holder != null) {
                        holder.setMediaItem(item, true);
                    }
                    selections.add(item);
                }
            }
        }

        public boolean isSelected(MediaItem item) {
            return selections.contains(item);
        }

        public void setItemSelected(MediaItem item, boolean b) {
            if (b) {
                selections.add(item);
            } else {
                selections.remove(item);
            }
            if (holderHashMap.containsKey(item)) {
                holderHashMap.get(item).setMediaItem(item, b);
            }
        }
    }

    interface OnItemClickListener {
        public void onItemClick(MediaItem item);

        public boolean onItemLongClick(MediaItem item);
    }

    public interface OnOpenFolderListener {
        public void onOpenFolder(String path);
    }
}

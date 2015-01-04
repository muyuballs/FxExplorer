package info.breezes.fxmanager;

import android.app.Activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import info.breezes.ComputerUnitUtils;
import info.breezes.fxmanager.android.app.QAlertDialog;
import info.breezes.fxmanager.dialog.ApkInfoDialog;
import info.breezes.fxmanager.dialog.FileInfoDialog;
import info.breezes.fxmanager.model.DrawerMenu;
import info.breezes.fxmanager.model.MediaItem;
import info.breezes.toolkit.log.Log;
import info.breezes.toolkit.ui.Toast;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class MediaFragment extends Fragment {
    private static final String ARG_DRAWER_MENU = "mediaItems";
    private static final String State_Path_Stack = "_path_stack_";

    private static Executor executor = Executors.newFixedThreadPool(10);

    private OnOpenFolderListener onOpenFolderListener;

    private DrawerMenu drawerMenu;

    private MediaAdapter mAdapter;

    private Stack<MediaItem> paths;

    private MediaProvider mediaProvider;
    private String currentPath;

    private ActionMode currentActionMode;

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
        drawerMenu = (DrawerMenu) getArguments().getSerializable(ARG_DRAWER_MENU);
        if (savedInstanceState != null) {
            Log.d(null, "restore saved state ," + drawerMenu.path);
            paths = (Stack<MediaItem>) savedInstanceState.getSerializable(State_Path_Stack);
        }
        if (paths == null) {
            paths = new Stack<>();
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
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
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
                        reloadActionMenu();
                    }
                } else {
                    if (item.type == MediaItem.MediaType.Folder) {
                        loadFileTree(item);
                    } else if (item.type == MediaItem.MediaType.File) {
                        launch(item);
                    }
                }
            }

            @Override
            public boolean onItemLongClick(MediaItem item) {
                if (currentActionMode == null) {
                    ((ActionBarActivity) getActivity()).startSupportActionMode(new ActionMode.Callback() {
                        @Override
                        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                            currentActionMode = actionMode;
                            actionMode.getMenuInflater().inflate(R.menu.menu_single_item, menu);
                            return true;
                        }

                        @Override
                        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                            return true;
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                            if (menuItem.getItemId() == R.id.action_select_all) {
                                if (mAdapter.getSelectedCount() != mAdapter.getItemCount()) {
                                    mAdapter.selectAll();
                                    actionMode.setTitle(String.format("%d", mAdapter.getSelectedCount()));
                                    reloadActionMenu();
                                }
                            } else if (menuItem.getItemId() == R.id.action_detail) {
                                if (mAdapter.getSelectedCount() > 1) {
                                    Toast.showText(getActivity(), "不能显示多个文件的详情");
                                } else {
                                    showItemDetailInfo();
                                }
                            } else if (menuItem.getItemId() == R.id.action_delete) {
                                deleteMediaItems(mAdapter.getSelectedItems().toArray(new MediaItem[0]));
                            } else if (menuItem.getItemId() == R.id.action_zip) {
                                compressMediaItems(mAdapter.getSelectedItems().toArray(new MediaItem[0]));
                            } else if (menuItem.getItemId() == R.id.action_rename) {
                                renameMediaItem(mAdapter.getSelectedItems().get(0));
                            }
                            return true;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode actionMode) {
                            currentActionMode = null;
                            mAdapter.clearSelection();
                        }
                    });
                }
                return false;
            }
        });
        loadRoot();
        return view;
    }

    private void deleteMediaItems(final MediaItem... items) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("提示");
        builder.setMessage("你确定要删除这些文件么?");
        builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int deleted = MediaItemUtil.delete(true, items);
                if (deleted > 0) {
                    if (deleted < items.length) {
                        Toast.showText(getActivity(), "部分文件删除失败");
                    } else {
                        Toast.showText(getActivity(), "删除成功");
                    }
                    reloadMediaList();
                }
            }
        });
        builder.setPositiveButton("取消", null);
        builder.show();
    }

    private void renameMediaItem(final MediaItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("");
        View content = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_content, null);
        final EditText editText = (EditText) content.findViewById(R.id.editText);
        editText.setText(item.title);
        builder.setView(content);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = editText.getText().toString();
                if (TextUtils.isEmpty(newName)) {
                    Toast.showText(getActivity(), "新文件名不能为空");
                    return;
                }
                dialog.dismiss();
                if (MediaItemUtil.rename(item, newName)) {
                    reloadMediaList();
                } else {
                    Toast.showText(getActivity(), "重命名失败");
                }
            }
        });
        AlertDialog dialog = builder.create();
        QAlertDialog.setAutoDismiss(dialog, false);
        dialog.show();
    }

    private void compressMediaItems(final MediaItem... items) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("提示");
        View content = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_content, null);
        final EditText editText = (EditText) content.findViewById(R.id.editText);
        editText.setHint("目标文件名");
        builder.setView(content);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = editText.getText().toString();
                if (TextUtils.isEmpty(newName)) {
                    Toast.showText(getActivity(), "文件名不能为空");
                    return;
                }
                String out = currentPath + File.separator + newName;
                if (!out.endsWith("\\.zip")) {
                    out += ".zip";
                }
                File file = new File(out);
                if (file.exists()) {
                    Toast.showText(getActivity(), "文件已存在");
                    return;
                }
                MediaItemUtil.compress(out, new MediaItemUtil.OnProgressChangeListener() {
                    private ProgressDialog pd;

                    @Override
                    public void onPreExecute() {
                        pd = new ProgressDialog(getActivity());
                        pd.setCancelable(false);
                        pd.setTitle("正在压缩");
                        pd.setIndeterminate(true);
                        pd.show();
                    }

                    @Override
                    public void onProgressChanged(String file, long max, long current) {
                        pd.setMessage(file);
                    }

                    @Override
                    public void onPostExecute(boolean success) {
                        pd.dismiss();
                        if (success) {
                            Toast.showText(getActivity(), "压缩文件成功");
                            reloadMediaList();
                        } else {
                            Toast.showText(getActivity(), "压缩文件失败");
                        }
                    }
                }, items);
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        QAlertDialog.setAutoDismiss(dialog, false);
        dialog.show();
    }

    private void reloadActionMenu() {
        if (currentActionMode != null) {
            currentActionMode.getMenu().clear();
            currentActionMode.getMenuInflater().inflate(mAdapter.getSelectedCount() > 1 ? R.menu.menu_mutil_item : R.menu.menu_single_item, currentActionMode.getMenu());
        }
    }

    private void reloadMediaList() {
        if (currentActionMode != null) {
            currentActionMode.finish();
        }
        loadMediaFromPath(currentPath);
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
            onOpenFolderListener.onOpenFolder(path.replaceFirst(drawerMenu.path, ""));
        }
        new AsyncTask<Void, Void, Void>() {
            private List<MediaItem> mediaList;
            private String msg;

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mediaList = mediaProvider.loadMedia(path, false);
                    Collections.sort(mediaList, new Comparator<MediaItem>() {
                        @Override
                        public int compare(MediaItem lhs, MediaItem rhs) {
                            return lhs.type == rhs.type ? 0 : lhs.type == MediaItem.MediaType.Folder ? 1 : -1;
                        }
                    });
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

    private void launch(MediaItem item) {
        try {
            String extension = MimeTypeMap.getFileExtensionFromUrl(item.path);
            if ("apk".equalsIgnoreCase(extension)) {
                ApkInfoDialog.showApkInfoDialog(getActivity(), item);
            } else {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(android.content.Intent.ACTION_VIEW);
                Uri uri = Uri.fromFile(new File(item.path));
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(item.path));
                Log.d(null, "MimeType:" + mimeType);
                intent.setDataAndType(uri, TextUtils.isEmpty(mimeType) ? "*/*" : mimeType);
                startActivity(intent);
            }
        } catch (Exception exp) {
            Toast.showText(getActivity(), exp.getMessage());
        }
    }

    private void showItemDetailInfo() {
        MediaItem mediaItem = mAdapter.getSelectedItems().get(0);
        if (mediaItem.type == MediaItem.MediaType.File) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(mediaItem.path);
            if ("apk".equalsIgnoreCase(extension)) {
                ApkInfoDialog.showApkInfoDialog(getActivity(), mediaItem);
            } else {
                FileInfoDialog.showSingleFileInfoDialog(getActivity(), mediaItem, mediaProvider, true);
            }
        } else {
            FileInfoDialog.showSingleFolderInfoDialog(getActivity(), mediaItem, mediaProvider);
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
                        this.descriptionView.setText(String.format("%d个项目", mediaItem.childCount));
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
            View view = LayoutInflater.from(context).inflate(R.layout.media_item_list, parent, false);
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

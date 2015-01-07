package info.breezes.fxmanager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

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
import info.breezes.PreferenceUtil;
import info.breezes.fxmanager.android.app.QAlertDialog;
import info.breezes.fxmanager.countly.CountlyEvent;
import info.breezes.fxmanager.countly.CountlyFragment;
import info.breezes.fxmanager.countly.CountlyUtils;
import info.breezes.fxmanager.dialog.ApkInfoDialog;
import info.breezes.fxmanager.dialog.FileInfoDialog;
import info.breezes.fxmanager.model.DrawerMenu;
import info.breezes.fxmanager.model.MediaItem;
import info.breezes.fxmanager.service.FileService;
import info.breezes.toolkit.log.Log;
import info.breezes.toolkit.ui.Toast;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class MediaFragment extends CountlyFragment {
    private static final String ARG_DRAWER_MENU = "mediaItems";
    private static final String State_Path_Stack = "_path_stack_";
    public static final String EXTRA_INIT_DIR = "info.breezes.fx.extra.INIT_DIR";
    public static final String EXTRA_DIR_NAME = "info.breezes.fx.extra.EXTRA_DIR_NAME";

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
                CountlyUtils.addEvent(CountlyEvent.LONG_PRESS, "");
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
                                CountlyUtils.addEvent(CountlyEvent.SELECT_ALL, "");
                                if (mAdapter.getSelectedCount() != mAdapter.getItemCount()) {
                                    mAdapter.selectAll();
                                    actionMode.setTitle(String.format("%d", mAdapter.getSelectedCount()));
                                    reloadActionMenu();
                                }
                            } else if (menuItem.getItemId() == R.id.action_detail) {
                                CountlyUtils.addEvent(CountlyEvent.OPEN_DETAIL, "");
                                if (mAdapter.getSelectedCount() > 1) {
                                    Toast.showText(getActivity(), getString(R.string.tip_cannt_show_multi_detail));
                                } else {
                                    showItemDetailInfo();
                                }
                            } else if (menuItem.getItemId() == R.id.action_delete) {
                                CountlyUtils.addEvent(CountlyEvent.DELETE, "");
                                deleteMediaItems(mAdapter.getSelectedItems());
                            } else if (menuItem.getItemId() == R.id.action_zip) {
                                CountlyUtils.addEvent(CountlyEvent.COMPRESS, "");
                                compressMediaItems(mAdapter.getSelectedItems());
                            } else if (menuItem.getItemId() == R.id.action_rename) {
                                CountlyUtils.addEvent(CountlyEvent.RENAME, "");
                                renameMediaItem(mAdapter.getSelectedItems().get(0));
                            } else if (menuItem.getItemId() == R.id.action_add_bookmark) {
                                CountlyUtils.addEvent(CountlyEvent.PIN_START, "");
                                pinToStart(mAdapter.getSelectedItems().get(0));
                            } else if (menuItem.getItemId() == R.id.action_qrcode) {
                                showQrCode(mAdapter.getSelectedItems().get(0));
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
        return view;
    }

    private void showQrCode(final MediaItem item) {
        new AsyncTask<Void, Void, Void>() {
            private Dialog dialog;
            private ImageView imageView;

            @Override
            protected void onPreExecute() {
                imageView = new ImageView(getActivity());
                ProgressBar pd = new ProgressBar(getActivity());
                pd.setIndeterminate(true);
                dialog = new Dialog(getActivity(), R.style.Dialog_NoTitle);
                dialog.setContentView(pd);
                dialog.show();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                dialog.setContentView(imageView);
            }

            @Override
            protected Void doInBackground(Void... params) {
                String path = FileService.startServeFile(getActivity(), item.path, 0);
                try {
                    QRCodeWriter writer = new QRCodeWriter();
                    BitMatrix matrix = writer.encode(path, BarcodeFormat.QR_CODE, 512, 512);
                    Bitmap bitmap = Bitmap.createBitmap(matrix.getWidth(), matrix.getHeight(), Bitmap.Config.RGB_565);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawColor(Color.WHITE);
                    Paint paint = new Paint();
                    TypedArray array = getActivity().getTheme().obtainStyledAttributes(R.styleable.Theme);
                    paint.setColor(array.getColor(R.styleable.Theme_colorPrimary, Color.BLACK));
                    array.recycle();
                    for (int i = 0; i < matrix.getHeight(); i++) {
                        for (int x = 0; x < matrix.getWidth(); x++) {
                            if (matrix.get(x, i)) {
                                canvas.drawPoint(x, i, paint);
                            }
                        }
                    }
                    imageView.setImageBitmap(bitmap);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.executeOnExecutor(executor);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_media_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            CountlyUtils.addEvent(CountlyEvent.REFRESH, "");
            reloadMediaList();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    private void pinToStart(MediaItem item) {
        Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        //快捷方式的名称
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, item.title);
        shortcut.putExtra("duplicate", false); //不允许重复创建
        Intent innerIntent = new Intent(getActivity(), MainActivity.class);
        innerIntent.putExtra(EXTRA_INIT_DIR, item.path);
        innerIntent.putExtra(EXTRA_DIR_NAME, item.title);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, innerIntent);
        //快捷方式的图标
        Intent.ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(getActivity(), R.drawable.ic_action_collection);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        getActivity().sendBroadcast(shortcut);
        Toast.showText(getActivity(), String.format(getString(R.string.tip_pin_start_ok), item.title))
        ;
    }

    private void deleteMediaItems(final List<MediaItem> items) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.setMessage(getString(R.string.dialog_msg_are_you_confirm_delete_them));
        builder.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int deleted = MediaItemUtil.delete(true, items);
                if (deleted > 0) {
                    if (deleted < items.size()) {
                        Toast.showText(getActivity(), getString(R.string.tip_part_of_delete_ok));
                    } else {
                        Toast.showText(getActivity(), getString(R.string.tip_delete_ok));
                    }
                    reloadMediaList();
                }
            }
        });
        builder.setPositiveButton(android.R.string.cancel, null);
        builder.show();
    }

    private void renameMediaItem(final MediaItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("");
        @SuppressLint("InflateParams")
        View content = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_content, null);
        final EditText editText = (EditText) content.findViewById(R.id.editText);
        editText.setText(item.title);
        builder.setView(content);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = editText.getText().toString();
                if (TextUtils.isEmpty(newName)) {
                    Toast.showText(getActivity(), getString(R.string.tip_file_name_cannt_null));
                    return;
                }
                dialog.dismiss();
                if (MediaItemUtil.rename(item, newName)) {
                    reloadMediaList();
                } else {
                    Toast.showText(getActivity(), getString(R.string.tip_rename_failed));
                }
            }
        });
        AlertDialog dialog = builder.create();
        QAlertDialog.setAutoDismiss(dialog, false);
        dialog.show();
    }

    private void compressMediaItems(final List<MediaItem> items) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_title_tip));
        @SuppressLint("InflateParams")
        View content = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_content, null);
        final EditText editText = (EditText) content.findViewById(R.id.editText);
        editText.setHint(getString(R.string.hint_target_file_name));
        builder.setView(content);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = editText.getText().toString();
                if (TextUtils.isEmpty(newName)) {
                    Toast.showText(getActivity(), getString(R.string.tip_file_name_cannt_null));
                    return;
                }
                String out = currentPath + File.separator + newName;
                if (!out.endsWith("\\.zip")) {
                    out += ".zip";
                }
                File file = new File(out);
                if (file.exists()) {
                    Toast.showText(getActivity(), getString(R.string.tip_file_already_exists));
                    return;
                }
                MediaItemUtil.compress(out, new MediaItemUtil.OnProgressChangeListener() {
                    private ProgressDialog pd;

                    @Override
                    public void onPreExecute() {
                        pd = new ProgressDialog(getActivity());
                        pd.setCancelable(false);
                        pd.setTitle(getString(R.string.dialog_title_compressing));
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
                            Toast.showText(getActivity(), getString(R.string.tip_compress_ok));
                            reloadMediaList();
                        } else {
                            Toast.showText(getActivity(), getString(R.string.tip_compress_failed));
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
            if (mAdapter.getSelectedItems().get(0).type == MediaItem.MediaType.File) {
                MenuItem item = currentActionMode.getMenu().findItem(R.id.action_add_bookmark);
                if (item != null) {
                    item.setEnabled(false);
                }
            }
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

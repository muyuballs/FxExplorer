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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import info.breezes.fxmanager.model.DrawerMenu;

/**
 * menu adapter
 * Created by admin on 2014/12/30.
 */
public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.DrawerMenuHolder> {

    public static class DrawerMenuHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final View itemView;
        private final TextView textView;
        private final ImageView iconView;
        private final MenuAdapter adapter;
        private DrawerMenu drawMenu;

        public DrawerMenuHolder(View itemView, MenuAdapter adapter) {
            super(itemView);
            this.adapter = adapter;
            this.itemView = itemView;
            this.textView = (TextView) itemView.findViewById(R.id.textView);
            this.iconView = (ImageView) itemView.findViewById(R.id.icon);
            this.itemView.setOnClickListener(this);
        }

        public void setDrawMenu(DrawerMenu drawMenu) {
            this.drawMenu = drawMenu;
            textView.setText(drawMenu.title);
            iconView.setImageDrawable(drawMenu.icon);
        }

        public DrawerMenu getDrawMenu() {
            return drawMenu;
        }

        @Override
        public void onClick(View v) {
            if (adapter != null && adapter.getOnItemClickListener() != null) {
                adapter.getOnItemClickListener().onItemClick(getDrawMenu());
            }
        }
    }

    private final Context context;
    private DrawerMenu[] menus;

    private OnItemClickListener onItemClickListener;

    public MenuAdapter(Context context, DrawerMenu[] menus) {
        this.context = context;
        this.menus = menus;
    }

    public void update(DrawerMenu[] drawerMenus) {
        this.menus = drawerMenus;
        notifyDataSetChanged();
    }

    @Override
    public DrawerMenuHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.drawer_menu_item, viewGroup, false);
        return new DrawerMenuHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(DrawerMenuHolder drawerMenuHolder, int i) {
        DrawerMenu menu = menus[i];
        drawerMenuHolder.setDrawMenu(menu);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return menus == null ? 0 : menus.length;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public OnItemClickListener getOnItemClickListener() {
        return onItemClickListener;
    }

    public interface OnItemClickListener {
        public void onItemClick(DrawerMenu item);
    }
}

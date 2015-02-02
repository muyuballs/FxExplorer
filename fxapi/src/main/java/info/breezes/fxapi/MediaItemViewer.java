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

package info.breezes.fxapi;


import java.util.List;


public interface MediaItemViewer {
    public static final String EXTRA_INIT_DIR = "info.breezes.fx.extra.INIT_DIR";
    public static final String EXTRA_DIR_NAME = "info.breezes.fx.extra.EXTRA_DIR_NAME";

    void setSelectAll();

    int getSelectedCount();

    List<MediaItem> getSelectedItems();

    void resetActionMenus();

    void reloadMediaList();

    String getCurrentPath();
}

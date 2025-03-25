/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.tool;

import android.content.Context;
import android.preference.PreferenceManager;

import net.micode.notes.R;
import net.micode.notes.ui.NotesPreferenceActivity;
/**
 * 资源解析工具类
 * 功能：统一管理笔记应用的UI资源映射关系
 */
public class ResourceParser {

    //颜色常量定义，对应五中主题色
    public static final int YELLOW           = 0; // 黄色主题
    public static final int BLUE             = 1; // 蓝色主题
    public static final int WHITE            = 2; // 白色主题
    public static final int GREEN            = 3; // 绿色主题
    public static final int RED              = 4; // 红色主题

    //默认颜色为黄色
    public static final int BG_DEFAULT_COLOR = YELLOW;

    //字体大小定义
    public static final int TEXT_SMALL       = 0; // 小号字体
    public static final int TEXT_MEDIUM      = 1; // 中号字体
    public static final int TEXT_LARGE       = 2; // 大号字体
    public static final int TEXT_SUPER       = 3; // 特大字体

    //默认字体大小为中号
    public static final int BG_DEFAULT_FONT_SIZE = TEXT_MEDIUM;

    /**
     * 笔记编辑界面背景资源映射类
     */
    public static class NoteBgResources {
        // 笔记内容区域背景资源数组
        private final static int [] BG_EDIT_RESOURCES = new int [] {
            R.drawable.edit_yellow, // 黄色背景
            R.drawable.edit_blue, // 蓝色背景
            R.drawable.edit_white, // 白色背景
            R.drawable.edit_green, // 绿色背景
            R.drawable.edit_red // 红色背景
        };

        // 笔记标题区域背景资源数组
        private final static int [] BG_EDIT_TITLE_RESOURCES = new int [] {
            R.drawable.edit_title_yellow,
            R.drawable.edit_title_blue,
            R.drawable.edit_title_white,
            R.drawable.edit_title_green,
            R.drawable.edit_title_red
        };

        /** 获取笔记内容背景资源ID */
        public static int getNoteBgResource(int id) {
            return BG_EDIT_RESOURCES[id];
        }

        /** 获取笔记标题背景资源ID */
        public static int getNoteTitleBgResource(int id) {
            return BG_EDIT_TITLE_RESOURCES[id];
        }
    }
    /**
     * 获取默认背景ID
     * @param context Android上下文
     * @return 背景资源索引ID
     */
    public static int getDefaultBgId(Context context) {
        // 检查用户是否启用随机背景功能
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                NotesPreferenceActivity.PREFERENCE_SET_BG_COLOR_KEY, false)) {
            // 随机返回一个背景索引
            return (int) (Math.random() * NoteBgResources.BG_EDIT_RESOURCES.length);
        } else {
            return BG_DEFAULT_COLOR; // 返回默认背景索引
        }
    }
    /**
     * 笔记列表项背景资源映射类
     */
    public static class NoteItemBgResources {
        private final static int [] BG_FIRST_RESOURCES = new int [] {
            R.drawable.list_yellow_up,
            R.drawable.list_blue_up,
            R.drawable.list_white_up,
            R.drawable.list_green_up,
            R.drawable.list_red_up
        };

        // 列表中间项背景资源
        private final static int [] BG_NORMAL_RESOURCES = new int [] {
            R.drawable.list_yellow_middle,
            R.drawable.list_blue_middle,
            R.drawable.list_white_middle,
            R.drawable.list_green_middle,
            R.drawable.list_red_middle
        };

        // 列表最后一项背景资源
        private final static int [] BG_LAST_RESOURCES = new int [] {
            R.drawable.list_yellow_down,
            R.drawable.list_blue_down,
            R.drawable.list_white_down,
            R.drawable.list_green_down,
            R.drawable.list_red_down,
        };

        // 单一项背景资源（当列表只有一项时使用）
        private final static int [] BG_SINGLE_RESOURCES = new int [] {
            R.drawable.list_yellow_single,
            R.drawable.list_blue_single,
            R.drawable.list_white_single,
            R.drawable.list_green_single,
            R.drawable.list_red_single
        };

        /** 获取列表首项背景资源 */
        public static int getNoteBgFirstRes(int id) {
            return BG_FIRST_RESOURCES[id];
        }

        /** 获取列表末项背景资源 */
        public static int getNoteBgLastRes(int id) {
            return BG_LAST_RESOURCES[id];
        }

        /** 获取单项背景资源 */
        public static int getNoteBgSingleRes(int id) {
            return BG_SINGLE_RESOURCES[id];
        }

        /** 获取普通项背景资源 */
        public static int getNoteBgNormalRes(int id) {
            return BG_NORMAL_RESOURCES[id];
        }

        /** 获取文件夹项背景资源 */
        public static int getFolderBgRes() {
            return R.drawable.list_folder;
        }
    }

    /**
     * 桌面小部件背景资源映射类
     */
    public static class WidgetBgResources {
        // 2x尺寸小部件背景资源
        private final static int [] BG_2X_RESOURCES = new int [] {
            R.drawable.widget_2x_yellow,
            R.drawable.widget_2x_blue,
            R.drawable.widget_2x_white,
            R.drawable.widget_2x_green,
            R.drawable.widget_2x_red,
        };

        /** 获取2x小部件背景资源 */
        public static int getWidget2xBgResource(int id) {
            return BG_2X_RESOURCES[id];
        }

        // 4x尺寸小部件背景资源
        private final static int [] BG_4X_RESOURCES = new int [] {
            R.drawable.widget_4x_yellow,
            R.drawable.widget_4x_blue,
            R.drawable.widget_4x_white,
            R.drawable.widget_4x_green,
            R.drawable.widget_4x_red
        };

        /** 获取4x小部件背景资源 */
        public static int getWidget4xBgResource(int id) {
            return BG_4X_RESOURCES[id];
        }
    }

    /**
     * 文字外观资源映射类
     */
    public static class TextAppearanceResources {
        // 文字样式资源数组
        private final static int [] TEXTAPPEARANCE_RESOURCES = new int [] {
            R.style.TextAppearanceNormal, // 常规样式
            R.style.TextAppearanceMedium, // 中等样式
            R.style.TextAppearanceLarge, // 大号样式
            R.style.TextAppearanceSuper  // 特大样式
        };

        /**
         * 获取文字样式资源ID
         * @param id 样式索引
         * @return 样式资源ID（索引越界时返回默认值）
         */
        public static int getTexAppearanceResource(int id) {
            /**
             * HACKME: Fix bug of store the resource id in shared preference.
             * The id may larger than the length of resources, in this case,
             * return the {@link ResourceParser#BG_DEFAULT_FONT_SIZE}
             */
            if (id >= TEXTAPPEARANCE_RESOURCES.length) {// 防止SharedPreferences存储的索引值越界
                return BG_DEFAULT_FONT_SIZE;
            }
            return TEXTAPPEARANCE_RESOURCES[id];
        }

        /** 获取可用文字样式数量 */
        public static int getResourcesSize() {
            return TEXTAPPEARANCE_RESOURCES.length;
        }
    }
}

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

 package net.micode.notes.ui;

 import android.content.Context;
 import android.database.Cursor;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.CursorAdapter;
 import android.widget.LinearLayout;
 import android.widget.TextView;
 
 import net.micode.notes.R;
 import net.micode.notes.data.Notes;
 import net.micode.notes.data.Notes.NoteColumns;
 
 // 自定义的文件夹列表适配器
 public class FoldersListAdapter extends CursorAdapter {
     // 定义查询数据库时需要的字段
     public static final String [] PROJECTION = {
             NoteColumns.ID, // 文件夹的ID
             NoteColumns.SNIPPET // 文件夹的名称（或摘要）
     };
 
     // 定义字段在查询结果中的索引
     public static final int ID_COLUMN   = 0; // ID字段的索引
     public static final int NAME_COLUMN = 1; // 名称字段的索引
 
     // 构造函数
     public FoldersListAdapter(Context context, Cursor c) {
         super(context, c);
     }
 
     // 创建新的视图
     @Override
     public View newView(Context context, Cursor cursor, ViewGroup parent) {
         // 返回一个新的文件夹列表项视图
         return new FolderListItem(context);
     }
 
     // 绑定视图
     @Override
     public void bindView(View view, Context context, Cursor cursor) {
         // 如果视图是文件夹列表项
         if (view instanceof FolderListItem) {
             // 如果是根文件夹，显示特定的字符串，否则显示文件夹名称
             String folderName = (cursor.getLong(ID_COLUMN) == Notes.ID_ROOT_FOLDER) ? context
                     .getString(R.string.menu_move_parent_folder) : cursor.getString(NAME_COLUMN);
             // 绑定文件夹名称到视图
             ((FolderListItem) view).bind(folderName);
         }
     }
 
     // 获取指定位置的文件夹名称
     public String getFolderName(Context context, int position) {
         // 获取指定位置的游标
         Cursor cursor = (Cursor) getItem(position);
         // 如果是根文件夹，返回特定的字符串，否则返回文件夹名称
         return (cursor.getLong(ID_COLUMN) == Notes.ID_ROOT_FOLDER) ? context
                 .getString(R.string.menu_move_parent_folder) : cursor.getString(NAME_COLUMN);
     }
 
     // 文件夹列表项的内部类
     private class FolderListItem extends LinearLayout {
         private TextView mName; // 用于显示文件夹名称的TextView
 
         // 构造函数
         public FolderListItem(Context context) {
             super(context);
             // 加载布局
             inflate(context, R.layout.folder_list_item, this);
             // 获取文件夹名称的TextView
             mName = (TextView) findViewById(R.id.tv_folder_name);
         }
 
         // 绑定数据到视图
         public void bind(String name) {
             // 设置文件夹名称
             mName.setText(name);
         }
     }
 }
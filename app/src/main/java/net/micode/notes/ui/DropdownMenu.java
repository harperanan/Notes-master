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
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.widget.Button;
 import android.widget.PopupMenu;
 import android.widget.PopupMenu.OnMenuItemClickListener;
 
 import net.micode.notes.R;
 
 // 自定义的下拉菜单类
 public class DropdownMenu {
     // 按钮控件，用于显示下拉菜单
     private Button mButton;
     // 弹出式菜单
     private PopupMenu mPopupMenu;
     // 菜单对象
     private Menu mMenu;
 
     // 构造函数
     public DropdownMenu(Context context, Button button, int menuId) {
         // 保存按钮控件
         mButton = button;
         // 设置按钮的背景图标
         mButton.setBackgroundResource(R.drawable.dropdown_icon);
         // 创建弹出式菜单
         mPopupMenu = new PopupMenu(context, mButton);
         // 获取菜单对象
         mMenu = mPopupMenu.getMenu();
         // 加载菜单资源
         mPopupMenu.getMenuInflater().inflate(menuId, mMenu);
         // 设置按钮的点击事件监听器
         mButton.setOnClickListener(new OnClickListener() {
             public void onClick(View v) {
                 // 显示弹出式菜单
                 mPopupMenu.show();
             }
         });
     }
 
     // 设置下拉菜单项点击事件的监听器
     public void setOnDropdownMenuItemClickListener(OnMenuItemClickListener listener) {
         // 如果弹出式菜单不为空，则设置监听器
         if (mPopupMenu != null) {
             mPopupMenu.setOnMenuItemClickListener(listener);
         }
     }
 
     // 查找菜单项
     public MenuItem findItem(int id) {
         // 在菜单中查找指定 ID 的菜单项
         return mMenu.findItem(id);
     }
 
     // 设置按钮的标题
     public void setTitle(CharSequence title) {
         // 设置按钮的文本
         mButton.setText(title);
     }
 }
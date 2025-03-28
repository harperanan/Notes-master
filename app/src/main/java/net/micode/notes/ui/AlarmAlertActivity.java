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

 // 导入必要的 Android 类库
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.DialogInterface.OnClickListener;
 import android.content.DialogInterface.OnDismissListener;
 import android.content.Intent;
 import android.media.AudioManager;
 import android.media.MediaPlayer;
 import android.media.RingtoneManager;
 import android.net.Uri;
 import android.os.Bundle;
 import android.os.PowerManager;
 import android.provider.Settings;
 import android.view.Window;
 import android.view.WindowManager;
 
 import net.micode.notes.R;
 import net.micode.notes.data.Notes;
 import net.micode.notes.tool.DataUtils;
 
 import java.io.IOException;
 
 // 定义一个名为 AlarmAlertActivity 的类，继承自 Activity，实现 OnClickListener 和 OnDismissListener 接口
 public class AlarmAlertActivity extends Activity implements OnClickListener, OnDismissListener {
     private long mNoteId; // 用于存储笔记的 ID
     private String mSnippet; // 用于存储笔记的摘要
     private static final int SNIPPET_PREW_MAX_LEN = 60; // 定义摘要的最大长度
     MediaPlayer mPlayer; // 用于播放闹钟声音的 MediaPlayer 对象
 
     // 在 Activity 创建时执行的代码
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         requestWindowFeature(Window.FEATURE_NO_TITLE); // 去掉标题栏
 
         final Window win = getWindow();
         win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED); // 确保在锁屏状态下也能显示
 
         // 如果屏幕未开启，则设置一些标志以确保屏幕开启并保持亮起
         if (!isScreenOn()) {
             win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                     | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                     | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                     | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
         }
 
         Intent intent = getIntent(); // 获取启动该 Activity 的 Intent
 
         // 从 Intent 中获取笔记的 ID 和摘要
         try {
             mNoteId = Long.valueOf(intent.getData().getPathSegments().get(1));
             mSnippet = DataUtils.getSnippetById(this.getContentResolver(), mNoteId);
             // 如果摘要长度超过最大长度，则截取前部分并添加省略号
             mSnippet = mSnippet.length() > SNIPPET_PREW_MAX_LEN ? mSnippet.substring(0,
                     SNIPPET_PREW_MAX_LEN) + getResources().getString(R.string.notelist_string_info)
                     : mSnippet;
         } catch (IllegalArgumentException e) {
             e.printStackTrace(); // 如果发生异常，打印堆栈信息
             return; // 并退出方法
         }
 
         mPlayer = new MediaPlayer(); // 创建 MediaPlayer 实例
         // 如果笔记在数据库中可见，则显示操作对话框并播放闹钟声音
         if (DataUtils.visibleInNoteDatabase(getContentResolver(), mNoteId, Notes.TYPE_NOTE)) {
             showActionDialog();
             playAlarmSound();
         } else {
             finish(); // 如果笔记不可见，则直接结束 Activity
         }
     }
 
     // 检查屏幕是否开启
     private boolean isScreenOn() {
         PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
         return pm.isScreenOn();
     }
 
     // 播放闹钟声音
     private void playAlarmSound() {
         Uri url = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM); // 获取默认的闹钟铃声 URI
 
         // 获取静音模式下受影响的音频流
         int silentModeStreams = Settings.System.getInt(getContentResolver(),
                 Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
 
         // 根据静音模式设置音频流类型
         if ((silentModeStreams & (1 << AudioManager.STREAM_ALARM)) != 0) {
             mPlayer.setAudioStreamType(silentModeStreams);
         } else {
             mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
         }
         try {
             mPlayer.setDataSource(this, url); // 设置音频源
             mPlayer.prepare(); // 准备播放
             mPlayer.setLooping(true); // 设置循环播放
             mPlayer.start(); // 开始播放
         } catch (IllegalArgumentException e) {
             e.printStackTrace(); // 捕获异常并打印堆栈信息
         } catch (SecurityException e) {
             e.printStackTrace();
         } catch (IllegalStateException e) {
             e.printStackTrace();
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
 
     // 显示操作对话框
     private void showActionDialog() {
         AlertDialog.Builder dialog = new AlertDialog.Builder(this);
         dialog.setTitle(R.string.app_name); // 设置对话框标题
         dialog.setMessage(mSnippet); // 设置对话框内容为笔记摘要
         dialog.setPositiveButton(R.string.notealert_ok, this); // 添加“确定”按钮
         if (isScreenOn()) {
             dialog.setNegativeButton(R.string.notealert_enter, this); // 如果屏幕开启，添加“进入”按钮
         }
         dialog.show().setOnDismissListener(this); // 显示对话框并设置关闭监听器
     }
 
     // 点击对话框按钮时的回调方法
     public void onClick(DialogInterface dialog, int which) {
         switch (which) {
             case DialogInterface.BUTTON_NEGATIVE:
                 // 如果点击“进入”按钮，启动 NoteEditActivity 并传递笔记 ID
                 Intent intent = new Intent(this, NoteEditActivity.class);
                 intent.setAction(Intent.ACTION_VIEW);
                 intent.putExtra(Intent.EXTRA_UID, mNoteId);
                 startActivity(intent);
                 break;
             default:
                 break;
         }
     }
 
     // 对话框关闭时的回调方法
     public void onDismiss(DialogInterface dialog) {
         stopAlarmSound(); // 停止闹钟声音
         finish(); // 结束 Activity
     }
 
     // 停止闹钟声音
     private void stopAlarmSound() {
         if (mPlayer != null) {
             mPlayer.stop(); // 停止播放
             mPlayer.release(); // 释放资源
             mPlayer = null; // 将 MediaPlayer 对象置为 null
         }
     }
 }
 
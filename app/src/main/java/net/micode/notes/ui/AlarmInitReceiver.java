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
 import android.app.AlarmManager;
 import android.app.PendingIntent;
 import android.content.BroadcastReceiver;
 import android.content.ContentUris;
 import android.content.Context;
 import android.content.Intent;
 import android.database.Cursor;
 
 import net.micode.notes.data.Notes;
 import net.micode.notes.data.Notes.NoteColumns;
 
 // 定义一个名为 AlarmInitReceiver 的类，继承自 BroadcastReceiver
 // 该类用于初始化闹钟提醒，当应用启动时，它会检查所有未触发的闹钟并重新设置它们
 public class AlarmInitReceiver extends BroadcastReceiver {
 
     // 定义查询笔记数据库时需要的字段
     private static final String [] PROJECTION = new String [] {
             NoteColumns.ID, // 笔记的 ID
             NoteColumns.ALERTED_DATE // 笔记的提醒时间
     };
 
     // 定义字段在查询结果中的索引
     private static final int COLUMN_ID                = 0;
     private static final int COLUMN_ALERTED_DATE      = 1;
 
     // 当接收到广播时执行的代码
     @Override
     public void onReceive(Context context, Intent intent) {
         // 获取当前时间的毫秒值
         long currentDate = System.currentTimeMillis();
 
         // 查询笔记数据库，获取所有未触发的闹钟提醒
         // 查询条件为提醒时间大于当前时间，且类型为笔记
         Cursor c = context.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                 PROJECTION, // 查询的字段
                 NoteColumns.ALERTED_DATE + ">? AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE, // 查询条件
                 new String[] { String.valueOf(currentDate) }, // 查询条件的参数
                 null); // 不对结果排序
 
         // 如果查询结果不为空
         if (c != null) {
             // 如果查询结果中有数据
             if (c.moveToFirst()) {
                 do {
                     // 获取当前笔记的提醒时间
                     long alertDate = c.getLong(COLUMN_ALERTED_DATE);
 
                     // 创建一个 Intent，用于触发闹钟提醒
                     Intent sender = new Intent(context, AlarmReceiver.class);
                     // 设置 Intent 的数据为当前笔记的 URI
                     sender.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, c.getLong(COLUMN_ID)));
 
                     // 创建一个 PendingIntent，用于在闹钟触发时发送广播
                     PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, sender, 0);
 
                     // 获取系统的 AlarmManager 服务
                     AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
 
                     // 设置闹钟提醒，当到达指定时间时触发 PendingIntent
                     alarmManager.set(AlarmManager.RTC_WAKEUP, alertDate, pendingIntent);
                 } while (c.moveToNext()); // 遍历查询结果，为每个未触发的闹钟设置提醒
             }
             c.close(); // 关闭 Cursor，释放资源
         }
     }
 }
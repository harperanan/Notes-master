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

 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 
 // 定义一个名为 AlarmReceiver 的类，继承自 BroadcastReceiver
 // 该类用于接收闹钟提醒的广播，并启动 AlarmAlertActivity 来显示提醒通知
 public class AlarmReceiver extends BroadcastReceiver {
     @Override
     public void onReceive(Context context, Intent intent) {
         // 将接收到的 Intent 的目标类设置为 AlarmAlertActivity
         // 这样当 Intent 被启动时，会启动 AlarmAlertActivity
         intent.setClass(context, AlarmAlertActivity.class);
 
         // 添加标志，表示启动一个新的任务
         // 这是因为 AlarmAlertActivity 通常需要独立于其他 Activity 运行
         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 
         // 使用 context 启动 AlarmAlertActivity
         // 这将显示闹钟提醒的界面
         context.startActivity(intent);
     }
 }
 
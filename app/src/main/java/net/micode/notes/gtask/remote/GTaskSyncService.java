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

package net.micode.notes.gtask.remote;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;


/**
 * Google Tasks 同步后台服务
 * 功能：处理与Google Tasks服务器的双向数据同步
 */

public class GTaskSyncService extends Service {
    // 同步动作类型标识常量
    public final static String ACTION_STRING_NAME = "sync_action_type";

    // 启动同步
    public final static int ACTION_START_SYNC = 0;

    // 取消同步
    public final static int ACTION_CANCEL_SYNC = 1;

    // 无效动作
    public final static int ACTION_INVALID = 2;

    // 广播相关常量
    public final static String GTASK_SERVICE_BROADCAST_NAME = "net.micode.notes.gtask.remote.gtask_sync_service";

    public final static String GTASK_SERVICE_BROADCAST_IS_SYNCING = "isSyncing";

    public final static String GTASK_SERVICE_BROADCAST_PROGRESS_MSG = "progressMsg";

    // 静态同步任务实例
    private static GTaskASyncTask mSyncTask = null;

    // 同步进度信息
    private static String mSyncProgress = "";

    /**
     * 启动同步任务
     */
    private void startSync() {
        if (mSyncTask == null) {
            mSyncTask = new GTaskASyncTask(this, new GTaskASyncTask.OnCompleteListener() {
                public void onComplete() {
                    mSyncTask = null;
                    sendBroadcast(""); // 发送空进度表示完成
                    stopSelf(); // 同步完成自动停止服务
                }
            });
            sendBroadcast("");// 初始化广播
            mSyncTask.execute();// 启动异步任务
        }
    }

    /**
     * 取消同步任务
     */
    private void cancelSync() {
        if (mSyncTask != null) {
            mSyncTask.cancelSync();
        }
    }

    @Override
    public void onCreate() {
        mSyncTask = null;
    }// 初始化时确保任务为空

    /**
     * 服务启动命令处理
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey(ACTION_STRING_NAME)) {
            switch (bundle.getInt(ACTION_STRING_NAME, ACTION_INVALID)) {
                case ACTION_START_SYNC:
                    startSync();
                    break;
                case ACTION_CANCEL_SYNC:
                    cancelSync();
                    break;
                default:
                    break;
            }
            return START_STICKY;// 服务被终止后自动重启
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onLowMemory() {
        if (mSyncTask != null) {
            mSyncTask.cancelSync();// 内存不足时取消同步
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }// 不提供绑定服务

    /**
     * 发送同步状态广播
     * @param msg 当前进度信息
     */
    public void sendBroadcast(String msg) {
        mSyncProgress = msg;
        Intent intent = new Intent(GTASK_SERVICE_BROADCAST_NAME);
        intent.putExtra(GTASK_SERVICE_BROADCAST_IS_SYNCING, mSyncTask != null);
        intent.putExtra(GTASK_SERVICE_BROADCAST_PROGRESS_MSG, msg);
        sendBroadcast(intent);
    }

    //静态工具方法
    /**
     * 从Activity启动同步
     * @param activity 调用方Activity
     */
    public static void startSync(Activity activity) {
        GTaskManager.getInstance().setActivityContext(activity);
        Intent intent = new Intent(activity, GTaskSyncService.class);
        intent.putExtra(GTaskSyncService.ACTION_STRING_NAME, GTaskSyncService.ACTION_START_SYNC);
        activity.startService(intent);
    }

    /**
     * 取消当前同步
     * @param context 上下文对象
     */
    public static void cancelSync(Context context) {
        Intent intent = new Intent(context, GTaskSyncService.class);
        intent.putExtra(GTaskSyncService.ACTION_STRING_NAME, GTaskSyncService.ACTION_CANCEL_SYNC);
        context.startService(intent);
    }

    /**
     * 检查是否正在同步
     */
    public static boolean isSyncing() {
        return mSyncTask != null;
    }

    /**
     * 获取当前进度信息
     */
    public static String getProgressString() {
        return mSyncProgress;
    }
}

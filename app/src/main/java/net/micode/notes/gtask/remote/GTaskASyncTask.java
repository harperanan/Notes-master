
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
/*异步操作类，实现GTask的异步操作过程
 * 主要方法：
 * private void showNotification(int tickerId, String content) 向用户提示当前同步的状态，是一个用于交互的方法
 * protected Integer doInBackground(Void... unused) 此方法在后台线程执行，完成任务的主要工作，通常需要较长的时间
 * protected void onProgressUpdate(String... progress)  可以使用进度条增加用户体验度。 此方法在主线程执行，用于显示任务执行的进度。
 * protected void onPostExecute(Integer result)  相当于Handler 处理UI的方式，在这里面可以使用在doInBackground 得到的结果处理操作UI
 */
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import net.micode.notes.R;
import net.micode.notes.ui.NotesListActivity;
import net.micode.notes.ui.NotesPreferenceActivity;


public class GTaskASyncTask extends AsyncTask<Void, String, Integer> {

    private static int GTASK_SYNC_NOTIFICATION_ID = 5234235;

    public interface OnCompleteListener {
        void onComplete();
    }

    private Context mContext;

    private NotificationManager mNotifiManager;

    private GTaskManager mTaskManager;

    private OnCompleteListener mOnCompleteListener;

    public GTaskASyncTask(Context context, OnCompleteListener listener) {
        mContext = context;
        mOnCompleteListener = listener;
        mNotifiManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mTaskManager = GTaskManager.getInstance();
    }

    public void cancelSync() {
        mTaskManager.cancelSync();
    }

    public void publishProgess(String message) {//发布进度单位，系统将会调用onProgressUpdate()方法更新这些值
        publishProgress(new String[] {
            message
        });
    }

//    private void showNotification(int tickerId, String content) {
//        Notification notification = new Notification(R.drawable.notification, mContext
//                .getString(tickerId), System.currentTimeMillis());
//        notification.defaults = Notification.DEFAULT_LIGHTS;
//        notification.flags = Notification.FLAG_AUTO_CANCEL;
//        PendingIntent pendingIntent;
//        if (tickerId != R.string.ticker_success) {
//            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
//                    NotesPreferenceActivity.class), 0);
//
//        } else {
//            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
//                    NotesListActivity.class), 0);
//        }
//        notification.setLatestEventInfo(mContext, mContext.getString(R.string.app_name), content,
//                pendingIntent);
//        mNotifiManager.notify(GTASK_SYNC_NOTIFICATION_ID, notification);
//    }
    /**
     * 显示同步状态通知栏提示
     * @param tickerId 通知提示文字资源ID（用于判断同步结果类型）
     * @param content 通知详细内容文本
     */
    private void showNotification(int tickerId, String content) {
        PendingIntent pendingIntent;

        // 根据同步结果类型创建不同的跳转意图
        if (tickerId != R.string.ticker_success) {
            // 同步失败时创建跳转到设置页面的意图
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
                    NotesPreferenceActivity.class), PendingIntent.FLAG_IMMUTABLE); // FLAG_IMMUTABLE表示创建的PendingIntent不可被修改
        } else {
            // 同步成功时创建跳转到笔记列表的意图
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
                    NotesListActivity.class), PendingIntent.FLAG_IMMUTABLE);
        }

        // 构建通知对象
        Notification.Builder builder = new Notification.Builder(mContext)
                .setAutoCancel(true)       // 点击后自动取消通知
                .setContentTitle(mContext.getString(R.string.app_name)) // 设置通知标题为应用名称
                .setContentText(content)   // 设置通知详细内容
                .setContentIntent(pendingIntent) // 设置点击通知的跳转行为
                .setWhen(System.currentTimeMillis()) // 设置通知时间为当前系统时间
                .setOngoing(true);          // 设置通知为持续状态（不会自动消失）

        // 生成Notification对象（兼容旧版本）
        Notification notification = builder.getNotification();

        // 显示通知（使用固定ID GTASK_SYNC_NOTIFICATION_ID，保证同类型通知覆盖）
        mNotifiManager.notify(GTASK_SYNC_NOTIFICATION_ID, notification);
    }
    @Override
    protected Integer doInBackground(Void... unused) {
        publishProgess(mContext.getString(R.string.sync_progress_login, NotesPreferenceActivity
                .getSyncAccountName(mContext)));//利用getString,将把 NotesPreferenceActivity.getSyncAccountName(mContext))的字符串内容传进sync_progress_login中
        return mTaskManager.sync(mContext, this);//进行后台同步具体操作
    }

    @Override
    protected void onProgressUpdate(String... progress) {
        showNotification(R.string.ticker_syncing, progress[0]);
        if (mContext instanceof GTaskSyncService) {//instanceof 判断mContext是否是GTaskSyncService的实例
            ((GTaskSyncService) mContext).sendBroadcast(progress[0]);
        }
    }

    @Override
    protected void onPostExecute(Integer result) {//用于在执行完后台任务后更新UI，显示结果
        if (result == GTaskManager.STATE_SUCCESS) {
            showNotification(R.string.ticker_success, mContext.getString(
                    R.string.success_sync_account, mTaskManager.getSyncAccount()));
            NotesPreferenceActivity.setLastSyncTime(mContext, System.currentTimeMillis());//设置最新同步时间
        } else if (result == GTaskManager.STATE_NETWORK_ERROR) {
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_network));
        } else if (result == GTaskManager.STATE_INTERNAL_ERROR) {
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_internal));
        } else if (result == GTaskManager.STATE_SYNC_CANCELLED) {
            showNotification(R.string.ticker_cancel, mContext
                    .getString(R.string.error_sync_cancelled));
        }//几种不同情况下的结果显示
        if (mOnCompleteListener != null) {
            new Thread(new Runnable() {//这里为方法内的一个线程

                public void run() {
                    mOnCompleteListener.onComplete();
                }//完成后的操作，使用onComplete（）将所有值都重新初始化，相当于完成一次操作
            }).start();
        }
    }
}

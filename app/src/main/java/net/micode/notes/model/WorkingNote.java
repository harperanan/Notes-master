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

 package net.micode.notes.model;

 import android.appwidget.AppWidgetManager;
 import android.content.ContentUris;
 import android.content.Context;
 import android.database.Cursor;
 import android.text.TextUtils;
 import android.util.Log;
 
 import net.micode.notes.data.Notes;
 import net.micode.notes.data.Notes.CallNote;
 import net.micode.notes.data.Notes.DataColumns;
 import net.micode.notes.data.Notes.DataConstants;
 import net.micode.notes.data.Notes.NoteColumns;
 import net.micode.notes.data.Notes.TextNote;
 import net.micode.notes.tool.ResourceParser.NoteBgResources;
 
 /**
  * 表示正在编辑的笔记。
  * 提供了加载笔记、保存笔记、设置笔记属性等功能。
  */
 public class WorkingNote {
     // 笔记对象
     private Note mNote;
     // 笔记 ID
     private long mNoteId;
     // 笔记内容
     private String mContent;
     // 笔记模式
     private int mMode;
     // 提醒时间
     private long mAlertDate;
     // 修改时间
     private long mModifiedDate;
     // 背景颜色 ID
     private int mBgColorId;
     // 小部件 ID
     private int mWidgetId;
     // 小部件类型
     private int mWidgetType;
     // 父文件夹 ID
     private long mFolderId;
     // 上下文
     private Context mContext;
     // 日志标签
     private static final String TAG = "WorkingNote";
     // 是否被删除
     private boolean mIsDeleted;
     // 笔记设置状态改变监听器
     private NoteSettingChangedListener mNoteSettingStatusListener;
 
     // 数据查询投影
     public static final String[] DATA_PROJECTION = new String[] {
             DataColumns.ID, // 数据 ID
             DataColumns.CONTENT, // 数据内容
             DataColumns.MIME_TYPE, // 数据类型
             DataColumns.DATA1, // 数据1
             DataColumns.DATA2, // 数据2
             DataColumns.DATA3, // 数据3
             DataColumns.DATA4, // 数据4
     };
 
     // 笔记查询投影
     public static final String[] NOTE_PROJECTION = new String[] {
             NoteColumns.PARENT_ID, // 父文件夹 ID
             NoteColumns.ALERTED_DATE, // 提醒时间
             NoteColumns.BG_COLOR_ID, // 背景颜色 ID
             NoteColumns.WIDGET_ID, // 小部件 ID
             NoteColumns.WIDGET_TYPE, // 小部件类型
             NoteColumns.MODIFIED_DATE // 修改时间
     };
 
     // 数据列索引
     private static final int DATA_ID_COLUMN = 0;
     private static final int DATA_CONTENT_COLUMN = 1;
     private static final int DATA_MIME_TYPE_COLUMN = 2;
     private static final int DATA_MODE_COLUMN = 3;
 
     // 笔记列索引
     private static final int NOTE_PARENT_ID_COLUMN = 0;
     private static final int NOTE_ALERTED_DATE_COLUMN = 1;
     private static final int NOTE_BG_COLOR_ID_COLUMN = 2;
     private static final int NOTE_WIDGET_ID_COLUMN = 3;
     private static final int NOTE_WIDGET_TYPE_COLUMN = 4;
     private static final int NOTE_MODIFIED_DATE_COLUMN = 5;
 
     /**
      * 创建一个新的笔记。
      * @param context 上下文
      * @param folderId 父文件夹 ID
      */
     private WorkingNote(Context context, long folderId) {
         mContext = context;
         mAlertDate = 0;
         mModifiedDate = System.currentTimeMillis();
         mFolderId = folderId;
         mNote = new Note();
         mNoteId = 0;
         mIsDeleted = false;
         mMode = 0;
         mWidgetType = Notes.TYPE_WIDGET_INVALIDE;
     }
 
     /**
      * 加载一个已存在的笔记。
      * @param context 上下文
      * @param noteId 笔记 ID
      * @param folderId 父文件夹 ID
      */
     private WorkingNote(Context context, long noteId, long folderId) {
         mContext = context;
         mNoteId = noteId;
         mFolderId = folderId;
         mIsDeleted = false;
         mNote = new Note();
         loadNote();
     }
 
     /**
      * 加载笔记数据。
      */
     private void loadNote() {
         Cursor cursor = mContext.getContentResolver().query(
                 ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, mNoteId), NOTE_PROJECTION, null,
                 null, null);
 
         if (cursor != null) {
             if (cursor.moveToFirst()) {
                 mFolderId = cursor.getLong(NOTE_PARENT_ID_COLUMN);
                 mBgColorId = cursor.getInt(NOTE_BG_COLOR_ID_COLUMN);
                 mWidgetId = cursor.getInt(NOTE_WIDGET_ID_COLUMN);
                 mWidgetType = cursor.getInt(NOTE_WIDGET_TYPE_COLUMN);
                 mAlertDate = cursor.getLong(NOTE_ALERTED_DATE_COLUMN);
                 mModifiedDate = cursor.getLong(NOTE_MODIFIED_DATE_COLUMN);
             }
             cursor.close();
         } else {
             Log.e(TAG, "No note with id:" + mNoteId);
             throw new IllegalArgumentException("Unable to find note with id " + mNoteId);
         }
         loadNoteData();
     }
 
     /**
      * 加载笔记的详细数据。
      */
     private void loadNoteData() {
         Cursor cursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI, DATA_PROJECTION,
                 DataColumns.NOTE_ID + "=?", new String[] {
                         String.valueOf(mNoteId)
                 }, null);
 
         if (cursor != null) {
             if (cursor.moveToFirst()) {
                 do {
                     String type = cursor.getString(DATA_MIME_TYPE_COLUMN);
                     if (DataConstants.NOTE.equals(type)) {
                         mContent = cursor.getString(DATA_CONTENT_COLUMN);
                         mMode = cursor.getInt(DATA_MODE_COLUMN);
                         mNote.setTextDataId(cursor.getLong(DATA_ID_COLUMN));
                     } else if (DataConstants.CALL_NOTE.equals(type)) {
                         mNote.setCallDataId(cursor.getLong(DATA_ID_COLUMN));
                     } else {
                         Log.d(TAG, "Wrong note type with type:" + type);
                     }
                 } while (cursor.moveToNext());
             }
             cursor.close();
         } else {
             Log.e(TAG, "No data with id:" + mNoteId);
             throw new IllegalArgumentException("Unable to find note's data with id " + mNoteId);
         }
     }
 
     /**
      * 创建一个空的笔记。
      * @param context 上下文
      * @param folderId 父文件夹 ID
      * @param widgetId 小部件 ID
      * @param widgetType 小部件类型
      * @param defaultBgColorId 默认背景颜色 ID
      * @return 新创建的笔记
      */
     public static WorkingNote createEmptyNote(Context context, long folderId, int widgetId,
                                               int widgetType, int defaultBgColorId) {
         WorkingNote note = new WorkingNote(context, folderId);
         note.setBgColorId(defaultBgColorId);
         note.setWidgetId(widgetId);
         note.setWidgetType(widgetType);
         return note;
     }
 
     /**
      * 加载一个已存在的笔记。
      * @param context 上下文
      * @param id 笔记 ID
      * @return 加载的笔记
      */
     public static WorkingNote load(Context context, long id) {
         return new WorkingNote(context, id, 0);
     }
 
     /**
      * 保存笔记。
      * @return 是否保存成功
      */
     public synchronized boolean saveNote() {
         if (isWorthSaving()) {
             if (!existInDatabase()) {
                 if ((mNoteId = Note.getNewNoteId(mContext, mFolderId)) == 0) {
                     Log.e(TAG, "Create new note fail with id:" + mNoteId);
                     return false;
                 }
             }
 
             mNote.syncNote(mContext, mNoteId);
 
             /**
              * 如果笔记关联了小部件，更新小部件内容
              */
             if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                     && mWidgetType != Notes.TYPE_WIDGET_INVALIDE
                     && mNoteSettingStatusListener != null) {
                 mNoteSettingStatusListener.onWidgetChanged();
             }
             return true;
         } else {
             return false;
         }
     }
 
     /**
      * 检查笔记是否存在于数据库中。
      * @return 是否存在于数据库中
      */
     public boolean existInDatabase() {
         return mNoteId > 0;
     }
 
     /**
      * 检查笔记是否值得保存。
      * @return 是否值得保存
      */
     private boolean isWorthSaving() {
         if (mIsDeleted || (!existInDatabase() && TextUtils.isEmpty(mContent))
                 || (existInDatabase() && !mNote.isLocalModified())) {
             return false;
         } else {
             return true;
         }
     }
 
     /**
      * 设置笔记设置状态改变监听器。
      * @param l 监听器
      */
     public void setOnSettingStatusChangedListener(NoteSettingChangedListener l) {
         mNoteSettingStatusListener = l;
     }
 
     /**
      * 设置提醒时间。
      * @param date 提醒时间
      * @param set 是否设置提醒
      */
     public void setAlertDate(long date, boolean set) {
         if (date != mAlertDate) {
             mAlertDate = date;
             mNote.setNoteValue(NoteColumns.ALERTED_DATE, String.valueOf(mAlertDate));
         }
         if (mNoteSettingStatusListener != null) {
             mNoteSettingStatusListener.onClockAlertChanged(date, set);
         }
     }
 
     /**
      * 标记笔记为删除状态。
      * @param mark 是否标记为删除
      */
     public void markDeleted(boolean mark) {
         mIsDeleted = mark;
         if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                 && mWidgetType != Notes.TYPE_WIDGET_INVALIDE && mNoteSettingStatusListener != null) {
             mNoteSettingStatusListener.onWidgetChanged();
         }
     }
 
     /**
      * 设置背景颜色 ID。
      * @param id 背景颜色 ID
      */
     public void setBgColorId(int id) {
         if (id != mBgColorId) {
             mBgColorId = id;
             if (mNoteSettingStatusListener != null) {
                 mNoteSettingStatusListener.onBackgroundColorChanged();
             }
             mNote.setNoteValue(NoteColumns.BG_COLOR_ID, String.valueOf(id));
         }
     }
 
     /**
      * 设置检查列表模式。
      * @param mode 模式
      */
     public void setCheckListMode(int mode) {
         if (mMode != mode) {
             if (mNoteSettingStatusListener != null) {
                 mNoteSettingStatusListener.onCheckListModeChanged(mMode, mode);
             }
             mMode = mode;
             mNote.setTextData(TextNote.MODE, String.valueOf(mMode));
         }
     }
 
     /**
      * 设置小部件类型。
      * @param type 小部件类型
      */
     public void setWidgetType(int type) {
         if (type != mWidgetType) {
             mWidgetType = type;
             mNote.setNoteValue(NoteColumns.WIDGET_TYPE, String.valueOf(mWidgetType));
         }
     }
 
     /**
      * 设置小部件 ID。
      * @param id 小部件 ID
      */
     public void setWidgetId(int id) {
         if (id != mWidgetId) {
             mWidgetId = id;
             mNote.setNoteValue(NoteColumns.WIDGET_ID, String.valueOf(mWidgetId));
         }
     }
 
     /**
      * 设置笔记内容。
      * @param text 笔记内容
      */
     public void setWorkingText(String text) {
         if (!TextUtils.equals(mContent, text)) {
             mContent = text;
             mNote.setTextData(DataColumns.CONTENT, mContent);
         }
     }
 
     /**
      * 将笔记转换为通话记录笔记。
      * @param phoneNumber 电话号码
      * @param callDate 通话时间
      */
     public void convertToCallNote(String phoneNumber, long callDate) {
         mNote.setCallData(CallNote.CALL_DATE, String.valueOf(callDate));
         mNote.setCallData(CallNote.PHONE_NUMBER, phoneNumber);
         mNote.setNoteValue(NoteColumns.PARENT_ID, String.valueOf(Notes.ID_CALL_RECORD_FOLDER));
     }
 
     /**
      * 检查笔记是否有提醒。
      * @return 是否有提醒
      */
     public boolean hasClockAlert() {
         return (mAlertDate > 0 ? true : false);
     }
 
     /**
      * 获取笔记内容。
      * @return 笔记内容
      */
     public String getContent() {
         return mContent;
     }
 
     /**
      * 获取提醒时间。
      * @return 提醒时间
      */
     public long getAlertDate() {
         return mAlertDate;
     }
 
     /**
      * 获取修改时间。
      * @return 修改时间
      */
     public long getModifiedDate() {
         return mModifiedDate;
     }
 
     /**
      * 获取背景颜色资源 ID。
      * @return 背景颜色资源 ID
      */
     public int getBgColorResId() {
         return NoteBgResources.getNoteBgResource(mBgColorId);
     }
 
     /**
      * 获取背景颜色 ID。
      * @return 背景颜色 ID
      */
     public int getBgColorId() {
         return mBgColorId;
     }
 
     /**
      * 获取标题背景颜色资源 ID。
      * @return 标题背景颜色资源 ID
      */
     public int getTitleBgResId() {
         return NoteBgResources.getNoteTitleBgResource(mBgColorId);
     }
 
     /**
      * 获取检查列表模式。
      * @return 模式
      */
     public int getCheckListMode() {
         return mMode;
     }
 
     /**
      * 获取笔记 ID。
      * @return 笔记 ID
      */
     public long getNoteId() {
         return mNoteId;
     }
 
     /**
      * 获取父文件夹 ID。
      * @return 父文件夹 ID
      */
     public long getFolderId() {
         return mFolderId;
     }
 
     /**
      * 获取小部件 ID。
      * @return 小部件 ID
      */
     public int getWidgetId() {
         return mWidgetId;
     }
 
     /**
      * 获取小部件类型。
      * @return 小部件类型
      */
     public int getWidgetType() {
         return mWidgetType;
     }
 
     /**
      * 笔记设置状态改变监听器。
      */
     public interface NoteSettingChangedListener {
         /**
          * 背景颜色改变时调用。
          */
         void onBackgroundColorChanged();
 
         /**
          * 提醒时间改变时调用。
          * @param date 提醒时间
          * @param set 是否设置提醒
          */
         void onClockAlertChanged(long date, boolean set);
 
         /**
          * 小部件内容改变时调用。
          */
         void onWidgetChanged();
 
         /**
          * 检查列表模式改变时调用。
          * @param oldMode 旧模式
          * @param newMode 新模式
          */
         void onCheckListModeChanged(int oldMode, int newMode);
     }
 }
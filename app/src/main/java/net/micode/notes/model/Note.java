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

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.Notes.TextNote;

import java.util.ArrayList;

/**
 * 表示笔记及其相关数据的类。
 * 提供了创建新笔记、设置笔记值、同步笔记数据到内容提供器等功能。
 */
public class Note {
    // 存储笔记的差异值
    private ContentValues mNoteDiffValues;
    // 存储笔记的数据
    private NoteData mNoteData;
    // 日志标签
    private static final String TAG = "Note";

    /**
     * 创建一个新的笔记 ID。
     * @param context 上下文
     * @param folderId 父文件夹 ID
     * @return 新笔记的 ID
     */
    public static synchronized long getNewNoteId(Context context, long folderId) {
        // 创建一个新的笔记
        ContentValues values = new ContentValues();
        long createdTime = System.currentTimeMillis();
        values.put(NoteColumns.CREATED_DATE, createdTime); // 创建时间
        values.put(NoteColumns.MODIFIED_DATE, createdTime); // 修改时间
        values.put(NoteColumns.TYPE, Notes.TYPE_NOTE); // 笔记类型
        values.put(NoteColumns.LOCAL_MODIFIED, 1); // 标记为本地修改
        values.put(NoteColumns.PARENT_ID, folderId); // 父文件夹 ID
        Uri uri = context.getContentResolver().insert(Notes.CONTENT_NOTE_URI, values); // 插入笔记

        long noteId = 0;
        try {
            noteId = Long.valueOf(uri.getPathSegments().get(1)); // 获取笔记 ID
        } catch (NumberFormatException e) {
            Log.e(TAG, "Get note id error :" + e.toString());
            noteId = 0;
        }
        if (noteId == -1) {
            throw new IllegalStateException("Wrong note id:" + noteId);
        }
        return noteId;
    }

    /**
     * 构造函数。
     */
    public Note() {
        mNoteDiffValues = new ContentValues();
        mNoteData = new NoteData();
    }

    /**
     * 设置笔记的某个字段值。
     * @param key 字段名
     * @param value 字段值
     */
    public void setNoteValue(String key, String value) {
        mNoteDiffValues.put(key, value); // 设置笔记的键值对
        mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1); // 标记为本地修改
        mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis()); // 更新修改时间
    }

    /**
     * 设置笔记的文本数据。
     * @param key 字段名
     * @param value 字段值
     */
    public void setTextData(String key, String value) {
        mNoteData.setTextData(key, value); // 设置文本数据
    }

    /**
     * 设置笔记的文本数据 ID。
     * @param id 文本数据 ID
     */
    public void setTextDataId(long id) {
        mNoteData.setTextDataId(id); // 设置文本数据 ID
    }

    /**
     * 获取笔记的文本数据 ID。
     * @return 文本数据 ID
     */
    public long getTextDataId() {
        return mNoteData.mTextDataId; // 返回文本数据 ID
    }

    /**
     * 设置笔记的通话记录数据 ID。
     * @param id 通话记录数据 ID
     */
    public void setCallDataId(long id) {
        mNoteData.setCallDataId(id); // 设置通话记录数据 ID
    }

    /**
     * 设置笔记的通话记录数据。
     * @param key 字段名
     * @param value 字段值
     */
    public void setCallData(String key, String value) {
        mNoteData.setCallData(key, value); // 设置通话记录数据
    }

    /**
     * 检查笔记是否有本地修改。
     * @return 是否有本地修改
     */
    public boolean isLocalModified() {
        return mNoteDiffValues.size() > 0 || mNoteData.isLocalModified(); // 检查是否有本地修改
    }

    /**
     * 同步笔记数据到内容提供器。
     * @param context 上下文
     * @param noteId 笔记 ID
     * @return 是否同步成功
     */
    public boolean syncNote(Context context, long noteId) {
        if (noteId <= 0) {
            throw new IllegalArgumentException("Wrong note id:" + noteId);
        }

        if (!isLocalModified()) {
            return true;
        }

        if (context.getContentResolver().update(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId), mNoteDiffValues, null,
                null) == 0) {
            Log.e(TAG, "Update note error, should not happen");
        }
        mNoteDiffValues.clear();

        if (mNoteData.isLocalModified()
                && (mNoteData.pushIntoContentResolver(context, noteId) == null)) {
            return false;
        }

        return true;
    }

    /**
     * 存储笔记的数据。
     */
    private class NoteData {
        // 文本数据 ID
        private long mTextDataId;
        // 文本数据值
        private ContentValues mTextDataValues;
        // 通话记录数据 ID
        private long mCallDataId;
        // 通话记录数据值
        private ContentValues mCallDataValues;
        // 日志标签
        private static final String TAG = "NoteData";

        /**
         * 构造函数。
         */
        public NoteData() {
            mTextDataValues = new ContentValues();
            mCallDataValues = new ContentValues();
            mTextDataId = 0;
            mCallDataId = 0;
        }

        /**
         * 检查是否有本地修改。
         * @return 是否有本地修改
         */
        boolean isLocalModified() {
            return mTextDataValues.size() > 0 || mCallDataValues.size() > 0; // 检查是否有本地修改
        }

        /**
         * 设置文本数据 ID。
         * @param id 文本数据 ID
         */
        void setTextDataId(long id) {
            if(id <= 0) {
                throw new IllegalArgumentException("Text data id should larger than 0");
            }
            mTextDataId = id;
        }

        /**
         * 设置通话记录数据 ID。
         * @param id 通话记录数据 ID
         */
        void setCallDataId(long id) {
            if (id <= 0) {
                throw new IllegalArgumentException("Call data id should larger than 0");
            }
            mCallDataId = id;
        }

        /**
         * 设置通话记录数据。
         * @param key 字段名
         * @param value 字段值
         */
        void setCallData(String key, String value) {
            mCallDataValues.put(key, value); // 设置通话记录数据
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1); // 标记为本地修改
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis()); // 更新修改时间
        }

        /**
         * 设置文本数据。
         * @param key 字段名
         * @param value 字段值
         */
        void setTextData(String key, String value) {
            mTextDataValues.put(key, value); // 设置文本数据
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1); // 标记为本地修改
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis()); // 更新修改时间
        }

        /**
         * 将笔记数据同步到内容提供器。
         * @param context 上下文
         * @param noteId 笔记 ID
         * @return 同步结果的 Uri
         */
        Uri pushIntoContentResolver(Context context, long noteId) {
            if (noteId <= 0) {
                throw new IllegalArgumentException("Wrong note id:" + noteId);
            }

            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            ContentProviderOperation.Builder builder = null;

            if(mTextDataValues.size() > 0) {
                mTextDataValues.put(DataColumns.NOTE_ID, noteId);
                if (mTextDataId == 0) {
                    mTextDataValues.put(DataColumns.MIME_TYPE, TextNote.CONTENT_ITEM_TYPE);
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI,
                            mTextDataValues);
                    try {
                        setTextDataId(Long.valueOf(uri.getPathSegments().get(1)));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Insert new text data fail with noteId" + noteId);
                        mTextDataValues.clear();
                        return null;
                    }
                } else {
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mTextDataId));
                    builder.withValues(mTextDataValues);
                    operationList.add(builder.build());
                }
                mTextDataValues.clear();
            }

            if(mCallDataValues.size() > 0) {
                mCallDataValues.put(DataColumns.NOTE_ID, noteId);
                if (mCallDataId == 0) {
                    mCallDataValues.put(DataColumns.MIME_TYPE, CallNote.CONTENT_ITEM_TYPE);
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI,
                            mCallDataValues);
                    try {
                        setCallDataId(Long.valueOf(uri.getPathSegments().get(1)));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Insert new call data fail with noteId" + noteId);
                        mCallDataValues.clear();
                        return null;
                    }
                } else {
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mCallDataId));
                    builder.withValues(mCallDataValues);
                    operationList.add(builder.build());
                }
                mCallDataValues.clear();
            }

            if (operationList.size() > 0) {
                try {
                    ContentProviderResult[] results = context.getContentResolver().applyBatch(
                            Notes.AUTHORITY, operationList);
                    return (results == null || results.length == 0 || results[0] == null) ? null
                            : ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId);
                } catch (RemoteException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    return null;
                } catch (OperationApplicationException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    return null;
                }
            }
            return null;
        }
    }
}
/*
 * 版权所有 (c) 2010-2011，MiCode开源社区（www.micode.net）
 * 遵循Apache License 2.0协议
 */

package net.micode.notes.gtask.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.NotesDatabaseHelper.TABLE;
import net.micode.notes.gtask.exception.ActionFailureException;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * SqlData类用于处理与数据库交互的数据操作，支持从JSON解析数据和同步到数据库。
 */
public class SqlData {
    private static final String TAG = SqlData.class.getSimpleName(); // 日志标签

    private static final int INVALID_ID = -99999; // 无效ID常量

    // 数据库查询的列投影
    public static final String[] PROJECTION_DATA = new String[] {
            DataColumns.ID, DataColumns.MIME_TYPE, DataColumns.CONTENT, DataColumns.DATA1,
            DataColumns.DATA3
    };

    // 列索引常量
    public static final int DATA_ID_COLUMN = 0;           // ID列索引
    public static final int DATA_MIME_TYPE_COLUMN = 1;    // MIME类型列索引
    public static final int DATA_CONTENT_COLUMN = 2;      // 内容列索引
    public static final int DATA_CONTENT_DATA_1_COLUMN = 3; // DATA1列索引
    public static final int DATA_CONTENT_DATA_3_COLUMN = 4; // DATA3列索引

    private ContentResolver mContentResolver; // 内容解析器，用于数据库操作
    private boolean mIsCreate;                // 标识是否为新建数据项
    private long mDataId;                     // 数据项ID
    private String mDataMimeType;             // MIME类型
    private String mDataContent;              // 内容
    private long mDataContentData1;           // DATA1字段值
    private String mDataContentData3;         // DATA3字段值
    private ContentValues mDiffDataValues;    // 记录需要更新的字段差异

    /**
     * 构造函数，用于创建新数据项。
     * @param context 上下文对象
     */
    public SqlData(Context context) {
        mContentResolver = context.getContentResolver();
        mIsCreate = true;
        mDataId = INVALID_ID; // 初始化为无效ID
        mDataMimeType = DataConstants.NOTE; // 默认MIME类型为便签
        mDataContent = ""; // 初始内容为空
        mDataContentData1 = 0; // DATA1初始为0
        mDataContentData3 = ""; // DATA3初始为空
        mDiffDataValues = new ContentValues(); // 初始化差异值容器
    }

    /**
     * 构造函数，从数据库Cursor加载现有数据项。
     * @param context 上下文对象
     * @param c 数据库Cursor
     */
    public SqlData(Context context, Cursor c) {
        mContentResolver = context.getContentResolver();
        mIsCreate = false; // 标记为现有数据
        loadFromCursor(c); // 从Cursor加载数据
        mDiffDataValues = new ContentValues(); // 初始化差异值容器
    }

    /**
     * 从Cursor加载数据到成员变量。
     * @param c 数据库Cursor
     */
    private void loadFromCursor(Cursor c) {
        mDataId = c.getLong(DATA_ID_COLUMN); // 读取ID
        mDataMimeType = c.getString(DATA_MIME_TYPE_COLUMN); // 读取MIME类型
        mDataContent = c.getString(DATA_CONTENT_COLUMN); // 读取内容
        mDataContentData1 = c.getLong(DATA_CONTENT_DATA_1_COLUMN); // 读取DATA1
        mDataContentData3 = c.getString(DATA_CONTENT_DATA_3_COLUMN); // 读取DATA3
    }

    /**
     * 从JSON对象解析数据并更新字段。
     * @param js JSON数据对象
     * @throws JSONException JSON解析异常
     */
    public void setContent(JSONObject js) throws JSONException {
        // 解析并更新ID字段
        long dataId = js.has(DataColumns.ID) ? js.getLong(DataColumns.ID) : INVALID_ID;
        if (mIsCreate || mDataId != dataId) {
            mDiffDataValues.put(DataColumns.ID, dataId); // 记录差异
        }
        mDataId = dataId;

        // 解析并更新MIME类型
        String dataMimeType = js.has(DataColumns.MIME_TYPE) ? js.getString(DataColumns.MIME_TYPE)
                : DataConstants.NOTE;
        if (mIsCreate || !mDataMimeType.equals(dataMimeType)) {
            mDiffDataValues.put(DataColumns.MIME_TYPE, dataMimeType);
        }
        mDataMimeType = dataMimeType;

        // 解析并更新内容
        String dataContent = js.has(DataColumns.CONTENT) ? js.getString(DataColumns.CONTENT) : "";
        if (mIsCreate || !mDataContent.equals(dataContent)) {
            mDiffDataValues.put(DataColumns.CONTENT, dataContent);
        }
        mDataContent = dataContent;

        // 解析并更新DATA1字段
        long dataContentData1 = js.has(DataColumns.DATA1) ? js.getLong(DataColumns.DATA1) : 0;
        if (mIsCreate || mDataContentData1 != dataContentData1) {
            mDiffDataValues.put(DataColumns.DATA1, dataContentData1);
        }
        mDataContentData1 = dataContentData1;

        // 解析并更新DATA3字段
        String dataContentData3 = js.has(DataColumns.DATA3) ? js.getString(DataColumns.DATA3) : "";
        if (mIsCreate || !mDataContentData3.equals(dataContentData3)) {
            mDiffDataValues.put(DataColumns.DATA3, dataContentData3);
        }
        mDataContentData3 = dataContentData3;
    }

    /**
     * 将当前数据转换为JSON对象。
     * @return JSON数据对象
     * @throws JSONException JSON构造异常
     */
    public JSONObject getContent() throws JSONException {
        if (mIsCreate) {
            Log.e(TAG, "数据尚未创建到数据库");
            return null;
        }
        JSONObject js = new JSONObject();
        js.put(DataColumns.ID, mDataId);
        js.put(DataColumns.MIME_TYPE, mDataMimeType);
        js.put(DataColumns.CONTENT, mDataContent);
        js.put(DataColumns.DATA1, mDataContentData1);
        js.put(DataColumns.DATA3, mDataContentData3);
        return js;
    }

    /**
     * 提交数据到数据库。
     * @param noteId 所属便签ID
     * @param validateVersion 是否验证版本
     * @param version 版本号（用于并发控制）
     * @throws ActionFailureException 操作失败异常
     */
    public void commit(long noteId, boolean validateVersion, long version) {
        if (mIsCreate) { // 处理新建数据
            if (mDataId == INVALID_ID && mDiffDataValues.containsKey(DataColumns.ID)) {
                mDiffDataValues.remove(DataColumns.ID); // 移除无效ID
            }

            mDiffDataValues.put(DataColumns.NOTE_ID, noteId); // 设置关联便签ID
            Uri uri = mContentResolver.insert(Notes.CONTENT_DATA_URI, mDiffDataValues);
            try {
                // 从插入的URI中获取新数据的ID
                mDataId = Long.valueOf(uri.getPathSegments().get(1));
            } catch (NumberFormatException e) {
                Log.e(TAG, "获取便签ID错误：" + e);
                throw new ActionFailureException("创建便签失败");
            }
        } else { // 处理更新数据
            if (mDiffDataValues.size() > 0) {
                int result = 0;
                if (!validateVersion) { // 不验证版本，直接更新
                    result = mContentResolver.update(
                            ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, mDataId),
                            mDiffDataValues,
                            null,
                            null
                    );
                } else { // 带版本验证的更新
                    result = mContentResolver.update(
                            ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, mDataId),
                            mDiffDataValues,
                            " ? IN (SELECT " + NoteColumns.ID + " FROM " + TABLE.NOTE
                                    + " WHERE " + NoteColumns.VERSION + "=?)",
                            new String[] { String.valueOf(noteId), String.valueOf(version) }
                    );
                }
                if (result == 0) {
                    Log.w(TAG, "无更新发生，可能在同步时用户已修改便签");
                }
            }
        }

        mDiffDataValues.clear(); // 清空差异记录
        mIsCreate = false; // 标记为已存在数据
    }

    /**
     * 获取当前数据项ID。
     * @return 数据ID
     */
    public long getId() {
        return mDataId;
    }
}
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

package net.micode.notes.tool;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * 笔记备份工具类（单例模式）
 * 功能：将用户笔记导出为可读文本文件到SD卡
 */

public class BackupUtils {
    private static final String TAG = "BackupUtils"; //备份标签定义
    // 单例实现
    private static BackupUtils sInstance;

    /**
     * 获取备份工具单例实例
     * @param context:Android上下文
     * @return:备份工具
     */
    public static synchronized BackupUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BackupUtils(context);
        }
        return sInstance;
    }

    /**
     * Following states are signs to represents backup or restore
     * status
     */
    // Currently, the sdcard is not mounted
    //备份状态码
    public static final int STATE_SD_CARD_UNMOUONTED           = 0; //SD卡未挂载
    // The backup file not exist---备份文件不存在
    public static final int STATE_BACKUP_FILE_NOT_EXIST        = 1;
    // The data is not well formated, may be changed by other programs
    //数据不符合格式or损坏
    public static final int STATE_DATA_DESTROIED               = 2;
    // Some run-time exception which causes restore or backup fails
    //系统错误
    public static final int STATE_SYSTEM_ERROR                 = 3;
    // Backup or restore success
    //操作成功
    public static final int STATE_SUCCESS                      = 4;

    private TextExport mTextExport; //文件导出处理

    private BackupUtils(Context context) {
        mTextExport = new TextExport(context);
    }

    /** 检查外部存储可用性 */
    private static boolean externalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /** 执行文本导出操作 */
    public int exportToText() {
        return mTextExport.exportToText();
    }

    /** 获取导出的文本文件名称 */
    public String getExportedTextFileName() {
        return mTextExport.mFileName;
    }

    /** 获取导出的文本文件目录 */
    public String getExportedTextFileDir() {
        return mTextExport.mFileDirectory;
    }

    /**
     * 文本导出处理器（内部类）
     * 负责实际的数据查询和文件写入操作
     */
    private static class TextExport {
        //笔记元数据查询类
        private static final String[] NOTE_PROJECTION = {
                NoteColumns.ID, //笔记id
                NoteColumns.MODIFIED_DATE, //修改时间
                NoteColumns.SNIPPET, //内容摘要
                NoteColumns.TYPE //类型
        };

        //状态
        private static final int NOTE_COLUMN_ID = 0;//笔记id

        private static final int NOTE_COLUMN_MODIFIED_DATE = 1; //修改时间

        private static final int NOTE_COLUMN_SNIPPET = 2;//内容摘要

        //笔记数据查询字段
        private static final String[] DATA_PROJECTION = {
                DataColumns.CONTENT, //内容主体
                DataColumns.MIME_TYPE, //mime类型
                DataColumns.DATA1, //拓展数据
                DataColumns.DATA2,
                DataColumns.DATA3,
                DataColumns.DATA4,
        };

        //状态
        private static final int DATA_COLUMN_CONTENT = 0; //内容主体

        private static final int DATA_COLUMN_MIME_TYPE = 1; //类型

        private static final int DATA_COLUMN_CALL_DATE = 2; //通话日期

        private static final int DATA_COLUMN_PHONE_NUMBER = 4; //电话号码

        private final String [] TEXT_FORMAT; //文本格式
        private static final int FORMAT_FOLDER_NAME          = 0; // 文件夹名称格式
        private static final int FORMAT_NOTE_DATE            = 1;// 笔记日期格式
        private static final int FORMAT_NOTE_CONTENT         = 2;// 笔记内容格式

        private Context mContext;  // Android上下文
        private String mFileName; // 导出文件名
        private String mFileDirectory; // 导出目录

        public TextExport(Context context) {
            // 从资源文件加载文本格式模板
            TEXT_FORMAT = context.getResources().getStringArray(R.array.format_for_exported_note);
            mContext = context;
            mFileName = "";
            mFileDirectory = "";
        }
        /** 获取指定格式模板 */
        private String getFormat(int id) {
            return TEXT_FORMAT[id];
        }

        /**
         * Export the folder identified by folder id to text
         * * 导出指定文件夹的笔记到文本
         *  @param folderId 文件夹ID
         * @param ps 输出流
         */
        private void exportFolderToText(String folderId, PrintStream ps) {
            // Query notes belong to this folder
            // 查询属于该文件夹的笔记
            Cursor notesCursor = mContext.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION, NoteColumns.PARENT_ID + "=?", new String[] {
                        folderId
                    }, null);

            if (notesCursor != null) {
                if (notesCursor.moveToFirst()) {
                    do {
                        // Print note's last modified date
                        // 输出笔记修改时间
                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                                mContext.getString(R.string.format_datetime_mdhm),
                                notesCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                        // Query data belong to this note
                        // 导出单条笔记内容
                        String noteId = notesCursor.getString(NOTE_COLUMN_ID);
                        exportNoteToText(noteId, ps);
                    } while (notesCursor.moveToNext());
                }
                notesCursor.close();
            }
        }

        /**
         * Export note identified by id to a print stream
         * 导出单条笔记内容
         * @param noteId 笔记ID
         * @param ps 输出流
         */
        private void exportNoteToText(String noteId, PrintStream ps) {
            // 查询笔记关联的数据项
            Cursor dataCursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI,
                    DATA_PROJECTION, DataColumns.NOTE_ID + "=?", new String[] {
                        noteId
                    }, null);
            //如果存在
            if (dataCursor != null) {
                if (dataCursor.moveToFirst()) {
                    do {
                        String mimeType = dataCursor.getString(DATA_COLUMN_MIME_TYPE);
                        if (DataConstants.CALL_NOTE.equals(mimeType)) {
                            // Print phone number
                            // 处理通话记录类型数据
                            String phoneNumber = dataCursor.getString(DATA_COLUMN_PHONE_NUMBER);
                            long callDate = dataCursor.getLong(DATA_COLUMN_CALL_DATE);
                            String location = dataCursor.getString(DATA_COLUMN_CONTENT);

                            if (!TextUtils.isEmpty(phoneNumber)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        phoneNumber));
                            }
                            // Print call date---输出通话日期
                            ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT), DateFormat
                                    .format(mContext.getString(R.string.format_datetime_mdhm),
                                            callDate)));
                            // Print call attachment location 输出通话位置
                            if (!TextUtils.isEmpty(location)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        location));
                            }
                        } else if (DataConstants.NOTE.equals(mimeType)) {
                            // 处理普通文本笔记
                            String content = dataCursor.getString(DATA_COLUMN_CONTENT);
                            if (!TextUtils.isEmpty(content)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        content));
                            }
                        }
                    } while (dataCursor.moveToNext());
                }
                dataCursor.close();
            }
            // print a line separator between note
            // 添加笔记分隔线
            try {
                ps.write(new byte[] {
                        Character.LINE_SEPARATOR, Character.LETTER_NUMBER
                });
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        /**
         * Note will be exported as text which is user readable
         * 执行文本导出主逻辑
         * @return 操作状态码
         */
        public int exportToText() {
            //检查sd卡状态
            if (!externalStorageAvailable()) {
                Log.d(TAG, "Media was not mounted");
                return STATE_SD_CARD_UNMOUONTED;
            }

            // 获取输出流
            PrintStream ps = getExportToTextPrintStream();
            if (ps == null) {
                Log.e(TAG, "get print stream error");
                return STATE_SYSTEM_ERROR;
            }
            // First export folder and its notes
            /* 导出文件夹结构 */
            // 查询所有非回收站文件夹（包括通话记录专用文件夹）
            Cursor folderCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    "(" + NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER + " AND "
                            + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER + ") OR "
                            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER, null, null);

            if (folderCursor != null) {
                if (folderCursor.moveToFirst()) {
                    do {
                        // Print folder's name---文件夹的名称
                        String folderName = "";
                        if(folderCursor.getLong(NOTE_COLUMN_ID) == Notes.ID_CALL_RECORD_FOLDER) {
                            folderName = mContext.getString(R.string.call_record_folder_name);
                        } else {
                            folderName = folderCursor.getString(NOTE_COLUMN_SNIPPET);
                        }
                        if (!TextUtils.isEmpty(folderName)) {
                            ps.println(String.format(getFormat(FORMAT_FOLDER_NAME), folderName));
                        }
                        //导出文件夹内容
                        String folderId = folderCursor.getString(NOTE_COLUMN_ID);
                        exportFolderToText(folderId, ps);
                    } while (folderCursor.moveToNext());
                }
                folderCursor.close();
            }

            // Export notes in root's folder
            /* 导出根目录下的独立笔记 */
            Cursor noteCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    NoteColumns.TYPE + "=" + +Notes.TYPE_NOTE + " AND " + NoteColumns.PARENT_ID
                            + "=0", null, null);

            if (noteCursor != null) {
                if (noteCursor.moveToFirst()) {
                    do {
                        // 输出笔记基本信息
                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                                mContext.getString(R.string.format_datetime_mdhm),
                                noteCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                        // Query data belong to this note
                        // 导出笔记内容
                        String noteId = noteCursor.getString(NOTE_COLUMN_ID);
                        exportNoteToText(noteId, ps);
                    } while (noteCursor.moveToNext());
                }
                noteCursor.close();
            }
            ps.close();

            return STATE_SUCCESS;
        }

        /**
         * Get a print stream pointed to the file {@generateExportedTextFile}
         * 创建并获取文本输出流
         */
        private PrintStream getExportToTextPrintStream() {
            // 生成带时间戳的文件名
            File file = generateFileMountedOnSDcard(mContext, R.string.file_path,
                    R.string.file_name_txt_format);
            if (file == null) {
                Log.e(TAG, "create file to exported failed");
                return null;
            }
            // 记录文件信息
            mFileName = file.getName();
            mFileDirectory = mContext.getString(R.string.file_path);
            PrintStream ps = null;
            try {
                FileOutputStream fos = new FileOutputStream(file);
                ps = new PrintStream(fos);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } catch (NullPointerException e) {
                e.printStackTrace();
                return null;
            }
            return ps;
        }
    }

    /**
     * Generate the text file to store imported data
     * 在SD卡上生成导出文件
     * @param context Android上下文
     * @param filePathResId 文件路径资源ID（如R.string.file_path）
     * @param fileNameFormatResId 文件名格式资源ID（如R.string.file_name_txt_format）
     * @return 生成的文件对象
     */
    private static File generateFileMountedOnSDcard(Context context, int filePathResId, int fileNameFormatResId) {
        StringBuilder sb = new StringBuilder();//构建文件路径
        sb.append(Environment.getExternalStorageDirectory());//SD卡根目录
        sb.append(context.getString(filePathResId));//路径
        File filedir = new File(sb.toString());
        sb.append(context.getString(
                fileNameFormatResId,
                DateFormat.format(context.getString(R.string.format_date_ymd),
                        System.currentTimeMillis())));
        File file = new File(sb.toString());

        try {
            if (!filedir.exists()) { // 创建目录（如果不存在）
                filedir.mkdir();
            }
            if (!file.exists()) { // 创建新文件
                file.createNewFile();
            }
            return file;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}



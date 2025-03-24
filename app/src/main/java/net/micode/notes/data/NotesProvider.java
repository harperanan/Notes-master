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

package net.micode.notes.data;


import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.R;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.NotesDatabaseHelper.TABLE;

//ContentProvider：Android四大组件之一，允许应用程序之间共享数据
public class NotesProvider extends ContentProvider {
    private static final UriMatcher mMatcher;//ui匹配器定义，用于匹配不同URI的UriMatcher对象，用于解析传入的URI，并确定应该执行哪种操作

    //NotesProvider的主要功能是作为一个内容提供者，为其他应用程序或组件提供对Notes数据的访问
    //允许其他应用程序查询、插入、更新或删除标签数据
    //通过URI匹配，NotesProvider能够区分对哪种数据类型的请求（单独的标签、标签的数据、文件夹操作等），并执行相应的操作
    private NotesDatabaseHelper mHelper;//NotesDatabaseHelper实类定义，用来操作SQLite数据库，负责创建、更新和查询数据库

    private static final String TAG = "NotesProvider";//日志标记定义，输出日志时用来表示是该类发出的消息

    //6个uri的匹配码，用于区分不同的uri类型
    private static final int URI_NOTE            = 1;
    private static final int URI_NOTE_ITEM       = 2;
    private static final int URI_DATA            = 3;
    private static final int URI_DATA_ITEM       = 4;

    private static final int URI_SEARCH          = 5;
    private static final int URI_SEARCH_SUGGEST  = 6;
    //初始化了一个UriMatcher对象mMatcher，并添加了一系列的URI匹配规则
    static {
        //创建了一个UriMatcher实例，并设置默认匹配码为NO_MATCH，表示如果没有任何URI匹配，则返回这个码
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        //添加规则，当URI的authority为Notes.AUTHORITY，路径为note时，返回匹配码URI_NOTE
        mMatcher.addURI(Notes.AUTHORITY, "note", URI_NOTE);
        //添加规则，当URI的authority为Notes.AUTHORITY，路径为note/后跟一个数字（#代表数字）时，返回匹配码URI_NOTE_ITEM
        mMatcher.addURI(Notes.AUTHORITY, "note/#", URI_NOTE_ITEM);
        //用于匹配数据相关的URI
        mMatcher.addURI(Notes.AUTHORITY, "data", URI_DATA);
        mMatcher.addURI(Notes.AUTHORITY, "data/#", URI_DATA_ITEM);
        //用于匹配搜索相关的URI
        mMatcher.addURI(Notes.AUTHORITY, "search", URI_SEARCH);
        //用于匹配搜索建议相关的URI
        mMatcher.addURI(Notes.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, URI_SEARCH_SUGGEST);
        mMatcher.addURI(Notes.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", URI_SEARCH_SUGGEST);
    }

    /**
     * x'0A' represents the '\n' character in sqlite. For title and content in the search result,
     * we will trim '\n' and white space in order to show more information.
     */
    //返回笔记的ID
    //笔记的id也被重命名为SUGGEST_COLUMN_INTENT_EXTRA_DATA，这通常用于安卓的搜索建议中，作为传递给相关Intent的额外数据
    //对SNIPPET列的处理1：首先使用REPLACE函数将x'0A'（即换行符 \n）替换为空字符串，然后使用TRIM函数删除前后的空白字符，处理后的结果分别重命名为SUGGEST_COLUMN_TEXT_1
    //对SNIPPET列的处理2：首先使用REPLACE函数将 x'0A'（即换行符 \n）替换为空字符串，然后使用TRIM函数删除前后的空白字符，处理后的结果分别重命名为SUGGEST_COLUMN_TEXT_2
    //返回一个用于搜索建议图标的资源ID，并命名为SUGGEST_COLUMN_ICON_1
    //返回一个固定的Intent动作ACTION_VIEW，并命名为SUGGEST_COLUMN_INTENT_ACTION
    //返回一个内容类型，并命名为SUGGEST_COLUMN_INTENT_DATA
    private static final String NOTES_SEARCH_PROJECTION = NoteColumns.ID + ","
        + NoteColumns.ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA + ","
        + "TRIM(REPLACE(" + NoteColumns.SNIPPET + ", x'0A','')) AS " + SearchManager.SUGGEST_COLUMN_TEXT_1 + ","
        + "TRIM(REPLACE(" + NoteColumns.SNIPPET + ", x'0A','')) AS " + SearchManager.SUGGEST_COLUMN_TEXT_2 + ","
        + R.drawable.search_result + " AS " + SearchManager.SUGGEST_COLUMN_ICON_1 + ","
        + "'" + Intent.ACTION_VIEW + "' AS " + SearchManager.SUGGEST_COLUMN_INTENT_ACTION + ","
        + "'" + Notes.TextNote.CONTENT_TYPE + "' AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA;

    //使用上面定义的映射选择数据，指定数据库中的表
    //where子句包含三个条件：1.搜索SNIPPET列中包含特定模式的行；2.排除父id为回收站的id；3.只选择类型为note标签的行
    private static String NOTES_SNIPPET_SEARCH_QUERY = "SELECT " + NOTES_SEARCH_PROJECTION
        + " FROM " + TABLE.NOTE
        + " WHERE " + NoteColumns.SNIPPET + " LIKE ?"
        + " AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER
        + " AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE;

    //重写onCreate方法
    //使用getContext()获取当前组件的上下文，以便NotesDatabaseHelper能够访问应用程序的资源和其他功能
    //mHelper用于存储NotesDatabaseHelper.getInstance返回的实例，该实例就可以在整个组件的其他方法中被访问和使用
    @Override
    public boolean onCreate() {
        mHelper = NotesDatabaseHelper.getInstance(getContext());
        return true;
    }
    //查询数据
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        //Cursor对象c用于存储查询结果
        Cursor c = null;
        //使用NotesDatabaseHelper的实例mHelper获取可读的数据库实例
        SQLiteDatabase db = mHelper.getReadableDatabase();
        //字符串id存储从URI中解析出的ID
        String id = null;
        //根据匹配不同的URI来进行不同查询
        switch (mMatcher.match(uri)) {
            //查询整个NOTE表
            case URI_NOTE:
                c = db.query(TABLE.NOTE, projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;
            //查询NOTE表中的特定项，从URI路径中获取ID，并添加到查询条件
            case URI_NOTE_ITEM:
                id = uri.getPathSegments().get(1);
                c = db.query(TABLE.NOTE, projection, NoteColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs, null, null, sortOrder);
                break;
            //查询整个DATA表
            case URI_DATA:
                c = db.query(TABLE.DATA, projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;
            //查询DATA表中的特定项，从URI路径中获取ID，并添加到查询条件
            case URI_DATA_ITEM:
                id = uri.getPathSegments().get(1);
                c = db.query(TABLE.DATA, projection, DataColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs, null, null, sortOrder);
                break;
            //处理搜索查询
            case URI_SEARCH:
            case URI_SEARCH_SUGGEST:
                //检查是否提供了不应与搜索查询一起使用的参数
                if (sortOrder != null || projection != null) {
                    throw new IllegalArgumentException(//抛出IllegalArgumentException
                            "do not specify sortOrder, selection, selectionArgs, or projection" + "with this query");
                }
                //根据URI类型，从URI路径段或查询参数中获取搜索字符串searchString
                String searchString = null;
                if (mMatcher.match(uri) == URI_SEARCH_SUGGEST) {
                    if (uri.getPathSegments().size() > 1) {
                        searchString = uri.getPathSegments().get(1);
                    }
                } else {
                    searchString = uri.getQueryParameter("pattern");
                }

                if (TextUtils.isEmpty(searchString)) {
                    return null;
                }//如果searchString为空或无效，返回null
                //字符串格式化，格式化后的字符串就会是"%s%"，即包含s是任何文本，然后执行SQL查询
                try {
                    searchString = String.format("%%%s%%", searchString);
                    c = db.rawQuery(NOTES_SNIPPET_SEARCH_QUERY,
                            new String[] { searchString });
                } catch (IllegalStateException ex) {
                    Log.e(TAG, "got exception: " + ex.toString());
                }
                break;
            //处理未知URI
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        //如果查询结果不为空（即Cursor对象c不是null），则为其设置一个通知URI
        //这意味着当与这个URI关联的数据发生变化时，任何注册了监听这个URI的ContentObserver都会被通知
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }
    //插入数据，Uri用来标识要插入数据的表，ContentValues对象包含要插入的键值对
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        //获取数据库
        SQLiteDatabase db = mHelper.getWritableDatabase();
        //存储数据项ID、便签ID、插入行ID
        long dataId = 0, noteId = 0, insertedId = 0;
        switch (mMatcher.match(uri)) {
            //将values插入到TABLE.NOTE表中，并返回插入行的ID
            case URI_NOTE:
                insertedId = noteId = db.insert(TABLE.NOTE, null, values);
                break;
            //首先检查values是否包含DataColumns.NOTE_ID，如果包含，则获取其值。如果不包含，记录一条日志信息。然后，将values插入到TABLE.DATA表中，并返回插入行的ID
            case URI_DATA:
                if (values.containsKey(DataColumns.NOTE_ID)) {
                    noteId = values.getAsLong(DataColumns.NOTE_ID);
                } else {
                    Log.d(TAG, "Wrong data format without note id:" + values.toString());
                }
                insertedId = dataId = db.insert(TABLE.DATA, null, values);
                break;
            //如果uri不是已知的URI类型，则抛出一个IllegalArgumentException
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        // Notify the note uri通知变化
        //如果noteId或dataId大于0（即成功插入了数据），则使用ContentResolver的notifyChange方法通知监听这些URI的观察者，告知数据已经改变
        //ContentUris.withAppendedId方法用于在基本URI后面追加一个ID，形成完整的URI
        if (noteId > 0) {
            getContext().getContentResolver().notifyChange(
                    ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId), null);
        }

        // Notify the data uri
        if (dataId > 0) {
            getContext().getContentResolver().notifyChange(
                    ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, dataId), null);
        }
        //返回包含新插入数据项ID的Uri
        return ContentUris.withAppendedId(uri, insertedId);
    }
    //删除数据项
    //uri标记要删除数据的表或数据项
    //selection一个可选的where子句用于指定删除条件
    //一个可选的字符串数组，用于替换selection中的占位符
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;//记录被删除的行数
        String id = null;//存储从uri中解析出的数据项ID
        SQLiteDatabase db = mHelper.getWritableDatabase();//可写的数据库对象，用于执行删除操作
        boolean deleteData = false;//用于标记是否删除了DATA表中的数据
        switch (mMatcher.match(uri)) {
            //修改selection子句，确保删除的笔记ID大于0，然后执行删除操作并返回删除的行数
            case URI_NOTE:
                selection = "(" + selection + ") AND " + NoteColumns.ID + ">0 ";
                count = db.delete(TABLE.NOTE, selection, selectionArgs);
                break;
            //从uri中解析出ID，检查ID是否小于等于0，如果是则不执行删除操作，否则执行删除操作并返回被删除的行数
            case URI_NOTE_ITEM:
                id = uri.getPathSegments().get(1);
                /**
                 * ID that smaller than 0 is system folder which is not allowed to
                 * trash
                 */
                long noteId = Long.valueOf(id);
                if (noteId <= 0) {
                    break;
                }
                count = db.delete(TABLE.NOTE,
                        NoteColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                break;
            //执行删除操作并返回被删除的行数，设置deleteData为true，表示删除了DATA表中数据
            case URI_DATA:
                count = db.delete(TABLE.DATA, selection, selectionArgs);
                deleteData = true;
                break;
            //从uri中解析出ID，执行删除操作并返回被删除的行数，设置deleteData为true，表示删除了DATA表中数据
            case URI_DATA_ITEM:
                id = uri.getPathSegments().get(1);
                count = db.delete(TABLE.DATA,
                        DataColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                deleteData = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        //如果count大于0说明有数据被删除
        //如果deleteData表示为true，则通知监听Notes.CONTENT_NOTE_URI的观察者数据已改变
        //通知监听传入uri的观察者数据已改变
        if (count > 0) {
            if (deleteData) {
                getContext().getContentResolver().notifyChange(Notes.CONTENT_NOTE_URI, null);
            }
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    //更新数据库中的数据
    //uri标记要删除数据的表或数据项
    //values一个包含新值的键值对集合
    //selection一个可选的where子句，用于指定更新条件
    //selectionArgs字符串数组，用于替换selection中的占位符
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;//记录被更新的行数
        String id = null;//存储从uri中解析出的数据项ID
        SQLiteDatabase db = mHelper.getWritableDatabase();//可写的数据库对象，用于执行更新操作
        boolean updateData = false;//用于标记是否更新了DATA表中的数据
        switch (mMatcher.match(uri)) {
            //调用increaseNoteVersion方法（用于增加便签版本），然后在note表执行更新操作并返回被更新的行数
            case URI_NOTE:
                increaseNoteVersion(-1, selection, selectionArgs);
                count = db.update(TABLE.NOTE, values, selection, selectionArgs);
                break;
            //从URI中解析出ID，并调用increaseNoteVersion方法，传入解析出的ID，最后在note表执行更新操作并返回被更新的行数
            case URI_NOTE_ITEM:
                id = uri.getPathSegments().get(1);
                increaseNoteVersion(Long.valueOf(id), selection, selectionArgs);
                count = db.update(TABLE.NOTE, values, NoteColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs);
                break;
            //在data表执行更新操作并返回被更新的行数。设置updateData为true，表示更新了DATA表中的数据
            case URI_DATA:
                count = db.update(TABLE.DATA, values, selection, selectionArgs);
                updateData = true;
                break;
            //从URI中解析出ID。执行更新操作并返回被更新的行数。置updateData为true，表示更新了DATA表中的数据
            case URI_DATA_ITEM:
                id = uri.getPathSegments().get(1);
                count = db.update(TABLE.DATA, values, DataColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs);
                updateData = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        //如果count大于0说明有数据被更新
        //如果updateData表示为true，则通知监听Notes.CONTENT_NOTE_URI的观察者数据已改变
        //通知监听传入uri的观察者数据已改变
        if (count > 0) {
            if (updateData) {
                getContext().getContentResolver().notifyChange(Notes.CONTENT_NOTE_URI, null);
            }
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    //解析传入的条件语句：一个SQL Where子句的一部分
    private String parseSelection(String selection) {
        return (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
    }

    //更新note表的version列，将其值增加1
    private void increaseNoteVersion(long id, String selection, String[] selectionArgs) {
        StringBuilder sql = new StringBuilder(120);
        sql.append("UPDATE ");
        sql.append(TABLE.NOTE);
        sql.append(" SET ");
        sql.append(NoteColumns.VERSION);
        sql.append("=" + NoteColumns.VERSION + "+1 ");

        if (id > 0 || !TextUtils.isEmpty(selection)) {
            sql.append(" WHERE ");
        }
        if (id > 0) {
            sql.append(NoteColumns.ID + "=" + String.valueOf(id));
        }
        if (!TextUtils.isEmpty(selection)) {
            String selectString = id > 0 ? parseSelection(selection) : selection;
            for (String args : selectionArgs) {
                selectString = selectString.replaceFirst("\\?", args);
            }
            sql.append(selectString);
        }

        mHelper.getWritableDatabase().execSQL(sql.toString());
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

}

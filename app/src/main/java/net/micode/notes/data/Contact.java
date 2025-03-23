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

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;

/**
 * 联系人查询工具类，用于通过电话号码获取联系人姓名
 * 使用缓存机制优化查询性能
 */

public class Contact {
    // 缓存联系人信息的静态HashMap（电话号码 -> 姓名）
    private static HashMap<String, String> sContactCache;
    // 日志标签
    private static final String TAG = "Contact";
    /**
     * 构建查询条件语句的模板：
     * 1. 使用PHONE_NUMBERS_EQUAL匹配电话号码
     * 2. 限定数据类型为电话类型
     * 3. 通过phone_lookup表进行高效匹配
     * 原始语句：
     * PHONE_NUMBERS_EQUAL(phone_number,?)
     * AND mimetype='vnd.android.cursor.item/phone_v2'
     * AND raw_contact_id IN (
     *     SELECT raw_contact_id
     *     FROM phone_lookup
     *     WHERE min_match = '+'
     * )
     */
    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
    + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
    + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id "
            + " FROM phone_lookup"
            + " WHERE min_match = '+')";

    /**
     * 通过电话号码获取联系人姓名
     * @param context Android上下文
     * @param phoneNumber 要查询的电话号码
     * @return 联系人姓名，未找到时返回null
     */
    public static String getContact(Context context, String phoneNumber) {
//        初始化缓存（懒加载）
        if(sContactCache == null) {
            sContactCache = new HashMap<String, String>();
        }
//        首先检查缓存
        if(sContactCache.containsKey(phoneNumber)) {
            return sContactCache.get(phoneNumber);
        }
        // 构建完整查询条件：
        // 1. 将"+"替换为最小匹配位数（根据国家代码自动计算）
        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));
        // 执行内容解析器查询
        Cursor cursor = context.getContentResolver().query(
                Data.CONTENT_URI, // 查询的数据URI
                new String [] { Phone.DISPLAY_NAME },// 需要返回的列（联系人姓名）
                selection, // WHERE条件
                new String[] { phoneNumber }, // 查询参数（实际电话号码）
                null);// 排序方式

        if (cursor != null && cursor.moveToFirst()) {
            try {
                // 获取第一行第一列的数据（联系人姓名）
                String name = cursor.getString(0);
                // 将结果存入缓存
                sContactCache.put(phoneNumber, name);
                return name;
            } catch (IndexOutOfBoundsException e) {
                // 处理列索引越界异常
                Log.e(TAG, " Cursor get string error " + e.toString());
                return null;
            } finally {
                // 确保关闭Cursor释放资源
                cursor.close();
            }
        } else {
            // 记录未找到联系人的情况
            Log.d(TAG, "No contact matched with number:" + phoneNumber);
            return null;
        }
    }
}

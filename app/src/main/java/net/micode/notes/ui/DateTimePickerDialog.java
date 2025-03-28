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

import java.util.Calendar;

import net.micode.notes.R;
import net.micode.notes.ui.DateTimePicker;
import net.micode.notes.ui.DateTimePicker.OnDateTimeChangedListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

// 自定义的日期时间选择对话框
public class DateTimePickerDialog extends AlertDialog implements OnClickListener {

    // 当前日期时间的 Calendar 实例
    private Calendar mDate = Calendar.getInstance();
    // 是否为24小时制
    private boolean mIs24HourView;
    // 回调接口，用于通知日期时间设置完成
    private OnDateTimeSetListener mOnDateTimeSetListener;
    // 日期时间选择器
    private DateTimePicker mDateTimePicker;

    // 定义回调接口，用于通知日期时间设置完成
    public interface OnDateTimeSetListener {
        void OnDateTimeSet(AlertDialog dialog, long date);
    }

    // 构造函数
    public DateTimePickerDialog(Context context, long date) {
        super(context);
        // 初始化日期时间选择器
        mDateTimePicker = new DateTimePicker(context);
        // 将日期时间选择器设置为对话框的内容视图
        setView(mDateTimePicker);
        // 设置日期时间选择器的回调监听器
        mDateTimePicker.setOnDateTimeChangedListener(new OnDateTimeChangedListener() {
            public void onDateTimeChanged(DateTimePicker view, int year, int month,
                                          int dayOfMonth, int hourOfDay, int minute) {
                // 更新 Calendar 实例
                mDate.set(Calendar.YEAR, year);
                mDate.set(Calendar.MONTH, month);
                mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                mDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                mDate.set(Calendar.MINUTE, minute);
                // 更新对话框标题
                updateTitle(mDate.getTimeInMillis());
            }
        });
        // 设置当前日期时间
        mDate.setTimeInMillis(date);
        mDate.set(Calendar.SECOND, 0);
        mDateTimePicker.setCurrentDate(mDate.getTimeInMillis());
        // 设置“确定”按钮
        setButton(context.getString(R.string.datetime_dialog_ok), this);
        // 设置“取消”按钮
        setButton2(context.getString(R.string.datetime_dialog_cancel), (OnClickListener)null);
        // 设置是否为24小时制
        set24HourView(DateFormat.is24HourFormat(this.getContext()));
        // 更新对话框标题
        updateTitle(mDate.getTimeInMillis());
    }

    // 设置是否为24小时制
    public void set24HourView(boolean is24HourView) {
        mIs24HourView = is24HourView;
    }

    // 设置日期时间设置完成的回调接口
    public void setOnDateTimeSetListener(OnDateTimeSetListener callBack) {
        mOnDateTimeSetListener = callBack;
    }

    // 更新对话框标题
    private void updateTitle(long date) {
        // 定义日期时间格式标志
        int flag =
                DateUtils.FORMAT_SHOW_YEAR |
                        DateUtils.FORMAT_SHOW_DATE |
                        DateUtils.FORMAT_SHOW_TIME;
        // 根据是否为24小时制设置格式标志
        flag |= mIs24HourView ? DateUtils.FORMAT_24HOUR : DateUtils.FORMAT_12HOUR;
        // 设置对话框标题为当前日期时间的字符串表示
        setTitle(DateUtils.formatDateTime(this.getContext(), date, flag));
    }

    // “确定”按钮的点击事件处理
    public void onClick(DialogInterface arg0, int arg1) {
        // 如果设置了回调接口，则通知日期时间设置完成
        if (mOnDateTimeSetListener != null) {
            mOnDateTimeSetListener.OnDateTimeSet(this, mDate.getTimeInMillis());
        }
    }

}
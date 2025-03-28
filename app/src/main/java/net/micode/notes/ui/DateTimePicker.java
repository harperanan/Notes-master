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

 import java.text.DateFormatSymbols;
 import java.util.Calendar;
 
 import net.micode.notes.R;
 
 import android.content.Context;
 import android.text.format.DateFormat;
 import android.view.View;
 import android.widget.FrameLayout;
 import android.widget.NumberPicker;
 
 public class DateTimePicker extends FrameLayout {
 
     // 默认启用状态
     private static final boolean DEFAULT_ENABLE_STATE = true;
 
     // 12小时制的一半
     private static final int HOURS_IN_HALF_DAY = 12;
     // 24小时制
     private static final int HOURS_IN_ALL_DAY = 24;
     // 一周的天数
     private static final int DAYS_IN_ALL_WEEK = 7;
     // 日期选择器的最小值
     private static final int DATE_SPINNER_MIN_VAL = 0;
     // 日期选择器的最大值
     private static final int DATE_SPINNER_MAX_VAL = DAYS_IN_ALL_WEEK - 1;
     // 24小时制的最小小时值
     private static final int HOUR_SPINNER_MIN_VAL_24_HOUR_VIEW = 0;
     // 24小时制的最大小时值
     private static final int HOUR_SPINNER_MAX_VAL_24_HOUR_VIEW = 23;
     // 12小时制的最小小时值
     private static final int HOUR_SPINNER_MIN_VAL_12_HOUR_VIEW = 1;
     // 12小时制的最大小时值
     private static final int HOUR_SPINNER_MAX_VAL_12_HOUR_VIEW = 12;
     // 分钟选择器的最小值
     private static final int MINUT_SPINNER_MIN_VAL = 0;
     // 分钟选择器的最大值
     private static final int MINUT_SPINNER_MAX_VAL = 59;
     // AM/PM选择器的最小值
     private static final int AMPM_SPINNER_MIN_VAL = 0;
     // AM/PM选择器的最大值
     private static final int AMPM_SPINNER_MAX_VAL = 1;
 
     // 日期选择器
     private final NumberPicker mDateSpinner;
     // 小时选择器
     private final NumberPicker mHourSpinner;
     // 分钟选择器
     private final NumberPicker mMinuteSpinner;
     // AM/PM选择器
     private final NumberPicker mAmPmSpinner;
     // 用于存储当前日期和时间的 Calendar 实例
     private Calendar mDate;
 
     // 日期显示值数组
     private String[] mDateDisplayValues = new String[DAYS_IN_ALL_WEEK];
 
     // 是否为上午（AM）
     private boolean mIsAm;
 
     // 是否为24小时制
     private boolean mIs24HourView;
 
     // 是否启用
     private boolean mIsEnabled = DEFAULT_ENABLE_STATE;
 
     // 是否正在初始化
     private boolean mInitialising;
 
     // 回调接口，用于通知日期和时间变化
     private OnDateTimeChangedListener mOnDateTimeChangedListener;
 
     // 日期选择器的值变化监听器
     private NumberPicker.OnValueChangeListener mOnDateChangedListener = new NumberPicker.OnValueChangeListener() {
         @Override
         public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
             // 更新日期
             mDate.add(Calendar.DAY_OF_YEAR, newVal - oldVal);
             // 更新日期控件显示
             updateDateControl();
             // 通知日期和时间变化
             onDateTimeChanged();
         }
     };
 
     // 小时选择器的值变化监听器
     private NumberPicker.OnValueChangeListener mOnHourChangedListener = new NumberPicker.OnValueChangeListener() {
         @Override
         public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
             // 是否需要更新日期
             boolean isDateChanged = false;
             // 获取当前时间
             Calendar cal = Calendar.getInstance();
             if (!mIs24HourView) {
                 // 12小时制逻辑
                 if (!mIsAm && oldVal == HOURS_IN_HALF_DAY - 1 && newVal == HOURS_IN_HALF_DAY) {
                     // 如果从11点变为12点（PM），日期加1
                     cal.setTimeInMillis(mDate.getTimeInMillis());
                     cal.add(Calendar.DAY_OF_YEAR, 1);
                     isDateChanged = true;
                 } else if (mIsAm && oldVal == HOURS_IN_HALF_DAY && newVal == HOURS_IN_HALF_DAY - 1) {
                     // 如果从12点变为11点（AM），日期减1
                     cal.setTimeInMillis(mDate.getTimeInMillis());
                     cal.add(Calendar.DAY_OF_YEAR, -1);
                     isDateChanged = true;
                 }
                 // 切换AM/PM
                 if (oldVal == HOURS_IN_HALF_DAY - 1 && newVal == HOURS_IN_HALF_DAY ||
                         oldVal == HOURS_IN_HALF_DAY && newVal == HOURS_IN_HALF_DAY - 1) {
                     mIsAm = !mIsAm;
                     updateAmPmControl();
                 }
             } else {
                 // 24小时制逻辑
                 if (oldVal == HOURS_IN_ALL_DAY - 1 && newVal == 0) {
                     // 如果从23点变为0点，日期加1
                     cal.setTimeInMillis(mDate.getTimeInMillis());
                     cal.add(Calendar.DAY_OF_YEAR, 1);
                     isDateChanged = true;
                 } else if (oldVal == 0 && newVal == HOURS_IN_ALL_DAY - 1) {
                     // 如果从0点变为23点，日期减1
                     cal.setTimeInMillis(mDate.getTimeInMillis());
                     cal.add(Calendar.DAY_OF_YEAR, -1);
                     isDateChanged = true;
                 }
             }
             // 计算新的小时值
             int newHour = mHourSpinner.getValue() % HOURS_IN_HALF_DAY + (mIsAm ? 0 : HOURS_IN_HALF_DAY);
             // 设置新的小时
             mDate.set(Calendar.HOUR_OF_DAY, newHour);
             // 通知日期和时间变化
             onDateTimeChanged();
             // 如果日期发生变化，更新年、月、日
             if (isDateChanged) {
                 setCurrentYear(cal.get(Calendar.YEAR));
                 setCurrentMonth(cal.get(Calendar.MONTH));
                 setCurrentDay(cal.get(Calendar.DAY_OF_MONTH));
             }
         }
     };
 
     // 分钟选择器的值变化监听器
     private NumberPicker.OnValueChangeListener mOnMinuteChangedListener = new NumberPicker.OnValueChangeListener() {
         @Override
         public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
             // 获取分钟选择器的最小值和最大值
             int minValue = mMinuteSpinner.getMinValue();
             int maxValue = mMinuteSpinner.getMaxValue();
             // 计算偏移量
             int offset = 0;
             if (oldVal == maxValue && newVal == minValue) {
                 offset += 1;
             } else if (oldVal == minValue && newVal == maxValue) {
                 offset -= 1;
             }
             // 如果有偏移量，更新日期和小时
             if (offset != 0) {
                 mDate.add(Calendar.HOUR_OF_DAY, offset);
                 mHourSpinner.setValue(getCurrentHour());
                 updateDateControl();
                 int newHour = getCurrentHourOfDay();
                 if (newHour >= HOURS_IN_HALF_DAY) {
                     mIsAm = false;
                     updateAmPmControl();
                 } else {
                     mIsAm = true;
                     updateAmPmControl();
                 }
             }
             // 设置新的分钟
             mDate.set(Calendar.MINUTE, newVal);
             // 通知日期和时间变化
             onDateTimeChanged();
         }
     };
 
     // AM/PM选择器的值变化监听器
     private NumberPicker.OnValueChangeListener mOnAmPmChangedListener = new NumberPicker.OnValueChangeListener() {
         @Override
         public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
             // 切换AM/PM状态
             mIsAm = !mIsAm;
             // 根据AM/PM状态调整小时
             if (mIsAm) {
                 mDate.add(Calendar.HOUR_OF_DAY, -HOURS_IN_HALF_DAY);
             } else {
                 mDate.add(Calendar.HOUR_OF_DAY, HOURS_IN_HALF_DAY);
             }
             // 更新AM/PM控件显示
             updateAmPmControl();
             // 通知日期和时间变化
             onDateTimeChanged();
         }
     };
 
     // 回调接口，用于通知日期和时间变化
     public interface OnDateTimeChangedListener {
         void onDateTimeChanged(DateTimePicker view, int year, int month,
                                int dayOfMonth, int hourOfDay, int minute);
     }
 
     // 构造函数
     public DateTimePicker(Context context) {
         this(context, System.currentTimeMillis());
     }
 
     // 构造函数
     public DateTimePicker(Context context, long date) {
         this(context, date, DateFormat.is24HourFormat(context));
     }
 
     // 构造函数
     public DateTimePicker(Context context, long date, boolean is24HourView) {
         super(context);
         // 初始化Calendar实例
         mDate = Calendar.getInstance();
         // 设置初始化状态
         mInitialising = true;
         // 判断当前时间是否为下午
         mIsAm = getCurrentHourOfDay() >= HOURS_IN_HALF_DAY;
         // 加载布局
         inflate(context, R.layout.datetime_picker, this);
 
         // 初始化日期选择器
         mDateSpinner = (NumberPicker) findViewById(R.id.date);
         mDateSpinner.setMinValue(DATE_SPINNER_MIN_VAL);
         mDateSpinner.setMaxValue(DATE_SPINNER_MAX_VAL);
         mDateSpinner.setOnValueChangedListener(mOnDateChangedListener);
 
         // 初始化小时选择器
         mHourSpinner = (NumberPicker) findViewById(R.id.hour);
         mHourSpinner.setOnValueChangedListener(mOnHourChangedListener);
         // 初始化分钟选择器
         mMinuteSpinner =  (NumberPicker) findViewById(R.id.minute);
         mMinuteSpinner.setMinValue(MINUT_SPINNER_MIN_VAL);
         mMinuteSpinner.setMaxValue(MINUT_SPINNER_MAX_VAL);
         mMinuteSpinner.setOnLongPressUpdateInterval(100);
         mMinuteSpinner.setOnValueChangedListener(mOnMinuteChangedListener);
 
         // 初始化AM/PM选择器
         String[] stringsForAmPm = new DateFormatSymbols().getAmPmStrings();
         mAmPmSpinner = (NumberPicker) findViewById(R.id.amPm);
         mAmPmSpinner.setMinValue(AMPM_SPINNER_MIN_VAL);
         mAmPmSpinner.setMaxValue(AMPM_SPINNER_MAX_VAL);
         mAmPmSpinner.setDisplayedValues(stringsForAmPm);
         mAmPmSpinner.setOnValueChangedListener(mOnAmPmChangedListener);
 
         // 更新控件到初始状态
         updateDateControl();
         updateHourControl();
         updateAmPmControl();
 
         // 设置24小时制或12小时制
         set24HourView(is24HourView);
 
         // 设置当前时间
         setCurrentDate(date);
 
         // 设置启用状态
         setEnabled(isEnabled());
 
         // 设置内容描述
         mInitialising = false;
     }
 
     // 设置启用状态
     @Override
     public void setEnabled(boolean enabled) {
         if (mIsEnabled == enabled) {
             return;
         }
         super.setEnabled(enabled);
         mDateSpinner.setEnabled(enabled);
         mMinuteSpinner.setEnabled(enabled);
         mHourSpinner.setEnabled(enabled);
         mAmPmSpinner.setEnabled(enabled);
         mIsEnabled = enabled;
     }
 
     // 获取启用状态
     @Override
     public boolean isEnabled() {
         return mIsEnabled;
     }
 
     // 获取当前日期和时间的毫秒值
     public long getCurrentDateInTimeMillis() {
         return mDate.getTimeInMillis();
     }
 
     // 设置当前日期和时间
     public void setCurrentDate(long date) {
         Calendar cal = Calendar.getInstance();
         cal.setTimeInMillis(date);
         setCurrentDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
                 cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
     }
 
     // 设置当前日期和时间
     public void setCurrentDate(int year, int month,
                                int dayOfMonth, int hourOfDay, int minute) {
         setCurrentYear(year);
         setCurrentMonth(month);
         setCurrentDay(dayOfMonth);
         setCurrentHour(hourOfDay);
         setCurrentMinute(minute);
     }
 
     // 获取当前年份
     public int getCurrentYear() {
         return mDate.get(Calendar.YEAR);
     }
 
     // 设置当前年份
     public void setCurrentYear(int year) {
         if (!mInitialising && year == getCurrentYear()) {
             return;
         }
         mDate.set(Calendar.YEAR, year);
         updateDateControl();
         onDateTimeChanged();
     }
 
     // 获取当前月份
     public int getCurrentMonth() {
         return mDate.get(Calendar.MONTH);
     }
 
     // 设置当前月份
     public void setCurrentMonth(int month) {
         if (!mInitialising && month == getCurrentMonth()) {
             return;
         }
         mDate.set(Calendar.MONTH, month);
         updateDateControl();
         onDateTimeChanged();
     }
 
     // 获取当前日期
     public int getCurrentDay() {
         return mDate.get(Calendar.DAY_OF_MONTH);
     }
 
     // 设置当前日期
     public void setCurrentDay(int dayOfMonth) {
         if (!mInitialising && dayOfMonth == getCurrentDay()) {
             return;
         }
         mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
         updateDateControl();
         onDateTimeChanged();
     }
 
     // 获取当前小时（24小时制）
     public int getCurrentHourOfDay() {
         return mDate.get(Calendar.HOUR_OF_DAY);
     }
 
     // 获取当前小时（根据24小时制或12小时制）
     private int getCurrentHour() {
         if (mIs24HourView){
             return getCurrentHourOfDay();
         } else {
             int hour = getCurrentHourOfDay();
             if (hour > HOURS_IN_HALF_DAY) {
                 return hour - HOURS_IN_HALF_DAY;
             } else {
                 return hour == 0 ? HOURS_IN_HALF_DAY : hour;
             }
         }
     }
 
     // 设置当前小时
     public void setCurrentHour(int hourOfDay) {
         if (!mInitialising && hourOfDay == getCurrentHourOfDay()) {
             return;
         }
         mDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
         if (!mIs24HourView) {
             if (hourOfDay >= HOURS_IN_HALF_DAY) {
                 mIsAm = false;
                 if (hourOfDay > HOURS_IN_HALF_DAY) {
                     hourOfDay -= HOURS_IN_HALF_DAY;
                 }
             } else {
                 mIsAm = true;
                 if (hourOfDay == 0) {
                     hourOfDay = HOURS_IN_HALF_DAY;
                 }
             }
             updateAmPmControl();
         }
         mHourSpinner.setValue(hourOfDay);
         onDateTimeChanged();
     }
 
     // 获取当前分钟
     public int getCurrentMinute() {
         return mDate.get(Calendar.MINUTE);
     }
 
     // 设置当前分钟
     public void setCurrentMinute(int minute) {
         if (!mInitialising && minute == getCurrentMinute()) {
             return;
         }
         mMinuteSpinner.setValue(minute);
         mDate.set(Calendar.MINUTE, minute);
         onDateTimeChanged();
     }
 
     // 获取是否为24小时制
     public boolean is24HourView () {
         return mIs24HourView;
     }
 
     // 设置是否为24小时制
     public void set24HourView(boolean is24HourView) {
         if (mIs24HourView == is24HourView) {
             return;
         }
         mIs24HourView = is24HourView;
         mAmPmSpinner.setVisibility(is24HourView ? View.GONE : View.VISIBLE);
         int hour = getCurrentHourOfDay();
         updateHourControl();
         setCurrentHour(hour);
         updateAmPmControl();
     }
 
     // 更新日期控件显示
     private void updateDateControl() {
         Calendar cal = Calendar.getInstance();
         cal.setTimeInMillis(mDate.getTimeInMillis());
         cal.add(Calendar.DAY_OF_YEAR, -DAYS_IN_ALL_WEEK / 2 - 1);
         mDateSpinner.setDisplayedValues(null);
         for (int i = 0; i < DAYS_IN_ALL_WEEK; ++i) {
             cal.add(Calendar.DAY_OF_YEAR, 1);
             mDateDisplayValues[i] = (String) DateFormat.format("MM.dd EEEE", cal);
         }
         mDateSpinner.setDisplayedValues(mDateDisplayValues);
         mDateSpinner.setValue(DAYS_IN_ALL_WEEK / 2);
         mDateSpinner.invalidate();
     }
 
     // 更新AM/PM控件显示
     private void updateAmPmControl() {
         if (mIs24HourView) {
             mAmPmSpinner.setVisibility(View.GONE);
         } else {
             int index = mIsAm ? Calendar.AM : Calendar.PM;
             mAmPmSpinner.setValue(index);
             mAmPmSpinner.setVisibility(View.VISIBLE);
         }
     }
 
     // 更新小时控件显示
     private void updateHourControl() {
         if (mIs24HourView) {
             mHourSpinner.setMinValue(HOUR_SPINNER_MIN_VAL_24_HOUR_VIEW);
             mHourSpinner.setMaxValue(HOUR_SPINNER_MAX_VAL_24_HOUR_VIEW);
         } else {
             mHourSpinner.setMinValue(HOUR_SPINNER_MIN_VAL_12_HOUR_VIEW);
             mHourSpinner.setMaxValue(HOUR_SPINNER_MAX_VAL_12_HOUR_VIEW);
         }
     }
 
     // 设置回调接口
     public void setOnDateTimeChangedListener(OnDateTimeChangedListener callback) {
         mOnDateTimeChangedListener = callback;
     }
 
     // 通知日期和时间变化
     private void onDateTimeChanged() {
         if (mOnDateTimeChangedListener != null) {
             mOnDateTimeChangedListener.onDateTimeChanged(this, getCurrentYear(),
                     getCurrentMonth(), getCurrentDay(), getCurrentHourOfDay(), getCurrentMinute());
         }
     }
 }
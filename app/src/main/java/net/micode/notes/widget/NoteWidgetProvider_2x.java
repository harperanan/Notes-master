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

package net.micode.notes.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.ResourceParser;

/**
 * 定义了一个具体的笔记小部件实现类 NoteWidgetProvider_2x。
 * 继承自抽象基类 NoteWidgetProvider，并实现了其抽象方法。
 * 专门管理2x类型的小部件
 */

public class NoteWidgetProvider_2x extends NoteWidgetProvider {

    //覆盖了 AppWidgetProvider 的 onUpdate 方法。
    //当小部件需要更新时调用此方法（例如首次添加小部件或系统触发更新）。
    //调用了父类的 update 方法来执行具体的小部件更新逻辑。
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.update(context, appWidgetManager, appWidgetIds);
    }

    //实现了父类的抽象方法 getLayoutId。
    //返回小部件的布局资源 ID，指定为 R.layout.widget_2x。
    //这个布局文件定义了小部件的 UI 结构，适用于 2x 小部件类型。
    @Override
    protected int getLayoutId() {
        return R.layout.widget_2x;
    }

    //实现了父类的抽象方法 getBgResourceId。
    //根据传入的背景颜色，返回对应的资源 ID,用于获取 2x 小部件的背景资源。
    @Override
    protected int getBgResourceId(int bgId) {
        return ResourceParser.WidgetBgResources.getWidget2xBgResource(bgId);
    }

    //实现了父类的抽象方法 getWidgetType。
    //返回当前小部件的类型，指定为 Notes.TYPE_WIDGET_2X。
    //这个值通常用于区分不同类型的笔记小部件1x、2x、4x 等
    @Override
    protected int getWidgetType() {
        return Notes.TYPE_WIDGET_2X;
    }
}

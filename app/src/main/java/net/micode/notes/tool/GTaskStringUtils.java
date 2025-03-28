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

/**
 * Google Tasks API 交互常量工具类
 * 包含与GTasks API通信时使用的JSON键名、操作类型标识、元数据标识等
 */
public class GTaskStringUtils {

    //json字段键名常量，操作类型常量
    /** 操作ID标识 */
    public final static String GTASK_JSON_ACTION_ID = "action_id";
    /** 操作列表标识 */
    public final static String GTASK_JSON_ACTION_LIST = "action_list";
    /** 操作类型标识（create/update/move等） */
    public final static String GTASK_JSON_ACTION_TYPE = "action_type";
    /** 创建操作类型 */
    public final static String GTASK_JSON_ACTION_TYPE_CREATE = "create";
    /** 全量获取操作类型 */
    public final static String GTASK_JSON_ACTION_TYPE_GETALL = "get_all";
    /** 移动操作类型*/
    public final static String GTASK_JSON_ACTION_TYPE_MOVE = "move";
    /** 更新操作类型 */
    public final static String GTASK_JSON_ACTION_TYPE_UPDATE = "update";
     /** 创建者ID标识 */
    public final static String GTASK_JSON_CREATOR_ID = "creator_id";
    /** 子实体集合标识 */
    public final static String GTASK_JSON_CHILD_ENTITY = "child_entity";
    /** 客户端版本标识 */
    public final static String GTASK_JSON_CLIENT_VERSION = "client_version";
    /** 完成状态标识 */
    public final static String GTASK_JSON_COMPLETED = "completed";
    /** 当前清单ID标识 */
    public final static String GTASK_JSON_CURRENT_LIST_ID = "current_list_id";
    /** 默认清单ID标识 */
    public final static String GTASK_JSON_DEFAULT_LIST_ID = "default_list_id";
    /** 删除状态标识 */
    public final static String GTASK_JSON_DELETED = "deleted";
    /** 目标清单标识 */
    public final static String GTASK_JSON_DEST_LIST = "dest_list";
    /** 目标父级标识 */
    public final static String GTASK_JSON_DEST_PARENT = "dest_parent";
    /** 目标父级类型标识 */
    public final static String GTASK_JSON_DEST_PARENT_TYPE = "dest_parent_type";
    /** 实体变更标识 */
    public final static String GTASK_JSON_ENTITY_DELTA = "entity_delta";
    /** 实体类型标识 */
    public final static String GTASK_JSON_ENTITY_TYPE = "entity_type";
    /** 获取已删除项标识 */
    public final static String GTASK_JSON_GET_DELETED = "get_deleted";
    /** 唯一ID标识 */
    public final static String GTASK_JSON_ID = "id";
    /** 索引位置标识 */
    public final static String GTASK_JSON_INDEX = "index";
    /** 最后修改时间标识 */
    public final static String GTASK_JSON_LAST_MODIFIED = "last_modified";
    /** 最新同步点标识 */
    public final static String GTASK_JSON_LATEST_SYNC_POINT = "latest_sync_point";
    /** 清单ID标识 */
    public final static String GTASK_JSON_LIST_ID = "list_id";
    /** 清单集合标识 */
    public final static String GTASK_JSON_LISTS = "lists";
    /** 名称字段标识 */
    public final static String GTASK_JSON_NAME = "name";
    /** 新ID标识（用于ID变更场景） */
    public final static String GTASK_JSON_NEW_ID = "new_id";
    /** 笔记集合标识 */
    public final static String GTASK_JSON_NOTES = "notes";
    /** 父级ID标识 */
    public final static String GTASK_JSON_PARENT_ID = "parent_id";
    /** 前兄弟节点ID标识 */
    public final static String GTASK_JSON_PRIOR_SIBLING_ID = "prior_sibling_id";
    /** 结果集标识 */
    public final static String GTASK_JSON_RESULTS = "results";
    /** 源清单标识 */
    public final static String GTASK_JSON_SOURCE_LIST = "source_list";
    /** 任务集合标识 */
    public final static String GTASK_JSON_TASKS = "tasks";
    /** 类型标识 */
    public final static String GTASK_JSON_TYPE = "type";
    /** 分组类型（如文件夹） */
    public final static String GTASK_JSON_TYPE_GROUP = "GROUP";
    /** 任务类型 */
    public final static String GTASK_JSON_TYPE_TASK = "TASK";
    /** 用户信息标识 */
    public final static String GTASK_JSON_USER = "user";
    /** MIUI笔记文件夹前缀（用于云端同步识别） */
    public final static String MIUI_FOLDER_PREFFIX = "[MIUI_Notes]";
    /** 默认文件夹名称 */
    public final static String FOLDER_DEFAULT = "Default";
    /** 通话记录文件夹名称 */
    public final static String FOLDER_CALL_NOTE = "Call_Note";
    /** 元数据文件夹标识 */
    public final static String FOLDER_META = "METADATA";
    /** 元数据头-Google任务ID */
    public final static String META_HEAD_GTASK_ID = "meta_gid";
    /** 元数据头-笔记信息 */
    public final static String META_HEAD_NOTE = "meta_note";
    /** 元数据头-数据内容 */
    public final static String META_HEAD_DATA = "meta_data";
    /** 元数据笔记名称（禁止用户操作的特殊笔记） */
    public final static String META_NOTE_NAME = "[META INFO] DON'T UPDATE AND DELETE";

}

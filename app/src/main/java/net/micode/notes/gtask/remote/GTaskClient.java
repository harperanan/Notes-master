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

package net.micode.notes.gtask.remote;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.gtask.data.Node;
import net.micode.notes.gtask.data.Task;
import net.micode.notes.gtask.data.TaskList;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.gtask.exception.NetworkFailureException;
import net.micode.notes.tool.GTaskStringUtils;
import net.micode.notes.ui.NotesPreferenceActivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

//实现GTASK的登陆操作，进行GTASK任务的创建，创建任务列表，从网络上获取任务和任务列表的内容
//主要使用类或技术：accountManager、JSONObject、HttoParams、authToken、Gid
public class GTaskClient {
    private static final String TAG = GTaskClient.class.getSimpleName();//日志标签

    private static final String GTASK_URL = "https://mail.google.com/tasks/";//GTASK基础URL

    private static final String GTASK_GET_URL = "https://mail.google.com/tasks/ig";//获取任务的URL

    private static final String GTASK_POST_URL = "https://mail.google.com/tasks/r/ig";//提交任务的URL

    private static GTaskClient mInstance = null;//单例实例

    private DefaultHttpClient mHttpClient;//HTTP客户端

    private String mGetUrl;//获取任务的URL

    private String mPostUrl;//提交任务的URL

    private long mClientVersion;//客户端版本

    private boolean mLoggedin;//登录状态

    private long mLastLoginTime;//上次登录时间

    private int mActionId;//操作ID

    private Account mAccount;//当前账号

    private JSONArray mUpdateArray;//更新任务列表的JSON数组

    //初始化变量
    private GTaskClient() {
        mHttpClient = null;
        mGetUrl = GTASK_GET_URL;
        mPostUrl = GTASK_POST_URL;
        mClientVersion = -1;
        mLoggedin = false;
        mLastLoginTime = 0;
        mActionId = 1;
        mAccount = null;
        mUpdateArray = null;
    }

    //获取实例化对象
    public static synchronized GTaskClient getInstance() {
        if (mInstance == null) {
            mInstance = new GTaskClient();
        }
        return mInstance;
    }

    //实现登陆操作
    //设置登录操作限制时间，如果超时则重新登录
    //两种登录方式：1.使用自己的URL登录；2.使用谷歌官方的URL登录
    //返回true或者false表示登录结果
    public boolean login(Activity activity) {
        // we suppose that the cookie would expire after 5 minutes
        // then we need to re-login
        final long interval = 1000 * 60 * 5;//判断登录操作是否超过5分钟
        if (mLastLoginTime + interval < System.currentTimeMillis()) {
            mLoggedin = false;
        }
        //重新登录
        // need to re-login after account switch
        if (mLoggedin
                && !TextUtils.equals(getSyncAccount().name, NotesPreferenceActivity
                        .getSyncAccountName(activity))) {
            mLoggedin = false;
        }
        //未超时则不需要重新登录
        if (mLoggedin) {
            Log.d(TAG, "already logged in");
            return true;
        }

        //更新最近一次的登录时间，改为系统当前的时间
        mLastLoginTime = System.currentTimeMillis();
        //判断是否登录到谷歌账户
        String authToken = loginGoogleAccount(activity, false);
        if (authToken == null) {
            Log.e(TAG, "login google account failed");
            return false;
        }

        //使用用户自己的域名登录
        // login with custom domain if necessary
        //将用户账户名改为统一格式后判断是否为google账户地址
        if (!(mAccount.name.toLowerCase().endsWith("gmail.com") || mAccount.name.toLowerCase()
                .endsWith("googlemail.com"))) {
            StringBuilder url = new StringBuilder(GTASK_URL).append("a/");
            int index = mAccount.name.indexOf('@') + 1;
            String suffix = mAccount.name.substring(index);
            url.append(suffix + "/");
            mGetUrl = url.toString() + "ig";//设置用户对应的getUrl
            mPostUrl = url.toString() + "r/ig";//设置用户对应的postUrl

            if (tryToLoginGtask(activity, authToken)) {
                mLoggedin = true;
            }
        }

        //如果用户账户无法登录，使用谷歌官方的URL进行登录
        // try to login with google official url
        if (!mLoggedin) {
            mGetUrl = GTASK_GET_URL;
            mPostUrl = GTASK_POST_URL;
            if (!tryToLoginGtask(activity, authToken)) {
                return false;
            }
        }

        mLoggedin = true;
        return true;
    }

    //登录谷歌账户的办法
    //1.使用令牌机制；2.使用AccontManager管理注册账号；3.返回值是账号的令牌
    private String loginGoogleAccount(Activity activity, boolean invalidateToken) {
        String authToken;//令牌，登录操作保证安全性
        AccountManager accountManager = AccountManager.get(activity);//AccountManager给用户提供了集中注册账号的接口
        //获取全部以com.google结尾的账号
        Account[] accounts = accountManager.getAccountsByType("com.google");

        if (accounts.length == 0) {
            Log.e(TAG, "there is no available google account");
            return null;
        }

        String accountName = NotesPreferenceActivity.getSyncAccountName(activity);
        Account account = null;
        //遍历获得的账号信息，寻找已经记录过的账户信息
        for (Account a : accounts) {
            if (a.name.equals(accountName)) {
                account = a;
                break;
            }
        }
        if (account != null) {
            mAccount = account;
        } else {
            Log.e(TAG, "unable to get an account with the same name in the settings");
            return null;
        }

        //获取选中账号的令牌
        // get the token now
        AccountManagerFuture<Bundle> accountManagerFuture = accountManager.getAuthToken(account,
                "goanna_mobile", null, activity, null, null);
        try {
            Bundle authTokenBundle = accountManagerFuture.getResult();
            authToken = authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN);
            //如果是invalidateToken，需要调用invalidateAuthToken()方法废除这个无效token
            if (invalidateToken) {
                accountManager.invalidateAuthToken("com.google", authToken);
                loginGoogleAccount(activity, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "get auth token failed");
            authToken = null;
        }

        return authToken;
    }

    //尝试登录GTASK，只是预先判断令牌是否有效以及能否登上GTASK的方法，而不是具体实现登录的方法
    private boolean tryToLoginGtask(Activity activity, String authToken) {
        if (!loginGtask(authToken)) {
            // maybe the auth token is out of date, now let's invalidate the
            // token and try again
            //删除过一个无效的authToken，申请一个新的后重新尝试登录
            authToken = loginGoogleAccount(activity, true);
            if (authToken == null) {
                Log.e(TAG, "login google account failed");
                return false;
            }

            if (!loginGtask(authToken)) {
                Log.e(TAG, "login gtask failed");
                return false;
            }
        }
        return true;
    }

    //实现登录GTASK的具体操作
    private boolean loginGtask(String authToken) {
        int timeoutConnection = 10000;
        int timeoutSocket = 15000;//socket是一种通信连接实现数据的交换的端口
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        mHttpClient = new DefaultHttpClient(httpParameters);
        //设置本地cookie
        BasicCookieStore localBasicCookieStore = new BasicCookieStore();
        mHttpClient.setCookieStore(localBasicCookieStore);
        HttpProtocolParams.setUseExpectContinue(mHttpClient.getParams(), false);

        // login gtask
        try {
            String loginUrl = mGetUrl + "?auth=" + authToken;//设置登录的url
            HttpGet httpGet = new HttpGet(loginUrl);//通过登录的uri实例化网页上资源的查找
            HttpResponse response = null;
            response = mHttpClient.execute(httpGet);

            //获取CookieStore里存放的cookie,看如果存有“GTL”，则说明有验证成功的有效的cookie
            // get the cookie now
            List<Cookie> cookies = mHttpClient.getCookieStore().getCookies();
            boolean hasAuthCookie = false;
            for (Cookie cookie : cookies) {
                if (cookie.getName().contains("GTL")) {
                    hasAuthCookie = true;
                }
            }
            if (!hasAuthCookie) {
                Log.w(TAG, "it seems that there is no auth cookie");
            }

            //获取client的内容，具体操作是在返回的Content中截取从_setup(开始到)}</script>中间的字符串内容，也就是gtask_url的内容
            // get the client version
            String resString = getResponseContent(response.getEntity());
            String jsBegin = "_setup(";
            String jsEnd = ")}</script>";
            int begin = resString.indexOf(jsBegin);
            int end = resString.lastIndexOf(jsEnd);
            String jsString = null;
            if (begin != -1 && end != -1 && begin < end) {
                jsString = resString.substring(begin + jsBegin.length(), end);
            }
            JSONObject js = new JSONObject(jsString);
            mClientVersion = js.getLong("v");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            // simply catch all exceptions
            Log.e(TAG, "httpget gtask_url failed");
            return false;
        }

        return true;
    }

    private int getActionId() {
        return mActionId++;
    }

    //创建一个用于向网络传输数据的对象
    private HttpPost createHttpPost() {
        HttpPost httpPost = new HttpPost(mPostUrl);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        httpPost.setHeader("AT", "1");
        return httpPost;
    }

    //通过URL获取响应后返回的数据，也就是网络上的数据和资源
    private String getResponseContent(HttpEntity entity) throws IOException {
        String contentEncoding = null;
        //通过URL得到HttpEntity对象，如果不为空则使用getContent()方法创建一个流将数据从网络都过来
        if (entity.getContentEncoding() != null) {
            contentEncoding = entity.getContentEncoding().getValue();
            Log.d(TAG, "encoding: " + contentEncoding);
        }

        InputStream input = entity.getContent();
        //GZIP是使用DEFLATE进行压缩数据的另一个压缩库
        if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
            input = new GZIPInputStream(entity.getContent());
        } else if (contentEncoding != null && contentEncoding.equalsIgnoreCase("deflate")) {
            Inflater inflater = new Inflater(true);//DEFLATE是一个无专利的压缩算法，可以实现无损数据压缩
            input = new InflaterInputStream(entity.getContent(), inflater);
        }

        try {
            InputStreamReader isr = new InputStreamReader(input);
            BufferedReader br = new BufferedReader(isr);//是一个包装类，它可以包装字符流，将字符流放入缓存里，先把字符读到缓存里，到缓存满了时候，再读入内存，是为了提供读的效率而设计的
            StringBuilder sb = new StringBuilder();

            while (true) {
                String buff = br.readLine();
                if (buff == null) {
                    return sb.toString();
                }
                sb = sb.append(buff);
            }
        } finally {
            input.close();
        }
    }

    //通过JSON发送请求
    //利用UrlEncodedFormEntity entity和httpPost.setEntity(entity)方法把js中的内容放置到httpPost中
    //执行请求后使用getResponseContent方法得到返回的数据和资源
    //将资源再次放入json后返回
    private JSONObject postRequest(JSONObject js) throws NetworkFailureException {
        if (!mLoggedin) {//未登录
            Log.e(TAG, "please login first");
            throw new ActionFailureException("not logged in");
        }

        //实例化一个httpPost的对象用来向服务器传输数据，在这里就是发送请求，而请求的内容在js里
        HttpPost httpPost = createHttpPost();
        try {
            LinkedList<BasicNameValuePair> list = new LinkedList<BasicNameValuePair>();
            list.add(new BasicNameValuePair("r", js.toString()));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, "UTF-8");
            httpPost.setEntity(entity);

            //执行请求
            // execute the post
            HttpResponse response = mHttpClient.execute(httpPost);
            String jsString = getResponseContent(response.getEntity());
            return new JSONObject(jsString);

        } catch (ClientProtocolException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("postRequest failed");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("postRequest failed");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("unable to convert response content to jsonobject");
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("error occurs when posting request");
        }
    }

    //创建单个任务
    //传入参数是一个.gtask.data.Task包里Task类的对象
    //利用json获取Task里的内容,并且创建相应的jsPost
    //利用postRequest得到任务的返回信息
    //使用task.setGid设置task的new_ID
    public void createTask(Task task) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();

            // action_list
            actionList.put(task.getCreateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            // post
            JSONObject jsResponse = postRequest(jsPost);
            JSONObject jsResult = (JSONObject) jsResponse.getJSONArray(
                    GTaskStringUtils.GTASK_JSON_RESULTS).get(0);
            task.setGid(jsResult.getString(GTaskStringUtils.GTASK_JSON_NEW_ID));

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("create task: handing jsonobject failed");
        }
    }

    //创建一个任务列表，与createTask几乎一样，区别就是最后设置的是tasklist的gid
    public void createTaskList(TaskList tasklist) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();

            // action_list
            actionList.put(tasklist.getCreateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            // post
            JSONObject jsResponse = postRequest(jsPost);
            JSONObject jsResult = (JSONObject) jsResponse.getJSONArray(
                    GTaskStringUtils.GTASK_JSON_RESULTS).get(0);
            tasklist.setGid(jsResult.getString(GTaskStringUtils.GTASK_JSON_NEW_ID));

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("create tasklist: handing jsonobject failed");
        }
    }

    //同步更新操作
    //使用JSONObject进行数据存储，使用jsPost.put，Put的信息包括UpdateArray和ClientVersion
    //使用postRequest发送这个jspost,进行处理
    public void commitUpdate() throws NetworkFailureException {
        if (mUpdateArray != null) {
            try {
                JSONObject jsPost = new JSONObject();

                // action_list
                jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, mUpdateArray);

                // client_version
                jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

                postRequest(jsPost);
                mUpdateArray = null;
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
                throw new ActionFailureException("commit update: handing jsonobject failed");
            }
        }
    }

    //添加更新的事项
    public void addUpdateNode(Node node) throws NetworkFailureException {
        if (node != null) {
            // too many update items may result in an error
            // set max to 10 items
            if (mUpdateArray != null && mUpdateArray.length() > 10) {
                commitUpdate();
            }

            if (mUpdateArray == null)
                mUpdateArray = new JSONArray();
            mUpdateArray.put(node.getUpdateAction(getActionId()));
        }
    }

    //移动task
    //通过getGid获取task所属列表的gid
    //通过JSONObject.put()函数设置移动后的task的相关属性值，从而达到移动的目的
    //通过postRequest进行更新后的发送
    public void moveTask(Task task, TaskList preParent, TaskList curParent)
            throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();
            JSONObject action = new JSONObject();

            // action_list
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_MOVE);
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, getActionId());
            action.put(GTaskStringUtils.GTASK_JSON_ID, task.getGid());
            if (preParent == curParent && task.getPriorSibling() != null) {
                // put prioring_sibing_id only if moving within the tasklist and
                // it is not the first one
                //设置优先级ID，只有当移动是发生在文件中
                action.put(GTaskStringUtils.GTASK_JSON_PRIOR_SIBLING_ID, task.getPriorSibling());
            }
            action.put(GTaskStringUtils.GTASK_JSON_SOURCE_LIST, preParent.getGid());
            action.put(GTaskStringUtils.GTASK_JSON_DEST_PARENT, curParent.getGid());
            if (preParent != curParent) {
                // put the dest_list only if moving between tasklists
                action.put(GTaskStringUtils.GTASK_JSON_DEST_LIST, curParent.getGid());
            }
            actionList.put(action);
            //将ACTION_LIST加入jsPost中
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            postRequest(jsPost);

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("move task: handing jsonobject failed");
        }
    }

    //删除操作结点
    //删除后使用postRequest发送删除后的结果
    public void deleteNode(Node node) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();

            // action_list
            node.setDeleted(true);
            //获取删除操作的ID，加入到actionLiast中
            actionList.put(node.getUpdateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            postRequest(jsPost);
            mUpdateArray = null;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("delete node: handing jsonobject failed");
        }
    }

    //获取任务列表
    //通过GetURI使用getResponseContent从网上获取数据
    //筛选出"_setup("到)}</script>的部分，并且从中获取GTASK_JSON_LISTS的内容返回
    public JSONArray getTaskLists() throws NetworkFailureException {
        if (!mLoggedin) {
            Log.e(TAG, "please login first");
            throw new ActionFailureException("not logged in");
        }

        try {
            HttpGet httpGet = new HttpGet(mGetUrl);
            HttpResponse response = null;
            response = mHttpClient.execute(httpGet);

            // get the task list
            //筛选工作，把筛选出的字符串放入jsString
            String resString = getResponseContent(response.getEntity());
            String jsBegin = "_setup(";
            String jsEnd = ")}</script>";
            int begin = resString.indexOf(jsBegin);
            int end = resString.lastIndexOf(jsEnd);
            String jsString = null;
            if (begin != -1 && end != -1 && begin < end) {
                jsString = resString.substring(begin + jsBegin.length(), end);
            }
            JSONObject js = new JSONObject(jsString);
            //获取GTASK_JSON_LISTS
            return js.getJSONObject("t").getJSONArray(GTaskStringUtils.GTASK_JSON_LISTS);
        } catch (ClientProtocolException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("gettasklists: httpget failed");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("gettasklists: httpget failed");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("get task lists: handing jasonobject failed");
        }
    }

    //通过传入的TASKList的gid,从网络上获取相应属于这个任务列表的任务
    public JSONArray getTaskList(String listGid) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();
            JSONObject action = new JSONObject();

            // action_list
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_GETALL);
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, getActionId());
            action.put(GTaskStringUtils.GTASK_JSON_LIST_ID, listGid);//设置为传入的listGid
            action.put(GTaskStringUtils.GTASK_JSON_GET_DELETED, false);
            actionList.put(action);
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            JSONObject jsResponse = postRequest(jsPost);
            return jsResponse.getJSONArray(GTaskStringUtils.GTASK_JSON_TASKS);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("get task list: handing jsonobject failed");
        }
    }

    public Account getSyncAccount() {
        return mAccount;
    }

    //重置更新的内容
    public void resetUpdateArray() {
        mUpdateArray = null;
    }
}

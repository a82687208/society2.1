package com.gdptc.society.manager;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.gdptc.society.apiServer.ApiParam.DB_HOST;
import static com.gdptc.society.apiServer.ApiParam.TIMEOUT;
import static com.gdptc.society.manager.ApplicationManager.directoryManager;
import static java.lang.Runtime.getRuntime;

/**
 * Created by Administrator on 2017/12/9/009.
 */

public class DBManager {
    private static DBManager dbManager;
    private final String TABLE_IMAGE = "imageInfo";
    private final String TABLE_LOG = "logInfo";

    public enum TABLE {
        ACCOUNT("accountInfo"), SCHOOL("schoolInfo"), PROVINCE("provinceInfo");

        private String value;

        TABLE(String table) {
            value = table;
        }

        public String toName() {
            return value;
        }
    }

    public static final String[] SCHOOL_SIMPLE_RESULT = { DBManager.SCHOOL_ID, DBManager.SCHOOL_LOGO,
            DBManager.SCHOOL_PROVINCE, DBManager.SCHOOL_TYPE, DBManager.SCHOOL_ADDR,
            DBManager.SCHOOL_OLD_NAME, DBManager.SCHOOL_NAME };

    public static final String SCHOOL_ID = "college_id";                             //院校ID
    public static final String SCHOOL_CODE = "code";                                 //学校编码
    public static final String SCHOOL_ADDR = "addr";                                 //通讯地址
    public static final String SCHOOL_INTRODUCTION = "brief_introduction";           //院校简介
    public static final String SCHOOL_NATURE = "college_nature";                     //院校办学性质
    public static final String SCHOOL_EMAIL = "email";                               //邮箱
    public static final String SCHOOL_LEVEL = "level";                               //院校等级
    public static final String SCHOOL_LOGO = "logo";                                 //校徽
    public static final String SCHOOL_SHIP = "member_ship";                          //教育组织
    public static final String SCHOOL_NAME = "name";                                 //院校名称
    public static final String SCHOOL_OFFICIAL = "official_website";                 //院校官网
    public static final String SCHOOL_OLD_NAME = "old_name";                         //院校曾用名
    public static final String SCHOOL_PROPERTY = "property";                         //学校属性
    public static final String SCHOOL_PROVINCE = "province";                         //省份
    public static final String SCHOOL_RANKING = "ranking";                           //院校排名
    public static final String SCHOOL_RANKING_COLLEGETYPE = "ranking_collegetype";   //院校性质排名
    public static final String SCHOOL_RECRUIT_TEL = "recruit_tel";                   //招办电话
    public static final String SCHOOL_RECRUIT = "recruit_website";                   //招办网址
    public static final String SCHOOL_TUITION_FEE = "tuition_fee";                   //学费
    public static final String SCHOOL_TYPE = "type";                                 //院校类别

    public static final String IMAGE_ID = "imgId";
    public static final String IMAGE_BITMAP = "bitmap";
    public static final int IMAGE_ID_LENGTH = 19;

    public static final String PROVINCE_INDEX = "provinceIndex";
    public static final String PROVINCE_NAME = "provinceName";

    public static final String ACCOUNT_PHONE = "phone";
    public static final String ACCOUNT_PSW = "password";
    public static final String ACCOUNT_PIC = "userPic";
    public static final String ACCOUNT_SCHOOL = SCHOOL_ID;
    public static final String ACCOUNT_STU_ID = "stuId";
    public static final String ACCOUNT_NAME = "name";
    public static final String ACCOUNT_SEX = "sex";
    public static final String ACCOUNT_ONLINE = "online";
    public static final String ACCOUNT_ADMIN = "admin";
    public static final String ACCOUNT_SOCIETY_ID = "societyId";

    public static final String SQL_MAIN = "main";

    private final String FORMAT = ".db";
    private SQLiteDatabase localMainDB;
    private Connection networkDB;

    public abstract static class ResultListener {
        public void dbOpen() {}
        public void insert(Object o1, Object o2, int result) {}
        public void delete(Object o1, Object o2, int result) {}
        public void update(Object o1, Object o2, int result) {}
        public void query(Object o1, Object o2, ResultSet resultSet) {}
        public void onFailure(Object o1, Object o2, Exception e) {}
        public void close() {}
        public void onAllTaskDone() {}
    }

    DBManager(Application application) {
        if (dbManager == null) {
            localMainDB = getDB(SQL_MAIN);
            dbManager = this;
            DriverManager.setLoginTimeout((int) TIMEOUT);
        }
    }

    public static DBManager getInstance() {
        return dbManager;
    }

    public void uploadException(String brands, String model, String version, String manufacturer, String exceptionMsg) throws java.sql.SQLException {
        String sql = "insert into " + TABLE_LOG + " (brands, model, version, manufacturer, exception) values(?,?,?,?,?)";
        Log.e(ApplicationManager.class.getSimpleName(), "connection");
        PreparedStatement pStatement = networkDB.prepareStatement(sql);
        pStatement.setString(1, brands);
        pStatement.setString(2, model);
        pStatement.setString(3, version);
        pStatement.setString(4, manufacturer);
        pStatement.setString(5, exceptionMsg);
        Log.e(ApplicationManager.class.getSimpleName(), "exec");
        pStatement.execute();
        Log.e(ApplicationManager.class.getSimpleName(), "close");
        pStatement.close();
    }

    public LocalDBAdapter open() {
        if (localMainDB == null || !localMainDB.isOpen()) {
            localMainDB = getDB(SQL_MAIN);
        }

        return new LocalDBAdapter(localMainDB);
    }

    public NetworkDBAdapter openForNetWork(int processNum, ResultListener resultListener) {
        return new NetworkDBAdapter(processNum, resultListener);
    }

    public NetworkDBAdapter openForNetWork(ResultListener resultListener) {
        int process = getRuntime().availableProcessors();
        return new NetworkDBAdapter(process < 4 ? process : 4, resultListener);
    }

    public NetworkDBAdapter openForNetWork() {
        return new NetworkDBAdapter(getRuntime().availableProcessors());
    }

    public LocalDBAdapter open(String name) {
        if (name.equals(SQL_MAIN))
            return open();
        else
            return new LocalDBAdapter(getDB(directoryManager.PATH_SQL + "/" + name + FORMAT));
    }

    private SQLiteDatabase getDB(String name) {
        return SQLiteDatabase.openOrCreateDatabase(directoryManager.PATH_SQL + "/" + name + FORMAT, null);
    }

    private synchronized void connectionNetWorkDB() throws ClassNotFoundException, java.sql.SQLException {
        if (networkDB == null)
            Class.forName("net.sourceforge.jtds.jdbc.Driver");                  //加载微软SQL服务驱动程序
        if (networkDB == null || networkDB.isClosed())
            networkDB = DriverManager.getConnection("jdbc:jtds:sqlserver://"
                    + DB_HOST + "/society", "ldw", "ldw123456789");             //创建数据连接
    }

    public void close() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (localMainDB != null && localMainDB.isOpen()) {
                    localMainDB.close();
                    localMainDB = null;
                }
                try {
                    if (networkDB != null) {
                        networkDB.close();
                        networkDB = null;
                    }
                }
                catch (java.sql.SQLException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public Timestamp getTime() throws java.sql.SQLException {
        if (networkDB != null) {
            CallableStatement callableStatement = networkDB.prepareCall("? = call getTime()");
            callableStatement.registerOutParameter(1, Types.TIMESTAMP);
            callableStatement.execute();
            return callableStatement.getTimestamp(1);
        }
        return new Timestamp(10000);
    }

    public class LocalDBAdapter {
        private SQLiteDatabase database;

        LocalDBAdapter(SQLiteDatabase database) {
            this(database, null);
        }

        LocalDBAdapter(SQLiteDatabase database, String sql) {
            this.database = database;
            if (sql != null)
                database.execSQL(sql);
        }

        public long insert(TABLE table, ContentValues contentValues) throws SQLException {
            return database.insertOrThrow(table.value, null, contentValues);
        }

        public int update(TABLE table, ContentValues contentValues, String whereClause, String[] whereArgs) {
            return database.update(table.value, contentValues, whereClause, whereArgs);
        }

        public long replace(TABLE table, ContentValues contentValues) {
            return database.replace(table.value, null, contentValues);
        }

        public int delete(TABLE table, String whereClause, String[] whereArgs) {
            return database.delete(table.value, whereClause, whereArgs);
        }

        public Cursor query(TABLE table, String[] columns, String selection, String[] selectionArgs, String groupBy,
                                    String having, String orderBy) {
            return database.query(table.value, columns, selection, selectionArgs, groupBy, having, orderBy);
        }

        public void close() {
            if (!database.equals(localMainDB))
                database.close();
            database = null;
        }

        public boolean isOpen() {
            return database.isOpen();
        }

    }

    public class NetworkDBAdapter {
        private boolean isTryOpen = false;
        private boolean dispathOpen = true;
        private List<ResultListener> resultListenerList;
        private ExecutorService executorService;
        private Map<Object, ExecRunnable> taskList;

        private class ExecRunnable implements Runnable {
            int limit = -1, filterRow = -1;
            String table;
            String tag;
            String select;
            String filterKey;
            String filterOrder;
            String filterSQL;
            String[] args;
            String[] columns;
            String groupBy, having, orderBy;
            ContentValues contentValues;
            StringBuilder columnBuilder;
            StringBuilder valuesBuilder;
            Future future;

            Object o1, o2;

            int n;
            Iterator<Map.Entry<String, Object>> iterator;
            Set<Map.Entry<String, Object>> keys;

            ExecRunnable(Object o1, Object o2) {
                this.o1 = o1;
                this.o2 = o2;
            }

            @Override
            public void run() {
                switch (tag) {
                    case "insert":
                        columnBuilder = new StringBuilder("insert into ");
                        valuesBuilder = new StringBuilder(" values(");
                        columnBuilder.append(table);
                        columnBuilder.append("(");

                        keys = contentValues.valueSet();
                        iterator = keys.iterator();
                        n = 0;

                        while (iterator.hasNext()) {
                            Map.Entry<String, Object> map = iterator.next();
                            if (n != 0) {
                                columnBuilder.append(",");
                                valuesBuilder.append(",");
                            }
                            columnBuilder.append(map.getKey());
                            Object value = map.getValue();

                            if (value instanceof String || value instanceof Character) {
                                valuesBuilder.append("'");
                                valuesBuilder.append(value);
                                valuesBuilder.append("'");
                            }
                            else
                                valuesBuilder.append(value);
                            ++n;
                        }
                        columnBuilder.append(")");
                        valuesBuilder.append(")");

                        try {
                            Statement stm = createStatement();
                            Log.e(DBManager.class.getSimpleName(), columnBuilder.toString() + valuesBuilder.toString());
                            int result = stm.executeUpdate(columnBuilder.toString() + valuesBuilder.toString());

                            try {
                                for (ResultListener resultListener : resultListenerList)
                                    resultListener.insert(o1, o2, result);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }


                            stm.close();
                        }
                        catch (java.sql.SQLException e) {
                            for (ResultListener resultListener : resultListenerList)
                                resultListener.onFailure(o1, o2, e);
                        }
                        break;
                    case "update":
                        columnBuilder = new StringBuilder();
                        columnBuilder.append("update ");
                        columnBuilder.append(table);
                        columnBuilder.append(" set");

                        keys = contentValues.valueSet();
                        iterator = keys.iterator();
                        n = 0;

                        while (iterator.hasNext()) {
                            Map.Entry<String, Object> map = iterator.next();
                            columnBuilder.append(n == 0 ? " " : ", ");
                            columnBuilder.append(map.getKey());
                            columnBuilder.append("=");

                            Object value = map.getValue();
                            if (value instanceof String || value instanceof Character) {
                                columnBuilder.append("'");
                                columnBuilder.append(value);
                                columnBuilder.append("'");
                            }
                            else
                                columnBuilder.append(value);
                            ++n;
                        }

                        try {
                            appendWhereArgs(columnBuilder, select, args);
                            Statement stm = createStatement();
                            Log.e(DBManager.class.getSimpleName(), columnBuilder.toString());
                            int result = stm.executeUpdate(columnBuilder.toString());

                            try {
                                for (ResultListener resultListener : resultListenerList)
                                    resultListener.update(o1, o2, result);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }

                            stm.close();
                        }
                        catch (java.sql.SQLException e) {
                            for (ResultListener resultListener : resultListenerList)
                                resultListener.onFailure(o1, o2, e);
                        }
                        break;
                    case "delete":
                        columnBuilder = new StringBuilder("delete from ");
                        columnBuilder.append(table);
                        try {
                            appendWhereArgs(columnBuilder, select, args);
                            Statement stm = createStatement();
                            Log.e(DBManager.class.getSimpleName(), columnBuilder.toString());
                            int result = stm.executeUpdate(columnBuilder.toString());

                            try {
                                for (ResultListener resultListener : resultListenerList)
                                    resultListener.delete(o1, o2, result);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }

                            stm.close();
                        }
                        catch (java.sql.SQLException e) {
                            for (ResultListener resultListener : resultListenerList)
                                resultListener.onFailure(o1, o2, e);
                        }
                        break;
                    case "query":
                        valuesBuilder = new StringBuilder("select ");
                        if (limit != -1) {
                            valuesBuilder.append("top ");
                            valuesBuilder.append(limit);
                            valuesBuilder.append(" ");
                        }
                        if (columns != null) {
                            for (int i = 0; i < columns.length; ++i) {
                                if (i != 0)
                                    valuesBuilder.append(", ");
                                valuesBuilder.append(columns[i]);
                            }
                        }
                        else
                            valuesBuilder.append("*");
                        valuesBuilder.append(" from ");
                        valuesBuilder.append(table);

                        try {
                            appendWhereArgs(valuesBuilder, select, args);

                            if (filterRow != -1) {
                                valuesBuilder.append(select == null ? " where " : " and ");
                                valuesBuilder.append(filterKey);
                                valuesBuilder.append(" not in(");
                                valuesBuilder.append("select top ");
                                valuesBuilder.append(filterRow);
                                valuesBuilder.append(" ");
                                valuesBuilder.append(filterKey);
                                valuesBuilder.append(" from ");
                                valuesBuilder.append(table);
                                if (filterSQL != null) {
                                    valuesBuilder.append(" where ");
                                    valuesBuilder.append(filterSQL);
                                }
                                if (filterOrder != null) {
                                    valuesBuilder.append(" order by ");
                                    valuesBuilder.append(filterOrder);
                                }
                                valuesBuilder.append(")");
                            }

                            if (groupBy != null) {
                                valuesBuilder.append(" group by ");
                                valuesBuilder.append(groupBy);
                            }

                            if (having != null) {
                                valuesBuilder.append(" having ");
                                valuesBuilder.append(having);
                            }

                            if (orderBy != null) {
                                valuesBuilder.append(" order by ");
                                valuesBuilder.append(orderBy);
                            }

                            Log.e(DBManager.class.getSimpleName(), valuesBuilder.toString());
                            Statement stm = createStatement();
                            ResultSet resultSet = stm.executeQuery(valuesBuilder.toString());

                            try {
                                for (ResultListener resultListener : resultListenerList)
                                    resultListener.query(o1, o2, resultSet);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }

                            resultSet.close();
                            stm.close();
                        }
                        catch (java.sql.SQLException e) {
                            for (ResultListener resultListener : resultListenerList)
                                resultListener.onFailure(o1, o2, e);
                        }
                        break;
                    case "open":
                        try {
                            connectionNetWorkDB();
                            for (ExecRunnable runnable : taskList.values())
                                runnable.future = executorService.submit(runnable);

                            if (dispathOpen) {
                                try {
                                    for (ResultListener resultListener : resultListenerList)
                                        resultListener.dbOpen();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        catch (Exception e) {
                            for (ResultListener resultListener : resultListenerList)
                                resultListener.onFailure(null, null, e);
                        }
                        isTryOpen = false;
                        break;
                    default:
                        break;
                }

                if (o2 != null)
                    taskList.remove(o2);
                if (taskList.size() == 0)
                    try {
                        for (ResultListener resultListener : resultListenerList)
                            resultListener.onAllTaskDone();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
            }
        }

        NetworkDBAdapter(int processNum) {
            this(processNum, null);
        }

        NetworkDBAdapter(int processNum, ResultListener resultListener) {
            resultListenerList = new ArrayList<>();
            if (resultListener != null)
                resultListenerList.add(resultListener);
            executorService = Executors.newFixedThreadPool(processNum);
            taskList = new ConcurrentHashMap<>();
        }

        public void open() {
            if (!isTryOpen) {
                isTryOpen = true;
                ExecRunnable execRunnable = new ExecRunnable(null, null);
                execRunnable.tag = "open";
                executorService.submit(execRunnable);
            }
        }

        public void insert(TABLE table, ContentValues contentValues) {
            insert(table, contentValues, null, null);
        }

        public void insert(TABLE table, ContentValues contentValues, Object o1, Object o2) {
            ExecRunnable runnable = new ExecRunnable(o1, o2);
            runnable.tag = "insert";
            runnable.table = table.value;
            runnable.contentValues = contentValues;
            if (o2 != null)
                taskList.put(o2, runnable);
            if (isOpen())
                runnable.future = executorService.submit(runnable);
            else {
                dispathOpen = false;
                open();
            }
        }

        public void update(TABLE table, ContentValues contentValues, String whereClause, String[] whereArgs) {
            update(table, contentValues, whereClause, whereArgs, null, null);
        }

        public void update(TABLE table, ContentValues contentValues, String whereClause, String[] whereArgs, Object o1, Object o2) {
            ExecRunnable runnable = new ExecRunnable(o1, o2);
            runnable.tag = "update";
            runnable.table = table.value;
            runnable.contentValues = contentValues;
            runnable.select = whereClause;
            runnable.args = whereArgs;
            if (o2 != null)
                taskList.put(o2, runnable);
            if (isOpen())
                runnable.future = executorService.submit(runnable);
            else {
                dispathOpen = false;
                open();
            }
        }

        public void uploadImage(long id, String path) throws Exception {
            File file = new File(path);
            byte[] b = new byte[(int) file.length()];

            FileInputStream fis = new FileInputStream(file);
            fis.read(b);

            uploadImage(id, b);
        }

        public void uploadImage(long id, byte[] bits) throws Exception {
            String sql = "insert into " + TABLE_IMAGE + " values(?,?)";
            PreparedStatement pStatement = networkDB.prepareStatement(sql);
            pStatement.setLong(1, id);
            pStatement.setBytes(2, bits);
            pStatement.execute();

            pStatement.close();
        }

        public void deleteImage(long id) throws java.sql.SQLException {
            String sql = "delete " + TABLE_IMAGE + " where " + IMAGE_ID + "=?";
            PreparedStatement pStatement = networkDB.prepareStatement(sql);
            pStatement.setLong(1, id);
            pStatement.execute();

            pStatement.close();
        }

        public byte[] downloadImage(long id) throws java.sql.SQLException {
            String sql = "select " + IMAGE_BITMAP + " from " + TABLE_IMAGE + " where " + IMAGE_ID + "=" + id;
            Statement statement = createStatement();
            ResultSet resultSet = statement.executeQuery(sql);

            if (!resultSet.wasNull())
                if (resultSet.next())
                    return resultSet.getBytes(1);
            return null;
        }

        public void delete(TABLE table, String whereClause, String[] whereArgs) {
            delete(table, whereClause, whereArgs, null, null);
        }

        public void delete(TABLE table, String whereClause, String[] whereArgs, Object o1, Object o2) {
            ExecRunnable runnable = new ExecRunnable(o1, o2);
            runnable.tag = "delete";
            runnable.table = table.value;
            runnable.select = whereClause;
            runnable.args = whereArgs;
            if (o2 != null)
                taskList.put(o2, runnable);
            if (isOpen())
                runnable.future = executorService.submit(runnable);
            else {
                dispathOpen = false;
                open();
            }
        }

        public void query(TABLE table, String[] columns, String selection, String[] selectionArgs,
                          String groupBy, String having, String orderBy) {
            query(table, columns, selection, selectionArgs, groupBy, having, orderBy, null, null);
        }

        public void query(TABLE table, String[] columns, String selection, String[] selectionArgs,
                          String groupBy, String having, String orderBy, Object o1, Object o2) {
            query(table, columns, selection, selectionArgs, groupBy, having, orderBy, -1, o1, o2);
        }

        public void query(TABLE table, String[] columns, String selection, String[] selectionArgs,
                          String groupBy, String having, String orderBy, int limit) {
            query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, -1, null, null, null, null);
        }

        public void query(TABLE table, String[] columns, String selection, String[] selectionArgs,
                          String groupBy, String having, String orderBy, int limit, Object o1, Object o2) {
            query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, -1, null, null, o1, o2);
        }

        public void query(TABLE table, String[] columns, String selection, String[] selectionArgs,
                          String groupBy, String having, String orderBy, int limit, int filterRow, String filterKey) {
            query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, filterRow, filterKey, null, null, null);
        }

        public void query(TABLE table, String[] columns, String selection, String[] selectionArgs,
                          String groupBy, String having, String orderBy, int limit, int filterRow, String filterKey,
                          String filterOrder, Object o1, Object o2) {
            ExecRunnable runnable = new ExecRunnable(o1, o2);
            runnable.tag = "query";
            runnable.limit = limit;
            runnable.table = table.value;
            runnable.columns = columns;
            runnable.select = selection;
            runnable.args = selectionArgs;
            runnable.groupBy = groupBy;
            runnable.having = having;
            runnable.orderBy = orderBy;
            runnable.filterRow = filterRow;
            runnable.filterKey = filterKey;
            runnable.filterOrder = filterOrder;
            if (o2 != null)
                taskList.put(o2, runnable);
            if (isOpen())
                runnable.future = executorService.submit(runnable);
            else {
                dispathOpen = false;
                open();
            }
        }

        public void query(TABLE table, String[] columns, String selection, String[] selectionArgs,
                          String groupBy, String having, String orderBy, int limit, int filterRow, String filterKey,
                          String filterSQL, String filterOrder, Object o1, Object o2) {
            ExecRunnable runnable = new ExecRunnable(o1, o2);
            runnable.tag = "query";
            runnable.limit = limit;
            runnable.table = table.value;
            runnable.columns = columns;
            runnable.select = selection;
            runnable.args = selectionArgs;
            runnable.groupBy = groupBy;
            runnable.having = having;
            runnable.orderBy = orderBy;
            runnable.filterRow = filterRow;
            runnable.filterKey = filterKey;
            runnable.filterOrder = filterOrder;
            runnable.filterSQL = filterSQL;
            if (o2 != null)
                taskList.put(o2, runnable);
            if (isOpen())
                runnable.future = executorService.submit(runnable);
            else {
                dispathOpen = false;
                open();
            }
        }

        public void addCallBack(ResultListener resultListener) {
            if (resultListener != null && !resultListenerList.contains(resultListener)) {
                resultListenerList.add(resultListener);
                if (isOpen())
                    resultListener.dbOpen();
            }
        }

        public void removeCallBack(ResultListener resultListener) {
            if (resultListener != null)
                resultListenerList.remove(resultListener);
        }

        public boolean containsTask(Object o2) {
            return taskList.containsKey(o2);
        }

        public void cancel(Object o2) {
            if (o2 != null) {
                ExecRunnable runnable = taskList.get(o2);
                if (runnable.future != null)
                    runnable.future.cancel(true);
                taskList.remove(o2);
            }
            else
                cancelAll();
        }

        public void cancelAll() {
            for (ExecRunnable runnable : taskList.values()) {
                if (runnable.future != null)
                    runnable.future.cancel(true);
            }
            taskList.clear();
        }

        public void close() {
            cancelAll();
            executorService.shutdownNow();
            taskList.clear();
        }

        public boolean isOpen() {
            try {
                return networkDB != null && !networkDB.isClosed();
            }
            catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        private void appendWhereArgs(StringBuilder builder, String whereClause, String[] whereArgs) throws java.sql.SQLException {
            builder.append((!TextUtils.isEmpty(whereClause) ? " where " + whereClause : ""));

            int index = 0;

            if (whereArgs != null) {
                for (String value : whereArgs) {
                    index = builder.indexOf("?", index + 1);
                    if (index != -1) {
                        builder.deleteCharAt(index);
                        builder.insert(index, value);
                    }
                    else
                        throw new java.sql.SQLException("Error: whereArgs is too long");
                }
            }
        }

        private Statement createStatement() throws java.sql.SQLException {
            return networkDB.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
        }

    }

}

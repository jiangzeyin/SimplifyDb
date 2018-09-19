package cn.simplifydb.database.run.write;

import cn.simplifydb.database.DbWriteService;
import cn.simplifydb.database.base.BaseWrite;
import cn.simplifydb.database.config.DatabaseContextHolder;
import cn.simplifydb.database.config.SystemColumn;
import cn.simplifydb.database.event.InsertEvent;
import cn.simplifydb.database.util.JdbcUtil;
import cn.simplifydb.database.util.SqlAndParameters;
import cn.simplifydb.database.util.SqlUtil;
import cn.simplifydb.system.DBExecutorService;
import cn.simplifydb.system.DbLog;
import cn.simplifydb.util.DbReflectUtil;
import com.alibaba.druid.util.JdbcUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;

/**
 * 普通insert
 *
 * @author jiangzeyin
 */
public class Insert<T> extends BaseWrite<T> {

    private List<T> list;
    /**
     * 是否批量执行插入操作
     */
    private boolean batch;

    public void setBatch(boolean batch) {
        this.batch = batch;
    }

    public List<T> getList() {
        return list;
    }

    public Insert setList(List<T> list) {
        this.list = list;
        return this;
    }


    /**
     * 获取实体上的监听事件
     *
     * @return 事件接口
     */
    private InsertEvent getEvent(Object data) {
        if (data != null && InsertEvent.class.isAssignableFrom(data.getClass())) {
            return (InsertEvent) data;
        }
        return null;
    }

    public Insert(Connection connection) {
        super(null, connection);
        setThrows(true);
    }

    /**
     * @param data 数据对象
     */
    public Insert(T data) {
        // TODO Auto-generated constructor stub
        super(data, null);
    }

    public Insert(List<T> list) {
        super((T) null, null);
        this.list = list;
    }

    /**
     * 添加数据
     *
     * @param data     对象
     * @param isThrows 发生异常是否抛出
     */
    public Insert(T data, boolean isThrows) {
        super(data, null);
        setThrows(isThrows);
    }

    public Insert(List<T> list, boolean isThrows) {
        super((T) null, null);
        this.list = list;
        setThrows(isThrows);
    }

    /**
     * 异步执行添加数据操作
     *
     * @author jiangzeyin
     */
    @Override
    public void run() {
        if (transactionConnection != null) {
            throw new RuntimeException("Transaction must sync");
        }
        setAsync();
        setThrowable(new Throwable());
        getAsyncLog();
        DBExecutorService.execute(() -> {
            // TODO Auto-generated method stub
            Object id = syncRun();
            if (id == null) {
                DbLog.getInstance().info(getData() + "异步执行失败");
            }
        });
    }


    /**
     * 执行添加数据操作
     *
     * @return 结果id
     * @author jiangzeyin
     */
    public Object syncRun() {
        // TODO Auto-generated method stub
        InsertEvent event = null;
        try {
            Callback callback = getCallback();
            // 单个对象添加
            T data = getData();
            if (data != null) {
                // 加载事件
                event = getEvent(data);
                if (event != null) {
                    Event.BeforeCode beforeCode = event.beforeInsert(this, data);
                    if (beforeCode == Event.BeforeCode.END) {
                        DbLog.getInstance().info("本次执行取消：" + data);
                        return beforeCode.getResultCode();
                    }
                }
                String tag = DbWriteService.getInstance().getDatabaseName(data.getClass());
                SqlAndParameters sqlAndParameters = SqlUtil.getInsertSql(this);
                setRunSql(sqlAndParameters.getSql());
                DbLog.getInstance().info(getTransferLog() + sqlAndParameters.getSql());
                Object key;
                if (transactionConnection == null) {
                    DataSource dataSource = DatabaseContextHolder.getWriteDataSource(tag);
                    key = JdbcUtil.executeInsert(dataSource, sqlAndParameters.getSql(), sqlAndParameters.getParameters());
                } else {
                    key = JdbcUtil.executeInsert(transactionConnection, sqlAndParameters.getSql(), sqlAndParameters.getParameters());
                }
                if (key == null) {
                    key = DbReflectUtil.getFieldValue(data, SystemColumn.getDefaultKeyName());
                } else {
                    DbReflectUtil.setFieldValue(data, SystemColumn.getDefaultKeyName(), key);
                }
                //T data = getData();

                // 实体事件
                if (event != null) {
                    event.completeInsert(key);
                }
                //  util
                if (callback != null) {
                    callback.success(key);
                }
                return key;
            }
            // 添加集合（多个对象）
            if (this.list != null && this.list.size() > 0) {
                if (batch) {
                    return batchRun(callback);
                }
                // 挨个执行
                return itemRun(callback);
            }
            throw new RuntimeException("please add data");
        } catch (Exception e) {
            // TODO: handle exception
            isThrows(e);
            if (event != null) {
                event.errorInsert(e);
            }
        } finally {
            runEnd();
            recycling();
        }
        return null;
    }

    private int batchRun(Callback callback) throws Exception {
        SqlAndParameters[] sqlAndParameters = SqlUtil.getInsertSqls(this);
        String sqlFirst = null;
        String sql;
        int valuesIndex;
        StringBuilder values = new StringBuilder();
        String valuesSql;
        int valueIndex;
        StringBuilder showSql = new StringBuilder();
        for (SqlAndParameters item : sqlAndParameters) {
            sql = item.getSql();
            valuesIndex = sql.indexOf("values");
            if (valuesIndex <= -1) {
                throw new RuntimeException("sql error not find values[" + sql + "]");
            }
            valuesIndex += 6;
            if (sqlFirst == null) {
                sqlFirst = sql.substring(0, valuesIndex);
            }
            valuesSql = sql.substring(valuesIndex);
            valueIndex = 0;
            if (values.length() > 0) {
                values.append(",");
                showSql.append(",");
            }
            for (char ch : valuesSql.toCharArray()) {
                if (ch == '?') {
                    values.append("'").append(item.getParameters().get(valueIndex++)).append("'");
                } else {
                    values.append(ch);
                }
                showSql.append(ch);
            }
        }
        values.insert(0, sqlFirst);
        values.append(";");
        showSql.insert(0, sqlFirst);
        showSql.append(";");
        setRunSql(showSql.toString());
        DbLog.getInstance().info(getTransferLog() + getRunSql());
        int count;
        if (transactionConnection == null) {
            String tag = DbWriteService.getInstance().getDatabaseName(list.get(0).getClass());
            DataSource dataSource = DatabaseContextHolder.getWriteDataSource(tag);
            count = JdbcUtils.executeUpdate(dataSource, values.toString());
        } else {
            count = JdbcUtils.executeUpdate(transactionConnection, values.toString(), Collections.EMPTY_LIST);
        }
        if (callback != null) {
            callback.success(count);
        }
        return count;
    }

    private int itemRun(Callback callback) throws Exception {
        Connection connection = null;
        T data;
        InsertEvent event;
        int successCount = 0;
        try {
            SqlAndParameters[] sqlAndParameters = SqlUtil.getInsertSqls(this);
            setRunSql("more:" + sqlAndParameters[0].getSql());
            for (int i = 0; i < sqlAndParameters.length; i++) {
                data = this.list.get(i);
                if (data == null) {
                    continue;
                }
                if (transactionConnection == null) {
                    String tag = DbWriteService.getInstance().getDatabaseName(data.getClass());
                    connection = DatabaseContextHolder.getWriteConnection(tag);
                } else {
                    connection = transactionConnection;
                }
                event = getEvent(data);
                if (event != null) {
                    Event.BeforeCode beforeCode = event.beforeInsert(this, data);
                    if (beforeCode == InsertEvent.BeforeCode.END) {
                        DbLog.getInstance().info("本次执行取消：" + data + " " + list);
                        continue;
                    }
                }
                DbLog.getInstance().info(sqlAndParameters[i].getSql());
                Object key = JdbcUtil.executeInsert(connection, sqlAndParameters[i].getSql(), sqlAndParameters[i].getParameters());
                if (key == null) {
                    key = DbReflectUtil.getFieldValue(data, SystemColumn.getDefaultKeyName());
                } else {
                    DbReflectUtil.setFieldValue(data, SystemColumn.getDefaultKeyName(), key);
                }
                if (event != null) {
                    event.completeInsert(key);
                }
                if (callback != null) {
                    //   异步回调如果  key是null 则 直接 返回实体
                    if (key == null) {
                        key = data;
                    }
                    callback.success(key);
                }
                successCount++;
            }
        } finally {
            // TODO: handle exception
            if (transactionConnection == null) {
                //  事物连接有事物对象管理
                JdbcUtils.close(connection);
            }
        }
        return successCount;
    }
}

package cn.jiangzeyin.database.run.write;

import cn.jiangzeyin.database.EntityInfo;
import cn.jiangzeyin.database.base.WriteBase;
import cn.jiangzeyin.database.config.DatabaseContextHolder;
import cn.jiangzeyin.database.config.SystemColumn;
import cn.jiangzeyin.database.event.UpdateEvent;
import cn.jiangzeyin.database.util.SqlAndParameters;
import cn.jiangzeyin.database.util.SqlUtil;
import cn.jiangzeyin.system.DbLog;
import cn.jiangzeyin.system.DBExecutorService;
import com.alibaba.druid.util.JdbcUtils;
import com.alibaba.druid.util.StringUtils;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * update 数据库操作
 *
 * @author jiangzeyin
 */
public class Update<T> extends WriteBase<T> {

    private String where;
    private List<Object> whereParameters;
    private Object keyValue;
    private String keyColumn;
    private HashMap<String, Object> update;
    private UpdateEvent event;

    public UpdateEvent getEvent() {
        return event;
    }

    public void setEvent(UpdateEvent event) {
        this.event = event;
    }

    /**
     *
     */
    public Update(T data) {
        super(data);
    }

    public Update(T data, UpdateEvent event) {
        super(data);
        this.event = event;
    }

    public Update(T data, boolean isThrows) {
        super(data);
        setThrows(isThrows);
    }

    public Update() {
        super(null);
    }

    public Update(boolean isThrows) {
        super(null);
        setThrows(isThrows);
    }

    public HashMap<String, Object> getUpdate() {
        return update;
    }

    public void setUpdate(HashMap<String, Object> update) {
        this.update = update;
    }

    /**
     * 添加要更新的字段
     *
     * @param column 列名
     * @param value  值
     * @author jiangzeyin
     */
    public void putUpdate(String column, Object value) {
        // 判断对应字段是否可以被修改
        if (SystemColumn.notCanUpdate(column))
            throw new IllegalArgumentException(column + " not update");
        if (update == null)
            update = new HashMap<>();
        update.put(column, value);
    }

    public Object getKeyValue() {
        return keyValue;
    }

    /**
     * 设置查询主键值
     *
     * @param keyValue 键
     * @author jiangzeyin
     */
    public void setKeyValue(Object keyValue) {
        this.keyValue = keyValue;
    }

    public String getKeyColumn() {
        if (StringUtils.isEmpty(keyColumn))
            return SystemColumn.getDefaultKeyName();
        return keyColumn;
    }

    /**
     * 设置主键列名
     * <p>
     * 默认为 id
     *
     * @param keyColumn 列
     * @author jiangzeyin
     */
    public void setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
    }

    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public void AppendWhere(String where) {
        if (StringUtils.isEmpty(this.where))
            this.where = where;
        else
            this.where += " and " + where;
    }

    public List<Object> getWhereParameters() {
        return whereParameters;
    }

    public void setWhereParameters(List<Object> whereParameters) {
        this.whereParameters = whereParameters;
    }

    public void setWhereParameters(Object... whereParameters) {
        if (this.whereParameters == null)
            this.whereParameters = new LinkedList<>();
        Collections.addAll(this.whereParameters, whereParameters);
    }

    @Override
    public void run() {
        setAsync(true);
        setThrowable(new Throwable());
        getAsyncLog();
        // TODO Auto-generated method stub
        DBExecutorService.execute(this::syncRun);
    }

    /**
     * @return 影响行数
     * @author jiangzeyin
     */
    @Override
    public long syncRun() {
        // TODO Auto-generated method stub
        try {
            T data = getData();
            String tag = data == null ? EntityInfo.getDatabaseName(getTclass()) : EntityInfo.getDatabaseName(data);
            SqlAndParameters sqlAndParameters = SqlUtil.getUpdateSql(this);
            DataSource dataSource = DatabaseContextHolder.getWriteDataSource(tag);
            DbLog.getInstance().info(getTransferLog() + sqlAndParameters.getSql());
            setRunSql(sqlAndParameters.getSql());
            int count = JdbcUtils.executeUpdate(dataSource, sqlAndParameters.getSql(), sqlAndParameters.getParameters());
            if (event != null)
                event.completeU(getKeyValue());
            return count;
        } catch (Exception e) {
            // TODO: handle exception
            isThrows(e);
            if (event != null)
                event.errorU(e);
        } finally {
            runEnd();
            recycling();
            this.update = null;
            this.whereParameters = null;
            this.event = null;
        }
        return 0L;
    }
}

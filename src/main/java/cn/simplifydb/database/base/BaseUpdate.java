package cn.simplifydb.database.base;

import cn.simplifydb.database.annotation.EntityConfig;
import cn.simplifydb.database.config.ModifyUser;
import cn.simplifydb.database.config.SystemColumn;
import cn.simplifydb.database.util.SqlAndParameters;
import cn.simplifydb.database.util.SqlUtil;
import cn.simplifydb.util.DbReflectUtil;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.builder.impl.SQLDeleteBuilderImpl;
import com.alibaba.druid.sql.builder.impl.SQLUpdateBuilderImpl;
import com.alibaba.druid.util.JdbcConstants;

import java.io.Serializable;
import java.sql.Connection;
import java.util.*;

/**
 * 修改相关
 * Created by jiangzeyin on 2018/9/19.
 *
 * @author jiangzeyin
 */
public abstract class BaseUpdate<T> extends BaseWrite<T> implements SQLUpdateAndDeleteBuilder {
    /**
     * 记录要修改的列名和值
     */
    private LinkedHashMap<String, Object> update = new LinkedHashMap<>(20);
    protected SQLUpdateBuilderImpl sqlUpdateBuilder;
    private String ids;
    private SqlAndParameters sqlAndParameters;
    /**
     * 多数据库修改
     */
    private List<MultipleUpdate> multipleUpdates;
    private LinkedList<Object> multipleValue;

    protected BaseUpdate(T data, Connection transactionConnection) {
        super(data, transactionConnection);
        this.createUpdateBuilder();
    }

    private void createUpdateBuilder() {
        this.sqlUpdateBuilder = new SQLUpdateBuilderImpl(JdbcConstants.MYSQL);
    }

    public BaseUpdate<T> setIds(String ids) {
        this.ids = ids;
        return this;
    }

    public BaseUpdate<T> setIds(Object id) {
        return setIds(String.valueOf(id));
    }

    public String getIds() {
        return ids;
    }

    public Map<String, Object> getUpdate() {
        return update;
    }

    /**
     * 添加使用多数据修改
     *
     * @param multipleUpdate 要修改的视图
     * @return this
     */
    public BaseUpdate<T> addMultipleUpdate(MultipleUpdate multipleUpdate) {
        if (this.multipleUpdates == null) {
            this.multipleUpdates = new ArrayList<>();
            this.sqlUpdateBuilder = null;
        }
        this.multipleUpdates.add(multipleUpdate);
        return this;
    }

    public void setMultipleUpdates(List<MultipleUpdate> multipleUpdates) {
        this.multipleUpdates = multipleUpdates;
        if (multipleUpdates == null) {
            this.createUpdateBuilder();
        }
    }

    @Override
    public BaseUpdate<T> setKeyValue(Object keyValue) {
        return setKeyColumnAndValue(SystemColumn.getDefaultKeyName(), keyValue);
    }

    @Override
    public BaseUpdate<T> setKeyColumnAndValue(String column, Object keyValue) {
        saveKeyColumnAndValue(column, keyValue);
        return this;
    }

    /**
     * 设置要修改的map
     *
     * @param update map
     * @return this
     */
    public BaseUpdate<T> setUpdate(Map<String, Object> update) {
        if (update != null) {
            Set<Map.Entry<String, Object>> set = update.entrySet();
            for (Map.Entry<String, Object> entry : set) {
                putUpdate(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    /**
     * 添加要更新的字段
     *
     * @param column 列名
     * @param value  值
     * @author jiangzeyin
     */
    public BaseUpdate<T> putUpdate(String column, Object value) {
        addUpdateColumn(sqlUpdateBuilder, column, value, this.update);
        return this;
    }

    /**
     * 添加修改的列
     *
     * @param sqlUpdateBuilder 修改对象
     * @param column           列
     * @param value            值
     */
    private void addUpdateColumn(SQLUpdateBuilderImpl sqlUpdateBuilder, String column, Object value, LinkedHashMap<String, Object> update) {
        checkUpdate(getTclass(), column);
        String fnVal = getFunctionVal(value);
        if (fnVal != null) {
            sqlUpdateBuilder.set(column + "=" + fnVal);
        } else {
            sqlUpdateBuilder.set(column + "=?");
        }
        update.put(column, value);
    }

    /**
     * 判断是否是sql 执行
     *
     * @param value 值
     * @return null 不是sql 执行
     */
    private String getFunctionVal(Object value) {
        if (!(value instanceof String)) {
            return null;
        }
        String strValue = (String) value;
        if (strValue.startsWith(SystemColumn.SQL_FUNCTION_VAL_PREFIX) && strValue.endsWith(SystemColumn.SQL_FUNCTION_VAL_SUFFIX)) {
            int start = strValue.indexOf(SystemColumn.SQL_FUNCTION_VAL_PREFIX) + SystemColumn.SQL_FUNCTION_VAL_PREFIX.length();
            return strValue.substring(start, strValue.indexOf(SystemColumn.SQL_FUNCTION_VAL_SUFFIX));
        }
        return null;
    }

    @Override
    public List<Object> getParameters() throws Exception {
        List<Object> newList = new LinkedList<>();
        SqlAndParameters sqlAndParameters = getSqlAndParameters();
        if (sqlAndParameters != null) {
            newList.addAll(0, sqlAndParameters.getParameters());
        }
        Collection<Object> collection;
        if (multipleValue != null) {
            // 多数据修改值
            collection = multipleValue;
        } else {
            collection = update.values();
        }
        for (Object object : collection) {
            String strValue = getFunctionVal(object);
            if (strValue == null) {
                //  putUpdate
                newList.add(object);
            }
        }
        // where
        List<Object> parameters = super.getParameters();
        if (parameters != null) {
            newList.addAll(parameters);
        }
        return newList;
    }


    /**
     * sql 记录操作人和时间
     */
    protected void loadModifyUser(SQLUpdateBuilderImpl sqlUpdateBuilder) {
        int optUserId = getOptUserId();
        if (optUserId < 1) {
            return;
        }
        Class cls = getTclass();
        if (ModifyUser.Modify.isModifyClass(cls)) {
            sqlUpdateBuilder.set(ModifyUser.Modify.getColumnUser() + "=" + optUserId);
            sqlUpdateBuilder.set(ModifyUser.Modify.getColumnTime() + "=" + ModifyUser.Modify.getModifyTime());
        }
    }

    /**
     * sql 记录最后修改时间
     *
     * @param sqlUpdateBuilder update
     */
    protected void loadModify(SQLUpdateBuilderImpl sqlUpdateBuilder) {
        Class cls = getTclass();
        EntityConfig entityConfig = (EntityConfig) cls.getAnnotation(EntityConfig.class);
        boolean isLogUpdate = true;
        if (entityConfig != null && !entityConfig.update()) {
            isLogUpdate = false;
        }
        if (isLogUpdate && SystemColumn.Modify.isStatus(cls)) {
            sqlUpdateBuilder.set(SystemColumn.Modify.getColumn() + "=" + SystemColumn.Modify.getTime());
        }
    }

    private SqlAndParameters getSqlAndParameters() throws Exception {
        if (sqlAndParameters == null && data != null) {
            sqlAndParameters = SqlUtil.getWriteSql(this, data);
        }
        return sqlAndParameters;
    }

    @Override
    protected void recycling() {
        super.recycling();
        this.ids = null;
        this.sqlUpdateBuilder = null;
        this.sqlAndParameters = null;
        this.update = null;
        this.multipleValue = null;
        this.multipleUpdates = null;
    }

    @Override
    protected void securityCheck(Object object, String keyColumn, Object keyValue) {
        if (ids != null) {
            if (object instanceof SQLUpdateBuilderImpl) {
                SQLUpdateBuilderImpl sqlUpdateBuilder = (SQLUpdateBuilderImpl) object;
                sqlUpdateBuilder.whereAnd(SystemColumn.getDefaultKeyName() + " in(" + ids + ")");
            } else if (object instanceof SQLDeleteBuilderImpl) {
                SQLDeleteBuilderImpl sqlDeleteBuilder = (SQLDeleteBuilderImpl) object;
                sqlDeleteBuilder.whereAnd(SystemColumn.getDefaultKeyName() + " in(" + ids + ")");
            }
        }
        super.securityCheck(object, keyColumn, keyValue);
    }

    @Override
    public String builder() throws Exception {
        String sql;
        if (multipleUpdates == null || multipleUpdates.size() <= 0) {
            SqlAndParameters sqlAndParameters = getSqlAndParameters();
            if (sqlAndParameters != null) {
                List<String> columns = sqlAndParameters.getColumns();
                if (columns != null) {
                    HashMap<String, String> map = sqlAndParameters.getSystemMap();
                    for (String item : columns) {
                        Object val = null;
                        if (map != null) {
                            val = map.get(item);
                        }
                        if (val == null) {
                            sqlUpdateBuilder.set(item + "=?");
                        } else {
                            sqlUpdateBuilder.setValue(item, val);
                        }
                    }
                }
                Object objId = DbReflectUtil.getFieldValue(data, SystemColumn.getDefaultKeyName());
                if (objId != null) {
                    sqlUpdateBuilder.whereAnd(SystemColumn.getDefaultKeyName() + "='" + objId.toString() + "'");
                }
            }
            loadModify(this.sqlUpdateBuilder);
            loadModifyUser(this.sqlUpdateBuilder);
            addTable(sqlUpdateBuilder);
            //
            securityCheck(sqlUpdateBuilder, this.keyColumn, this.keyValue);
            sql = sqlUpdateBuilder.toString();
        } else {
            StringBuffer allSql = new StringBuffer();
            multipleUpdates.forEach(multipleUpdate -> {
                Map<String, Object> map = multipleUpdate.getUpdateMap();
                if (map == null || map.size() <= 0) {
                    return;
                }
                SQLUpdateBuilderImpl sqlUpdateBuilder = multipleUpdate.getSqlUpdateBuilder();
                // 修改的列
                Set<Map.Entry<String, Object>> entries = map.entrySet();
                LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>(20);
                entries.forEach(stringObjectEntry -> {
                    checkUpdate(getTclass(), stringObjectEntry.getKey());
                    addUpdateColumn(sqlUpdateBuilder, stringObjectEntry.getKey(), stringObjectEntry.getValue(), linkedHashMap);
                });
                if (multipleValue == null) {
                    multipleValue = new LinkedList<>();
                }
                multipleValue.addAll(linkedHashMap.values());
                //
                loadModify(sqlUpdateBuilder);
                loadModifyUser(sqlUpdateBuilder);
                addTable(sqlUpdateBuilder);
                securityCheck(sqlUpdateBuilder, multipleUpdate.getKeyColumn(), multipleUpdate.getKeyValue());
                //
                allSql.append(sqlUpdateBuilder.toString()).append(";");
            });
            sql = allSql.toString();
        }
        setRunSql(sql);
        return sql;
    }

    /**
     * 添加表名
     *
     * @param sqlUpdateBuilder update 对象
     */
    private void addTable(SQLUpdateBuilderImpl sqlUpdateBuilder) {
        SQLUpdateStatement sqlUpdateStatement = sqlUpdateBuilder.getSQLUpdateStatement();
        if (sqlUpdateStatement == null || sqlUpdateStatement.getFrom() == null) {
            String tableName = SqlUtil.getTableName(this, getTclass());
            sqlUpdateBuilder.from(tableName);
        }
    }

    @Override
    public SQLUpdateAndDeleteBuilder from(String table) {
        sqlUpdateBuilder.from(table);
        return this;
    }

    @Override
    public SQLUpdateAndDeleteBuilder from(String table, String alias) {
        sqlUpdateBuilder.from(table, alias);
        return this;
    }

    @Override
    public SQLUpdateAndDeleteBuilder limit(int rowCount) {
        sqlUpdateBuilder.limit(rowCount);
        return this;
    }

    @Override
    public SQLUpdateAndDeleteBuilder limit(int rowCount, int offset) {
        sqlUpdateBuilder.limit(rowCount, offset);
        return this;
    }

    @Override
    public SQLUpdateAndDeleteBuilder where(String sql) {
        sqlUpdateBuilder.where(sql);
        return this;
    }

    @Override
    public SQLUpdateAndDeleteBuilder whereAnd(String sql) {
        sqlUpdateBuilder.whereAnd(sql);
        return this;
    }

    @Override
    public SQLUpdateAndDeleteBuilder whereOr(String sql) {
        sqlUpdateBuilder.whereOr(sql);
        return this;
    }

    @Override
    public SQLUpdateAndDeleteBuilder set(String... items) {
        if (data == null) {
            sqlUpdateBuilder.set(items);
        } else {
            throw new IllegalArgumentException("update entity not set");
        }
        return this;
    }

    @Override
    public String toString() {
        String updateSql;
        try {
            updateSql = sqlUpdateBuilder.toString();
        } catch (Exception e) {
            updateSql = "";
        }
        return super.toString() + "BaseUpdate{" +
                "update=" + update +
                ", sqlUpdateBuilder=" + updateSql +
                ", ids='" + ids + '\'' +
                ", sqlAndParameters=" + sqlAndParameters +
                ", multipleUpdates=" + multipleUpdates +
                ", multipleValue=" + multipleValue +
                '}';
    }

    /**
     * 多数据修改视图
     */
    public static class MultipleUpdate implements Serializable {
        private String keyColumn;
        private Object keyValue;
        private Map<String, Object> updateMap;
        private SQLUpdateBuilderImpl sqlUpdateBuilder;

        public MultipleUpdate() {
            this.sqlUpdateBuilder = new SQLUpdateBuilderImpl(JdbcConstants.MYSQL);
        }

        public SQLUpdateBuilderImpl getSqlUpdateBuilder() {
            return sqlUpdateBuilder;
        }

        public String getKeyColumn() {
            return keyColumn;
        }

        public MultipleUpdate setKeyColumn(String keyColumn) {
            this.keyColumn = keyColumn;
            return this;
        }

        public Object getKeyValue() {
            return keyValue;
        }

        public MultipleUpdate setKeyValue(Object keyValue) {
            this.keyValue = keyValue;
            return this;
        }

        public MultipleUpdate setKeyColumnAndValue(String column, Object keyValue) {
            this.keyColumn = column;
            this.keyValue = keyValue;
            return this;
        }

        public Map<String, Object> getUpdateMap() {
            return updateMap;
        }

        public MultipleUpdate setUpdateMap(Map<String, Object> updateMap) {
            this.updateMap = updateMap;
            return this;
        }

        @Override
        public String toString() {
            String updateSql;
            try {
                updateSql = sqlUpdateBuilder.toString();
            } catch (Exception e) {
                updateSql = "";
            }
            return "MultipleUpdate{" +
                    "keyColumn='" + keyColumn + '\'' +
                    ", keyValue=" + keyValue +
                    ", updateMap=" + updateMap +
                    ", sqlUpdateBuilder=" + updateSql +
                    '}';
        }
    }
}

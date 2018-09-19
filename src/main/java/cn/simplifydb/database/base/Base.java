package cn.simplifydb.database.base;


import cn.jiangzeyin.StringUtil;
import cn.simplifydb.database.config.DataSourceConfig;
import cn.simplifydb.database.config.DatabaseContextHolder;
import cn.simplifydb.database.config.SystemColumn;
import cn.simplifydb.system.DbLog;
import cn.simplifydb.util.DbReflectUtil;
import com.alibaba.druid.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * 数据库操作公共
 *
 * @author jiangzeyin
 */
public abstract class Base<T> {
    /**
     * 异常是否抛出
     */
    private boolean isThrows;
    /**
     * 外键表
     */
    private HashMap<String, Class<?>> refMap;
    private HashMap<String, String> refWhere;
    /**
     * 外键列名
     */
    private String refKey;
    /**
     * 排除不操作字段
     */
    private List<String> remove;
    /**
     * 数据库标示
     */
    private String tag;
    /**
     * 数据库对应class
     */
    private Class<?> tclass;
    private long runTime;
    private String runSql;
    private String tempTransferLog;
    /**
     * 操作的对应tag
     */
    private String tagName;

    /**
     * 是否使用数据库名
     */
    private boolean useDataBaseName;


    public boolean isUseDataBaseName() {
        return useDataBaseName;
    }

    public void setUseDataBaseName(boolean useDataBaseName) {
        this.useDataBaseName = useDataBaseName;
    }



    private String getTagName() {
        if (tagName == null) {
            return DatabaseContextHolder.getConnectionTagName();
        }
        return tagName;
    }

    private void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public HashMap<String, String> getRefWhere() {
        return refWhere;
    }


    /**
     * 创建时就获取操作人
     */
    Base() {
        // TODO Auto-generated constructor stub
        runTime = System.currentTimeMillis();
    }

    protected void setRunSql(String runSql) {
        if (!StringUtil.isEmpty(runSql)) {
            runSql = runSql.replaceAll("[\r\n]", " ");
        }
        this.runSql = runSql;
    }

    /**
     * 获取运行的sql
     *
     * @return sql
     */
    public String getRunSql() {
        return runSql;
    }

    protected void runEnd() {
        if (DataSourceConfig.SQL_TIMEOUT <= 0) {
            return;
        }
        long time = System.currentTimeMillis() - runTime;
        if (time > DataSourceConfig.SQL_TIMEOUT) {
            String tagName = getTagName();
            DbLog.getInstance().warn(tagName + "执行时间过长：" + time + "  " + runSql);
        }
    }


    /**
     * 返回操作的泛型类
     *
     * @return class
     */
    public Class<?> getTclass() {
        return getTclass(true);
    }

    /**
     * 获取tclass
     *
     * @param getRef true 获取泛型
     * @return 返回当前操作的泛型
     */
    public Class<?> getTclass(boolean getRef) {
        if (tclass == null && getRef) {
            tclass = DbReflectUtil.getTClass(getClass());
        }
        return tclass;
    }

    public void setTclass(Class<?> tclass) {
        this.tclass = tclass;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<String> getRemove() {
        return remove;
    }

    /**
     * 设置排除获取字段
     *
     * @param remove 要移除的字段
     * @author jiangzeyin
     */
    public void setRemove(String... remove) {
        if (remove == null) {
            return;
        }
//        List<String> remove = this.remove;
        //getRemove();
        if (this.remove == null) {
            this.remove = new LinkedList<>();
        }
        for (String item : remove) {
            if (!this.remove.contains(item)) {
                this.remove.add(item.toLowerCase());
            }
        }
    }

    public String getRefKey() {
        if (StringUtils.isEmpty(refKey)) {
            return SystemColumn.getDefaultRefKeyName();
        }
        return refKey;
    }

    /**
     * 设置外键的列
     *
     * @param refKey 外键的列
     * @author jiangzeyin
     */
    public void setRefKey(String refKey) {
        this.refKey = refKey;
    }

    public HashMap<String, Class<?>> getRefMap() {
        return refMap;
    }


    /**
     * 添加外键 关系
     *
     * @param name     外键名称
     * @param refClass 外键类
     * @author jiangzeyin
     */
    public void putRefClass(String name, Class<?> refClass) {
        if (refMap == null) {
            refMap = new HashMap<>();
        }
        refMap.put(name.toLowerCase(), refClass);
    }

    public void putRefClass(String name, Class<?> refClass, String where) {
        if (refMap == null) {
            refMap = new HashMap<>();
        }
        if (refWhere == null) {
            refWhere = new HashMap<>();
        }
        refMap.put(name, refClass);
        refWhere.put(name, where);
    }

    public boolean isThrows() {
        return isThrows;
    }

    public void setThrows(boolean isThrows) {
        this.isThrows = isThrows;
    }

    /**
     * 处理异常
     *
     * @param t 异常信息
     * @author jiangzeyin
     */
    public void isThrows(Throwable t) {
        if (isThrows) {
            throw new RuntimeException(t);
        }
        DbLog.getInstance().error("执行数据库操作", t);
    }

    /**
     * 回收对象信息
     *
     * @author jiangzeyin
     */
    protected void recycling() {
        refMap = null;
        refKey = null;
        refWhere = null;
        remove = null;
        tag = null;
        tclass = null;
        runSql = null;
        runTime = 0L;
        tagName = null;
        // tag 标记
        DatabaseContextHolder.recycling();
    }

    protected String getTransferLog() {
        if (tempTransferLog != null) {
            return tempTransferLog;
        }
        return DataSourceConfig.isActive() ? "" : getLine();
    }

    private String getLine() {
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[4];
        return String.format("[%s-%s-%s]", StringUtil.simplifyClassName(stackTraceElement.getClassName()), stackTraceElement.getMethodName(), stackTraceElement.getLineNumber());
    }

    protected void getAsyncLog() {
        tempTransferLog = getLine();
        setTagName(DatabaseContextHolder.getConnectionTagName());
    }
}

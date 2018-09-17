package cn.simplifydb.database.base;


import cn.simplifydb.database.config.SystemColumn;
import cn.simplifydb.system.DbLog;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Set;

/**
 * 写入数据
 *
 * @author jiangzeyin
 */
@SuppressWarnings("unchecked")
public abstract class WriteBase<T> extends Base<T> {
    private Callback callback;
    private T data;
    private Throwable throwable;
    private boolean isAsync;
    /**
     * 事务的链接信息
     */
    protected Connection transactionConnection;

    /**
     * 设置回调事件监听
     *
     * @param callback 事件
     * @return WriteBase
     */
    public WriteBase setCallback(Callback callback) {
        this.callback = callback;
        return this;
    }

    public Callback getCallback() {
        return callback;
    }

    private Throwable getThrowable() {
        return throwable;
    }

    protected void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    private boolean isAsync() {
        return isAsync;
    }

    protected void setAsync() {
        this.isAsync = true;
    }

    protected WriteBase(Connection transactionConnection) {
        this.transactionConnection = transactionConnection;
    }

    protected WriteBase() {

    }

    /**
     * 异步执行
     *
     * @author jiangzeyin
     */
    public abstract void run();

    /**
     * @param data 对应实体
     */
    public WriteBase(T data) {
        // TODO Auto-generated constructor stub
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public WriteBase setData(T data) {
        this.data = data;
        return this;
    }

    /**
     * @param t 异常
     * @author jiangzeyin
     */
    @Override
    public void isThrows(Throwable t) {
        // TODO Auto-generated method stub
        if (isAsync()) {
            t.addSuppressed(getThrowable());
            if (isThrows()) {
                throw new RuntimeException(t);
            } else {
                DbLog.getInstance().error("执行数据库操作", t);
            }
        } else {
            super.isThrows(t);
        }
    }

    /**
     * @author jiangzeyin
     */
    @Override
    protected void recycling() {
        // TODO Auto-generated method stub
        super.recycling();
        data = null;
        throwable = null;
        transactionConnection = null;
    }

    public interface Event {

        /**
         * 操作前
         */
        enum BeforeCode {
            CONTINUE("继续", 0),
            END("结束", -100);

            BeforeCode(String desc, int resultCode) {
                this.desc = desc;
                this.resultCode = resultCode;
            }

            private String desc;
            private int resultCode;

            public int getResultCode() {
                return resultCode;
            }

            public String getDesc() {
                return desc;
            }
        }
    }

    /**
     * 效验update 是否合法
     *
     * @param cls    cls
     * @param update map
     */
    protected static void checkUpdate(Class cls, HashMap<String, Object> update) {
        if (update != null) {
            Set<String> set = update.keySet();
            for (String item : set) {
                if (SystemColumn.notCanUpdate(item))
                    throw new IllegalArgumentException(item + " not update");
                if (SystemColumn.isSequence(cls, item))
                    throw new IllegalArgumentException(item + " not update sequence");
            }
        }
    }

    /**
     * 事件回调
     */
    public interface Callback {
        /**
         * success
         *
         * @param key 主键
         */
        void success(Object key);
    }

}
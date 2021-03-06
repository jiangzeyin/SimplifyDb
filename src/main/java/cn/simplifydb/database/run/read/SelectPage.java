package cn.simplifydb.database.run.read;

import cn.simplifydb.database.base.BaseRead;
import cn.simplifydb.database.config.DatabaseContextHolder;
import cn.simplifydb.database.util.JdbcUtil;
import cn.simplifydb.database.util.SqlUtil;
import cn.simplifydb.util.Util;
import cn.simplifydb.system.DbLog;
import com.alibaba.druid.sql.PagerUtils;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.druid.util.JdbcUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 分页查询
 *
 * @author jiangzeyin
 */
public class SelectPage<T> extends BaseRead<T> {

    /**
     * 页码，默认是第一页
     */
    private int pageNo = 1;
    /**
     * 每页显示的记录数，默认是15
     */
    private int pageSize = 5;
    /**
     * 总记录数
     */
    private int totalRecord;
    /**
     * 总页数
     */
    private int totalPage;

    public int getPageNo() {
        return pageNo;
    }

    public SelectPage<T> setPageNo(int pageNo) {
        if (pageNo <= 0) {
            throw new IllegalArgumentException(String.valueOf(pageNo));
        }
        this.pageNo = pageNo;
        return this;
    }

    public int getPageSize() {
        return pageSize;
    }

    public SelectPage<T> setPageSize(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException(String.valueOf(pageSize));
        }
        this.pageSize = pageSize;
        return this;
    }

    public int getTotalRecord() {
        return totalRecord;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public SelectPage() {

    }

    /**
     * 固定类型
     *
     * @param result result
     */
    public SelectPage(Result result) {
        setResultType(result);
    }

    /**
     * 分页基本使用
     *
     * @param pageNo   页数
     * @param pageSize 每页条数
     */
    public SelectPage(int pageNo, int pageSize) {
        // TODO Auto-generated constructor stub
        setPageNoAndSize(pageNo, pageSize);
    }

    /**
     * 设置页码和每页个数
     *
     * @param pageNo   页码
     * @param pageSize 每页个数
     * @return this
     */
    public SelectPage setPageNoAndSize(int pageNo, int pageSize) {
        if (pageNo <= 0 || pageSize <= 0) {
            throw new IllegalArgumentException(pageNo + "  " + pageSize);
        }
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        return this;
    }

    /**
     * 设置查看起始信息
     *
     * @param start  开始的下标
     * @param length 长度
     * @return this
     */
    public SelectPage setDisplayPage(int start, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("  " + length);
        }
        int pageNo = 1;
        if (start >= length) {
            pageNo += start / length;
        }
        this.setPageNo(pageNo);
        this.setPageSize(length);
        return this;
    }

    private void setTotalRecord(long totalRecord) {
        this.totalRecord = (int) totalRecord;
        // 在设置总页数的时候计算出对应的总页数，在下面的三目运算中加法拥有更高的优先级，所以最后可以不加括号。
        long totalPage = totalRecord % pageSize == 0 ? totalRecord / pageSize : totalRecord / pageSize + 1;
        //        this.setTotalPage((int) totalPage);
        this.totalPage = (int) totalPage;
    }

    public int getOffset() {
        return (getPageNo() - 1) * getPageSize();
    }

    /**
     * @return 结果
     * @author jiangzeyin
     */
    @SuppressWarnings({"hiding", "unchecked"})
    @Override
    public <t> t run() {
        // TODO Auto-generated method stub
        String countSql = null;
        try {
            String tag = getTag();
            String sql = builder();
            countSql = PagerUtils.count(sql, JdbcConstants.MYSQL);
            DataSource dataSource = DatabaseContextHolder.getReadDataSource(tag);
            List<Map<String, Object>> list;
            long count = 0;
            {
                // 查询数据总数
                list = JdbcUtils.executeQuery(dataSource, countSql, getParameters());
                if (!Util.checkListMapNull(list)) {
                    Map<String, Object> countMap = list.get(0);
                    Collection collection = countMap.values();
                    count = (Long) collection.toArray()[0];
                    setTotalRecord(count);
                }
            }
            DbLog.getInstance().info(getTransferLog(4) + getRunSql());
            if (count > 0) {
                // 查询数据
                countSql = null;
                sql = PagerUtils.limit(sql, JdbcConstants.MYSQL, getOffset(), getPageSize());
                list = JdbcUtils.executeQuery(dataSource, sql, getParameters());
                // 判断是否开启还原
                if (isUnescapeHtml()) {
                    JdbcUtil.htmlUnescape(list);
                }
            } else {
                list = new ArrayList<>();
            }
            {
                if (getResultType() == Result.JsonArray) {
                    return (t) JSON.toJSON(list);
                }
                // 结果是分页数据
                if (getResultType() == Result.PageResultType) {
                    JSONObject data = new JSONObject();
                    data.put("results", list);
                    data.put("pageNo", pageNo);
                    data.put("pageSize", pageSize);
                    data.put("totalPage", totalPage);
                    data.put("totalRecord", totalRecord);
                    return (t) data;
                }
                List<?> resultList = SqlUtil.convertList(this, list);
                return (t) resultList;
            }
        } catch (Exception e) {
            // TODO: handle exception
            if (countSql != null) {
                DbLog.getInstance().info(getTransferLog(4) + countSql);
            }
            isThrows(e);
        } finally {
            runEnd();
            recycling();
        }
        return null;
    }

    @Override
    public String toString() {
        return super.toString() + "SelectPage{" +
                "pageNo=" + pageNo +
                ", pageSize=" + pageSize +
                ", totalRecord=" + totalRecord +
                ", totalPage=" + totalPage +
                '}';
    }
}

package cn.simplifydb.test;

import cn.simplifydb.Init;
import cn.simplifydb.database.base.BaseRead;
import cn.simplifydb.database.run.read.Select;
import com.alibaba.fastjson.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Created by jiangzeyin on 2018/11/26.
 */
public class TestSelectHtml {
    @Before
    public void init() throws Exception {
        Init.init();
    }

    /**
     * 默认读取配置
     */
    @Test
    public void selectEntity() {
        Select<cn.simplifydb.entity.test.Test> testSelect = new Select<>();
        testSelect.setTclass(cn.simplifydb.entity.test.Test.class);
        List<cn.simplifydb.entity.test.Test> testList = testSelect.run();
        System.out.println(testList);
    }

    /**
     * 指定配置
     */
    @Test
    public void selectEntity2() {
        Select<cn.simplifydb.entity.test.Test> testSelect = new Select<>();
        testSelect.setTclass(cn.simplifydb.entity.test.Test.class);
        testSelect.setUnescapeHtml(false);
        List<cn.simplifydb.entity.test.Test> testList = testSelect.run();
        System.out.println(testList);
    }

    @Test
    public void selectEntity3() {
        Select<cn.simplifydb.entity.test.Test> testSelect = new Select<>();
        testSelect.setTclass(cn.simplifydb.entity.test.Test.class);
//        testSelect.setUnescapeHtml(false);
        testSelect.setResultType(BaseRead.Result.JsonObject);
        JSONObject object = testSelect.run();
        System.out.println(object.getJSONObject("name"));
        System.out.println(object);
    }
}

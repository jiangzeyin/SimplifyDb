# 版本日志

## 2.0.14

* 事务操作支持获取Select【cn.simplifydb.database.run.write.Transaction.Operate.getSelect】
* 添加默认事务实现类【cn.simplifydb.database.run.TransactionCallBack】
* 事务支持显示获取【cn.simplifydb.database.run.write.Transaction.create(java.lang.Class)】
* update 支持同时修改多同表不同条件的值 【cn.simplifydb.database.base.BaseUpdate.MultipleUpdate】
-------------------------------------------------------------------------------------------------------------


## 2.0.13

* 事务异常添加原异常
* 操作对象的泛型Class 都使用T 或者任意 
-------------------------------------------------------------------------------------------------------------


## 2.0.12

* 事务获取操作对象添加泛型
* SystemSessionInfo 取消获取用户名方法
* update 事件回调方法添加影响行数参数 

-------------------------------------------------------------------------------------------------------------

## 2.0.11

* 事务操作对象添加传class方法
* 修护获取行号问题

-------------------------------------------------------------------------------------------------------------

## 2.0.10

* 修正事物类命名 [cn.simplifydb.database.TransactionException]
* 新增可配置自动全局还原html 实体 cn.simplifydb.database.config.ConfigProperties.UNESCAPE_HTML

-------------------------------------------------------------------------------------------------------------

## 2.0.9

* 事件消息不自动回收

-------------------------------------------------------------------------------------------------------------

## 2.0.6-7-8

*  生成模式不打印取消执行日志
*  RemoveEvent 事件
*  操作Event 事件可以传出失败原因
*  操作对象添加toString 方法

-------------------------------------------------------------------------------------------------------------

## 2.0.5

*  update 和 remove 防止整表操作
*  SelectFunction 错误

-------------------------------------------------------------------------------------------------------------


## 2.0.4

*  insert 批量执行List 只有一条数据时异常
*  update sql 执行参数异常
*  update sql 执行的标识前缀于后缀支持自定义 cn.simplifydb.database.config.SystemColumn.SQL_FUNCTION_VAL_PREFIX

-------------------------------------------------------------------------------------------------------------

## 2.0.3

*  putUpdate 多个时，参数顺序错误
*  insert 批量执行系统字段拼接错误

-------------------------------------------------------------------------------------------------------------


## 2.0.2

*  limit 重写，防止limit 不在最后
*  keyValue 允许value 为null
*  update 部分字段，如果实体class实现了更新接口，自动创建一个对象去调用接口

-------------------------------------------------------------------------------------------------------------


## 2.0.1

*  日常版本更新

-------------------------------------------------------------------------------------------------------------


## 2.0.0

*  全面优化sql 生成器，使用druid builder生成

-------------------------------------------------------------------------------------------------------------


package org.god.ibatis.core;

import java.sql.Connection;

/**
 * @program: MyBatis
 * @description: 事务管理器接口
 * @author: Zhang Nan
 * @create: 2022-10-29 09:48
 **/
public interface Transaction {
    /**
     * 提交事务
     */
    void commit();

    /**
     * 回滚事务
     */
    void rollback();

    /**
     * 关闭事务，释放资源
     */
    void close();

    void openConnection();

    Connection getConnection();
}

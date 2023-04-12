package org.god.ibatis.core;

import java.sql.Connection;

/**
 * @program: MyBatis
 * @description: 实现Transaction接口，对应事务管理器中的MANAGED（该类不实现了）
 * @author: Zhang Nan
 * @create: 2022-10-29 12:00
 **/
public class ManagedTransaction implements Transaction{
    @Override
    public void commit() {

    }

    @Override
    public void rollback() {

    }

    @Override
    public void close() {

    }

    @Override
    public void openConnection() {

    }

    @Override
    public Connection getConnection() {
        return null;
    }
}

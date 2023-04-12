package org.god.ibatis.core;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @program: MyBatis
 * @description: 实现的Trasaction的接口，对应事务管理器的JDBC
 * @author: Zhang Nan
 * @create: 2022-10-29 11:58
 **/
public class JdbcTransaction implements Transaction{

    //数据源类型接口: UNPOOLED、POOLED、JDNI
    private DataSource dataSource;
    //自动提交标识
    private boolean autoCommit;

    private Connection connection;

    @Override
    public Connection getConnection() {
        return connection;
    }

    /**
     * 这里提供有参构造，需要传入数据源、是否自动提交事务，创建事务管理器对象
     * @param dataSource
     * @param autoCommit
     */
    public JdbcTransaction(DataSource dataSource, boolean autoCommit) {
        this.dataSource = dataSource;
        this.autoCommit = autoCommit;
    }

    @Override
    public void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void openConnection(){
        if (connection == null) {
            try {
                connection = dataSource.getConnection();
                connection.setAutoCommit(autoCommit);     //这里很重要，需要加这一行代码，开启事务
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

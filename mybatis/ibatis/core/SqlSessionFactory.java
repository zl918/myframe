package org.god.ibatis.core;

import org.god.ibatis.pojo.GodMappedStatement;

import java.util.Map;

/**
 * @program: MyBatis
 * @description: 工厂类，用于创建sqlSession会话对象，并且一个数据库大多只有一个工厂类
 * @author: Zhang Nan
 * @create: 2022-10-29 09:58
 **/
public class SqlSessionFactory {
    /**
     * 事务管理器属性
     */
    private Transaction transaction;

    /**
     * Mapper属性，将映射的Mapper.xml信息封装到这里
     * 使用Map集合的方式存储
     */
    private Map<String, GodMappedStatement> MappedStatement;


    //下面是给这个类提供有参构造、无参构造，setter and getter方法

    public SqlSessionFactory() {
    }

    public SqlSessionFactory(Transaction transaction, Map<String, GodMappedStatement> mappedStatement) {
        this.transaction = transaction;
        MappedStatement = mappedStatement;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Map<String, GodMappedStatement> getMappedStatement() {
        return MappedStatement;
    }

    public void setMappedStatement(Map<String, GodMappedStatement> mappedStatement) {
        MappedStatement = mappedStatement;
    }

    /**
     * 创建sqlSession会话对象，利用这个对象执行sql语句
     * @return sqlSession对象
     */
    public SqlSession openSession(){
        //需要开启连接
        transaction.openConnection();

        //这里很巧，传入this相当于将sqlSessionFactory对象作为参数，sqlSession需要执行sql语句，就需要事务，transaction、MappedStatement
        SqlSession sqlSession = new SqlSession(this);
        return sqlSession;
    }

}

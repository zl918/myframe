package org.god.ibatis.pojo;

/**
 * @program: MyBatis
 * @description: 一个封装这Mapper.xml信息的普通Java类
 * @author: Zhang Nan
 * @create: 2022-10-29 11:29
 **/
public class GodMappedStatement {

    private String resultType;
    private String sql;

    public GodMappedStatement() {
    }

    public GodMappedStatement(String resultType, String sql) {
        this.resultType = resultType;
        this.sql = sql;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    @Override
    public String toString() {
        return "GodMappedStatement{" +
                "resultType='" + resultType + '\'' +
                ", sql='" + sql + '\'' +
                '}';
    }
}

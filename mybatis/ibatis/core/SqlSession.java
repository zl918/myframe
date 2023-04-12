package org.god.ibatis.core;


import java.lang.reflect.Method;
import java.sql.*;

/**
 * @program: MyBatis
 * @description: 这是MyBatis会话类，由SqlSessionFactory的openSession创建
 * @author: Zhang Nan
 * @create: 2022-11-02 13:11
 **/
public class SqlSession {

    private SqlSessionFactory sqlSessionFactory;

    public SqlSession(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * 提交方法
     */
    public void commit(){
        try {
            sqlSessionFactory.getTransaction().getConnection().commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 回滚方法
     */
    public void rollback(){
        try {
            sqlSessionFactory.getTransaction().getConnection().rollback();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭方法
     */
    public void close(){
        try {
            sqlSessionFactory.getTransaction().getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * insert方法
     * 观察使用阔以看出需要两个参数，一个sqlId,一个封装了sql数据的普通类
     */
    public int insert(String sqlId,Object pojo){
        int count = 0;
        try {
            //首先获取连接
            Connection connection = sqlSessionFactory.getTransaction().getConnection();
            //获取sql语句，这是原始的mapper映射文件里的sql  insert into t_user(id,name,age) values (#{id},#{name},#{age})
            String mappersql = sqlSessionFactory.getMappedStatement().get(sqlId).getSql();
            //转化成正常JDBC需要的sql语句
            String sql = mappersql.replaceAll("#\\{[0-9A-Za-z_$]*}","?");
            PreparedStatement ps = connection.prepareStatement(sql);
            //给问号传值
            int fromIndex = 0;
            int index= 1;
            while (true){
                int startIndex = mappersql.indexOf("#",fromIndex);
                if (startIndex < 0) {
                    break;
                }
                int endIndex = mappersql.indexOf("}",fromIndex);
                String name = mappersql.substring(startIndex + 2, endIndex).trim();  //去除{}中的前后空白
                fromIndex = endIndex + 1;
                //有id，如何获取getId()方法
                String getMothod = "get" + name.toUpperCase().charAt(0) + name.substring(1);
                Method getMothed = pojo.getClass().getDeclaredMethod(getMothod);
                Object retValue = getMothed.invoke(pojo);
                ps.setString(index,retValue.toString());
                index ++;
            }
            count = ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * 查询一条记录的方法
     * @return 返回一个对象，一个使用resultType类型的对象
     */
    public Object selectOne(String sqlId,Object data){
        Object obj = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        //查询的样子： Object result = sqlSession.selectOne("selectByCarId", 6);  可以看出需要一个sqlId,一个数据(这个数据不知道什么类型的)

        try {
            //通过sqlId获取映射文件中的sql语句
            String mapperSql = sqlSessionFactory.getMappedStatement().get(sqlId).getSql();
            //将得到的原始sql转化成JDBC的sql语句
            String sql = mapperSql.replaceAll("#\\{[0-9A-Za-z_$]*}","?");
            Connection connection = sqlSessionFactory.getTransaction().getConnection();
            ps = connection.prepareStatement(sql);
            //下面就是往占位符中填值
            ps.setString(1,data.toString());
            //返回查询结果集
            rs = ps.executeQuery();
            //封装结果类型
            String resultType = sqlSessionFactory.getMappedStatement().get(sqlId).getResultType();    //resultType这个值是: com.zn.pojo.Car
            if (rs.next()) {
                Class<?> resultTypeClass = Class.forName(resultType);    //这个是获取返回的结果类型的类
                obj = resultTypeClass.newInstance();       //这条语句实际就是 Object obj = new User();
                //给obj的哪条属性附上哪条值？？
/*                mysql> select * from t_user where id = "12";
                +----+-----------+-----+
                | id | name      | age |
                +----+-----------+-----+
                | 12 | 张娜娜     | 23  |
                +----+-----------+-----+
                根据上面的查询结果：
                    可以将查询结果集的字段名作为插入的属性名
                */
                ResultSetMetaData rsmd = rs.getMetaData();     //rsmd就包含了查询结果集的字段名
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    String columnName = rsmd.getColumnName(i + 1);
                    //获取封装类的set方法名
                    String setMethodName = "set" + columnName.toUpperCase().charAt(0) + columnName.substring(1);    //通过字段名拼接出属性的set方法名
                    Method setName = resultTypeClass.getDeclaredMethod(setMethodName,String.class);      //获取方法
                    //使用该方法给属性赋值
                    setName.invoke(obj,rs.getString(columnName));
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return obj;
    }

}


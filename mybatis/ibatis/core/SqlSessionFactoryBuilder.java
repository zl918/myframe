package org.god.ibatis.core;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.god.ibatis.pojo.Const;
import org.god.ibatis.pojo.GodMappedStatement;
import org.god.ibatis.utils.Resources;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: MyBatis
 * @description: 构建类，用于构建工厂类sqlSessionFactory
 * @author: Zhang Nan
 * @create: 2022-10-29 09:48
 **/
public class SqlSessionFactoryBuilder {

    /**
     * 提供构造方法，通过分析MyBatis使用程序可以看出，这个类是new出来的
     */
    public SqlSessionFactoryBuilder() {
    }

    /**
     * 该实例方法用于创建sqlSessionFactory工厂类
     * @param inputStream  指向核心配置文件
     * @return  返回一个工厂类
     */
    public SqlSessionFactory build(InputStream inputStream) {
        SqlSessionFactory factory = null;
        try {
            //解析配置文件
            SAXReader reader = new SAXReader();
            Document document = null;     //这里不需要使用工具类将inputStream转化成流，因为Mybatis使用的时候它传进来的就需要是一个流
            document = reader.read(inputStream);

            //获取environments的标签和标签所带的属性值
            Element environments = (Element) document.selectSingleNode("/configuration/environments");
            String defaultId = environments.attributeValue("default");

            //进而获取environment、dataSource标签
            Element environment = (Element) document.selectSingleNode("/configuration/environments/environment[@id='" + defaultId + "']");
            Element transactionElt = environment.element("transactionManager");
            Element dataSourceElt = environment.element("dataSource");

            //创建事务管理器对象，需要创建数据源对象
            DataSource dataSource = getDataSource(dataSourceElt);
            //解析核心配置文件，创建事务管理器对象，而创建事务管理器对象又需要dataSource对象(在JdbcTransaction可以看出来)
            Transaction transaction = getTransaction(transactionElt,dataSource);

            //获取Mapper的resource属性信息
            List<Node> nodes = document.selectNodes("//mapper");    //这里使用selectNodes方法，获取所有的mapper属性信息，注意:这里必须使用“//”是获取整个配置文件的。而单斜杠是从根路径下获取
            //为什么需要获取多个？因为有多个Mapper文件，需要定义一个List集合存放mapper中的resource信息
            List<String> mapperXMLPathList = new ArrayList<>();
            nodes.forEach(node -> {
                Element mapper = (Element) node;  //这里需要强转为Element类型，有更多的方法
                String resource = mapper.attributeValue("resource");
                mapperXMLPathList.add(resource);
            });

            //解析核心配置文件，获取SQL映射对象
            Map<String, GodMappedStatement > mappedStatement = getMappedStatement(mapperXMLPathList);
            factory = new SqlSessionFactory(transaction,mappedStatement);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return factory;
    }

    private Map<String, GodMappedStatement> getMappedStatement(List<String> mapperXMLPathList) {
        //定义一个Map集合，key装namespace文件定位信息，value装sql和resultType信息
        Map<String,GodMappedStatement> mappedStatementMap = new HashMap<>();
        mapperXMLPathList.forEach( mapperXMLPath ->{
            try {
                SAXReader reader = new SAXReader();
                Document document = reader.read(Resources.getResourceAsReader(mapperXMLPath));   //获取了一个mapper.xml配置文件,这里使用了我们封装的方法
                Element mapper = (Element) document.selectSingleNode("mapper");
                //获取mapper的namespace
                String namespace = mapper.attributeValue("namespace");
                //获取该节点下的所有sql节点
                List<Element> elements = mapper.elements();
                elements.forEach(element -> {
                    String id = element.attributeValue("id");
                    //新的id出来了
                    String sqlId = namespace + "." + id;
                    String resultType = element.attributeValue("resultType");
                    String sql = element.getTextTrim();
                    GodMappedStatement godMappedStatement = new GodMappedStatement(resultType,sql);
                    mappedStatementMap.put(sqlId,godMappedStatement);
                });
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        });
        return mappedStatementMap;
    }

    private Transaction getTransaction(Element transactionElt,DataSource dataSource) {
        //在外面定义一个transaction对象
        Transaction transaction = null;

        String type = transactionElt.attributeValue("type");
        //这里的事务管理器类型有两个：JDBC、MANAGED,需要在常量类中定义这两个常量字符串
        if (Const.JDBC_TRANSACTION.equals(type)) {
            transaction = new JdbcTransaction(dataSource,false);  //这里默认是开启事务
        }
        if (Const.MANAGED_TRANSACTION.equals(type)) {
            transaction = new ManagedTransaction();
        }
        return transaction;
    }

    private DataSource getDataSource(Element dataSourceElt) {
        //在这里定义一个DataSource为空，下面赋值
        DataSource dataSource = null;

        //创建一个Map集合用于装property信息
        Map<String,String> map = new HashMap<>();
        //获取UnpooledDataSource类创建需要的值
        List<Element> propertyElts = dataSourceElt.elements("property");
        propertyElts.forEach(propertyElt -> {
            String name = propertyElt.attributeValue("name");
            String value = propertyElt.attributeValue("value");
            map.put(name,value);
        });
        //获取数据源属性type:有三个，UNPOOLED,POOLED,JNDI
        String type = dataSourceElt.attributeValue("type").trim().toUpperCase();  //.trim().toUpperCase()去除前后空白，转大写
        //判断是哪个属性的值，来创建DataSource
        //"UNPOOLED".equls(type)，一般不采用这种方式，一般在外面定义一个常量类来代替这个字符串值
        if (Const.UNPOOLED_DATASOURCE.equals(type)) {
            dataSource = new UnpooledDataSource(map.get("driver"),map.get("url"),map.get("username"),map.get("password"));  //只实现了这个UNPOOLED，需要传值
        }
        if (Const.POOLED_DATASOURCE.equals(type)) {
            dataSource = new PooledDataSource();
        }
        if (Const.JNDI_DATASOURCE.equals(type)) {
            dataSource = new JndiDataSource();
        }
        return dataSource;
    }
}

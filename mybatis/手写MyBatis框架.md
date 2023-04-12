# 手写MyBatis框架

## ①构建模块，引入依赖，创建基本类

- 创建maven模块，名为godbatis,再向pom.xml中引入依赖
  - **dom4j、jaxen**：用于解析xml文件，MyBatis就是一堆Java类解析xml文件完成数据库操作
  - **junit**：测试类（可要可不要），在框架编写中需要测试
  - **Mysql**：mysql的驱动，MyBatis就是高度封装的JDBC，也是需要Mysql驱动的

- 根据MyBatis的使用可以创建几个基本类

  ```java
  SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();
  SqlSessionFactory sqlSessionFactory = sqlSessionFactoryBuilder.build(Resources.getResourceAsReader("mybatis-config.xml"));
  sqlSession = sqlSessionFactory.openSession();
  ```

  - 首先，创建工具类，用于将核心配置文件转化成输入流，创建工具类Resources,并且提供方法getResourceAsReader

    ```java
    package org.god.ibatis.utils;
    import java.io.InputStream;
    
    // 这个工具类，通过分析程序，是从类的根路径下获取文件
    public class Resources {
    	
    	//构造方法私有化，防止new对象，可以直接调用，这是一种规范写法
        private Resources(){}
    
        public static InputStream getResourceAsReader(String xmlDocumrent){
            return ClassLoader.getSystemClassLoader().getResourceAsStream(xmlDocumrent);
        }
    }
    ```

  - 创建sqlSessionFactoryBuilder类，构建类用于构建工厂类，提供build方法获取输入流

    ```java
    package org.god.ibatis.core;
    import java.io.InputStream;
    
    public class sqlSessionFactoryBuilder {
    
    	//提供构造方法，通过分析MyBatis使用程序可以看出，这个类是new出来的
        public sqlSessionFactoryBuilder() {
        }
    
        public SqlSessionFactory build(InputStream in){
            sqlSessionFactory factory = new sqlSessionFactory();     //这里只要调用build方法就返回一个sqlSessionFactory对象
            return factory
        }
    }
    ```

  - 创建工厂类，SqlSessionFactory，用于创建sqlSession会话对象，并且一个数据库一个SqlSessionFactory类，它可以创建多个会话对象

    ```java
    package org.god.ibatis.core;
    
    public class SqlSessionFactory {
        
    }
    ```

## ②分析sqlSessionFacetory中有哪些属性

- 通过一个(转化输入流)工具类，将配置文件传入sqlSessionFactoryBuilder类的build方法中，创建的sqlSessionFactory工厂类，所以需要分析核心配置文件中有哪些东西，决定sqlSessionFactory中有哪些属性

  ```xml
  <configuration>
      <environments default="development">
          <environment id="development">
              <transactionManager type="JDBC"/>
              <dataSource type="POOLED">
                  <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                  <property name="url" value="jdbc:mysql://localhost:3306/mybatis"/>
                  <property name="username" value="root"/>
                  <property name="password" value="421127"/>
              </dataSource>
          </environment>
      </environments>
  
      <mappers>
          <mapper resource="sqlMapper.xml"/>
      </mappers>
  </configuration>
  ```

  通过分析，需要至少三个属性：**事务管理器属性(transactionManager)、数据源属性(dataSource)、mapper映射属性**

  **为什么sqlSessionFactory要有这些属性，因为需要通过sqlSessionFactory这个类来解析核心配置文件，解析出核心配置文件的数据库信息来创建数据库会话对象sqlSession，再通过这个sqlSession对象来完成数据库操作**

- 其中前两个属性好装载，在第三个属性中它还绑定了一个sql映射文件，映射文件中包含了需要进行的sql操作

  sqlMapper.xml

  ```xml
  <mapper namespace="car">
      <insert id="insertCar">
          insert into t_car(id,car_num,brand,guide_price,produce_time,car_type)
          values (null,#{carNum},#{brand},#{guideprice},#{producetime},#{cartype})
      </insert>
  
      <select id="selectByCarId" resultType="com.zn.pojo.Car">
          select
          id,car_num as carNum,brand,guide_price as guidePrice,produce_time as produceTime,car_type as carType
          from t_car where id=#{id}
      </select>
  </mapper>
  ```

  - 而如何封装这些信息是个问题，在<mappers>中可能不止一个<mapper>，所以我们需要一个Map集合来封装这些信息再将这个Map集合作为一个属性传给sqlSessionFactory，这里用到了**面向对象编程思想**

    ![](C:\Users\user\Desktop\picture\封装的Mapper信息原理.png)

  - 首先，在sqlSessionFactory中定义一个属性,其他两个属性再分析

    ```java
    public class sqlSessionFactory {
        /**
         * 事务管理器属性
         */
    
        /**
         * 数据源属性
         */
    
        /**
         * Mapper属性，将映射的Mapper.xml信息封装到这里
         * 使用Map集合的方式存储
         */
        private Map<String, GodMappedStatement> MappedStatement;
    }
    ```

  - 再创建一个不同的pojo类用于存储Mapper.xml中的信息，类名GodMappedStatement,这是一个普通的Java类，封装了resultType属性(用于存储返回值类型)、sql属性(用于存放sql语句)；提供有参、无参构造，setting and getting方法、toString方法

    ```java
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
    ```


## ③分析sqlSessionFactory中的事务管理器属性

- **首先**，我们知道，在Mybatis的事务管理器中有两个可选项：**JDBC、MANAGED**

  所以我们需要在sqlSessionFactory中灵活定义事务管理器属性，所以需要创建一个事务管理器接口，在定义两个对应的事务管理器类型的类去实现接口，面向接口编程，可以直接在sqlSessionFactory中放一个事务管理器的接口属性，在使用的时候再灵活的实现该接口就行

  - 定义接口**Transaction.java**

    ```java
    public interface Transaction {
    	// 提交事务
        void commit();
    
    	// 回滚事务
        void rollback();
    
    	// 关闭事务，释放资源
        void close();
        
    }
    ```

    写两个实现类，供后面实现准确的事务类型做准备：JdbcTransaction.java、ManagedTransaction.java

    **JdbcTransaction.java**

    ```java
    public class JdbcTransaction implements Transaction{
        @Override
        public void commit() {
    		
        }
    
        @Override
        public void rollback() {
    
        }
    
        @Override
        public void close() {
    
        }
    }
    ```

    **ManagedTransaction.java**（后期该类不做实现）

    ```java
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
    }
    
    ```

    在sqlSessionFactory.java中定义接口属性

    ```
    public class sqlSessionFactory {
        /**
         * 事务管理器属性
         */
        private Transaction transaction;      //定义的接口属性，到时候核心配置文件中选择哪个实现类型，就动态new哪个实现类 
    
        /**
         * 数据源属性
         */
    
        /**
         * Mapper属性，将映射的Mapper.xml信息封装到这里
         * 使用Map集合的方式存储
         */
        private Map<String, GodMappedStatement> MappedStatement;
    }
    ```

- **然后**，我们需要完成实现的**JdbcTransaction.java**类中的方法

  ```java
  public class JdbcTransaction implements Transaction{
      
      /**
       *数据源属性
      */
      
      @Override
      public void commit() {
  		//Connection.commit();
      }
  
      @Override
      public void rollback() {
  		//Connection.commit();
      }
  
      @Override
      public void close() {
  		//Connection.commit();
      }
  }
  ```

  - **而需要实现上面的方法就需要获取连接对象Connection，而该对象又是通过数据源来获取的，所以干脆将数据源属性定义到事务管理器类型的实现类中，后期工厂类需要数据源，直接从事务管理器的实现类中获取数据源属性**

    **sqlSessionFactory.java**

    ```java
    public class sqlSessionFactory {
        /**
         * 事务管理器属性
         */
        private Transaction transaction;
    
        /**
         * Mapper属性，将映射的Mapper.xml信息封装到这里
         * 使用Map集合的方式存储
         */
        private Map<String, GodMappedStatement> MappedStatement;
    }
    ```

- **然后**，我们分析数据源属性，根据配置文件，它的数据源属性有三个类型：UNPOOLED、POOLED、JNDI

  而这三个属性又同时满足一个条件，都实现了JDK的规范：javax.sql.DataSource

  所以可以在**JdbcTransaction.java**提供一个数据源属性接口DataSource，在分别定义三个实现类，来实现三个不同的类别

  - UnpooledDataSource.java

    ```java
    public class UnpooledDataSource implements javax.sql.DataSource{
    
        private String driver;
        private String url;
        private String username;
        private String password;
    
    
        public UnpooledDataSource(String driver, String url, String username, String password) {
            try {
                //注册驱动
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            this.url = url;
            this.username = username;
            this.password = password;
        }
    
        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url,username,password);
        }
      	//其他的方法不需要实现
    }
    ```

  - 其他两个实现类（PooledDataSource.java、JndiDataSource.java）不实现

- **最后**，对JdbcTransaction.java的三个方法进行实现

  - 首先，这三个方法：commit、rollback、close需要连接对象来实现，所以在外面定义一个连接对象connection属性，这个连接对象属性的值可以通过DataSource的getConnection来获取

    **在类中提供一个有参构造方法，需要传入数据源和是否自动提交事务**

  - 其次，有个难题，如何实现这三个方法的connection都一样，定义一个方法openConnection来给connection属性赋值

    **关键**：**需要判断当connection是Null的时候，在给属性赋值**，而connection是JdbcTransaction.java中的一个属性，就是不变的，这样可以保证三个方法都是同一个connection。

  - 最后，通过connection属性去是实现三个方法，记得在接口中添加一个openConnection方法接口

    **注意**：需要提供一个连接对象的get方法，这是事务管理器的connection，后面还需要执行sql语句，这个connection需要保持一直，所以提供一个get方法，直接从事务管理器的实现类中取，就都是一样的(记得在接口中定义一个方法 Connection getConnection();)

    ```java
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
    	
        public Connection getConnection() {
            return connection;
        }
        /**
         * 这里提供有参构造，需要传入数据源、是否自动提交事务
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
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    ```

## ④编写sqlSessionFactorybuilder的build方法

- **首先**，给sqlSessionFactroy.java提供有参无参构造方法，在给setter and getter 方法

  ```java
  public class sqlSessionFactory {
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
  	//有参和无参构造方法
      public sqlSessionFactory() {
      }
  
      public sqlSessionFactory(Transaction transaction, Map<String, GodMappedStatement> mappedStatement) {
          this.transaction = transaction;
          MappedStatement = mappedStatement;
      }
  	
  	//提供setter and getter方法
      public Transaction getTransaction() {return transaction;}
  	
      public void setTransaction(Transaction transaction) {this.transaction = transaction;}
  
      public Map<String, GodMappedStatement> getMappedStatement() {return MappedStatement;}
  
      public void setMappedStatement(Map<String, GodMappedStatement> mappedStatement) {MappedStatement = mappedStatement;}
  }
  ```

- **然后**，编写sqlSessionFactorybuilder.java中的build方法，开始new出sqlSessionFactory对象,

  下面需要创造sqlSessionFactory对象(**sqlSessionFactory factory = new sqlSessionFactory(transaction,mappedStatement);**)需要有事务管理器对象和mappedStatement对象两个属性

  获取事务管理器对象，可以将这个对象new出来，我们选择在外面定义这个方法来获取

  **Transaction transaction = getTransaction(transactionElt,dataSource);**，这里需要dataSource对象，可以在JdbcTransaction.java中看出来

  **为什么获取Transaction需要dataSource呢？为什么在sqlSessionFactory.java后面将数据源属性放入了事务管理器属性里了呢？**

  ```text
  这里有个整体的思维：
  	这里所有类都是用来解析核心配置文件，最后都是给sqlSessionFactiry类来服务的，所有的工作都是将配置文件中的数据封装成对象放入
  	sqlSessionFactiry对象中
  	
  	而我们通过核心配置文件可以知道，sqlSessionFactiry需要三个属性：①事务管理器属性、②数据源属性、③Mapper属性
  	而我们的事务管理器属性中至少又需要包含三个方法：①commit、②rollback、③close,即提交事务、回滚事务、关闭资源
  	这三个方法又需要Connection对象(数据库连接对象)来调用，而这个对象又需要数据源dataSource来创造,所以在Transaction的实现类中定义了数据源
  ```

  而获取事务管理器对象还需要数据源对象，就需要**DataSource dataSource = getDataSource(dataSourceElt);**在外面定义这个方法，在外面去实现它

  ```java
  public class sqlSessionFactoryBuilder {
      /**
       * 提供构造方法，通过分析MyBatis使用程序可以看出，这个类是new出来的
       */
      public sqlSessionFactoryBuilder() {
      }
      /**
       * 该实例方法用于创建sqlSessionFactory工厂类
       * @param inputStream  指向核心配置文件
       * @return  返回一个工厂类
       */
      
      
      public sqlSessionFactory build(InputStream inputStream) throws DocumentException {
          //解析配置文件
          SAXReader reader = new SAXReader();
          Document document = reader.read(inputStream);
  
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
  
  
  
          //解析核心配置文件，获取SQL映射对象
          Map<String, GodMappedStatement > mappedStatement = null;
          sqlSessionFactory factory = new sqlSessionFactory(transaction,mappedStatement);
          return factory;
      }
  
      private Transaction getTransaction(Element transactionElt,DataSource dataSource) {
          return null;
      }
  }
  ```

- 实现**getDataSource(dataSourceElt)**方法

  ```java
      private DataSource getDataSource(Element dataSourceElt) {
          //创建一个Map集合用于装property信息
          Map<String,String> map = new HashMap<>();
          //获取UnpooledDataSource类创建需要的值
          List<Element> propertyElts = dataSourceElt.elements("property");
          propertyElts.forEach(propertyElt -> {
              String name = propertyElt.attributeValue("name");
              String value = propertyElt.attributeValue("value");
              map.put(name,value);
          });
  
          //在这里定义一个DataSource为空，下面赋值
          DataSource dataSource = null;
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
  ```

- 实现**getTransaction(transactionElt,dataSource)**方法

  ```java
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
  ```

- **最后**，还差一个**mappedStatement**参数就可以完成build方法，就可以完成sqlSessionFactory对象的创建

  ```java
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
  ```

  **mappedStatement**参数是个集合，它的key需要mapper的定位(前面讲到)，value是封装的返回的结果集的类型和sql语句

  首先，需要获取核心配置文件里的mapper的所有文件位置，他是一个集合，建立一个List集合去装它，再写一个方法用于解析mapper的sql映射的配置文件**getMappedStatement(mapperXMLPathList)**

  - **getMappedStatement(mapperXMLPathList)**的实现

    ```java
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
    ```

- 写完,build方法就结束了，就可以通过它来完成对sqlSessionFactory对象的创建

## ⑤创建SqlSession类，用于执行sql语句

- **首先**，这个类通过用MyBatis阔以看出，是利用SqlSessionFactory的openSession方法来获取的，所以，再sqlSessionFactory中编写代码

  这里的this很妙，需要利用SqlSession对象，来完成事务和sql语句的执行，所以需要将transaction和MappedStatement传到sqlSession中，就直接利用this关键字将SqlSessionFactory对象传进去

  ```java
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
  ```

- **然后**，创建SqlSession类

  ```java
  public class SqlSession {
  
      private SqlSessionFactory sqlSessionFactory;
  
      public SqlSession(SqlSessionFactory sqlSessionFactory) {
          this.sqlSessionFactory = sqlSessionFactory;
      }
  }
  ```

- 接着，在SqlSession中编写方法，insert、selectOne、commit、rollback、close等方法

  


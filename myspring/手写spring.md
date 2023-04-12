## 先从使用者的角度出发

- **建立一个User类、UserDao类、UserService类**

  - ```java
    public class com.zn.annotation.bean.User {
        private String name;
        private int age;
    
        public void setName(String name) {
            this.name = name;
        }
    
        public void setAge(int age) {
            this.age = age;
        }
    
        @Override
        public String toString() {
            return "com.zn.annotation.bean.User{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }
    ```

  - ```java
    public class UserDao {
    
        public void insert(){
            System.out.println("UserDao的insert方法执行");
        }
    }
    ```

  - ```java
    public class UserService {
    
        private UserDao userDao;
    
        public void setUserDao(UserDao userDao) {
            this.userDao = userDao;
        }
    
        public void save(){
            userDao.insert();
        }
    }
    ```

- **从使用者的角度创建一个myspring.xml**

  - ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <beans>
        <bean id="com.zn.annotation.bean.User" class="com.zn.spring6.bean.com.zn.annotation.bean.User">
            <property name="name" value="张三"/>
            <property name="age" value="20"/>
        </bean>
    
        <bean id="UserDao" class="com.zn.spring.bean.UserDao"/>
    
        <bean id="UserService" class="com.zn.spring.bean.UserService">
            <property name="userDao" ref="UserDao"/>
        </bean>
    </beans>
    ```

- **建立一个junit单元测试来测试这个程序**

  - ```java
    public void testMySpring(){
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("myspring.xml");
        Object user = applicationContext.getBean("com.zn.annotation.bean.User");
        System.out.println(user);
    
        UserService userService = (UserService) applicationContext.getBean("UserService");
        userService.save();
    }
    ```



- 仔细分析上面的测试程序和xml文件，可以知道，框架的构建：

  - 第一步，需要一个ApplicationContext接口提供一个getBean的方法，再需要一个ClassPathXmlApplicationContext的实体类来实现它，这里的ClassApplicationContext需要提供一个有参的构造方法，在这个有参构造中需要解析xml文件，初始化bean对象，将对象创建出来，最重要

    的就是这个方法。

    这里前面需要了解一个东西，就是spring在存储对象的时候是有三级缓存概念

    **什么是spring的三级缓存？**

    第一级缓存：也叫单例池，存放已经经历了完整生命周期的Bean对象。

    第二级缓存：存放早期暴露出来的Bean对象，实例化以后，就把对象放到这个Map中。（Bean可能只经过实例化，属性还未填充）。

    第三级缓存：存放早期暴露的Bean的工厂。

    **注：**

    只有单例的bean会通过三级缓存提前暴露来解决循环依赖的问题，而非单例的bean，每次从容器中获取都是一个新的对象，都会重新创建，所以非单例的bean是没有缓存的，不会将其放到三级缓存中。

    为了解决第二级缓存中AOP生成新对象的问题，Spring就提前AOP，比如在加载b的流程中，如果发送了循环依赖，b依赖了a，就要对a执行AOP，提前获取增强以后的a对象，这样b对象依赖的a对象就是增强以后的a了。

    二三级缓存就是为了解决循环依赖，且之所以是二三级缓存而不是二级缓存，主要是可以解决循环依赖对象需要提前被aop代理，以及如果没有循环依赖，早期的bean也不会真正暴露，不用提前执行代理过程，也不用重复执行代理过程。

    **对象在三级缓存中的创建流程**

    A依赖B，B依赖A

    1、A创建过程中需要B，于是先将A放到三级缓存，去实例化B。

    2、B实例化的过程中发现需要A，于是B先查一级缓存寻找A，如果没有，再查二级缓存，如果还没有，再查三级缓存，找到了A，然后把三级缓存里面的这个A放到二级缓存里面，并删除三级缓存里面的A。

    3、B顺利初始化完毕，将自己放到一级缓存里面（此时B里面的A依然是创建中的状态）。然后回来接着创建A，此时B已经创建结束，可以直接从一级缓存里面拿到B，去完成A的创建，并将A放到一级缓存。

    

    所以解析的xml文件中的每个bean都是一个Bean对象,  这个bean包括**id**和**class**,这样正好就可以将解析出来的对象储存到一个大Map集合中，而Map的key存储着**id**,value存储着这个**bean对象**，在getBean方法中传入一个String字符串参数，在通过这个Map的get方法通过id,获取这个Bean对象

  - 最重要的一点是写ClassPathXmlApplitionContext的构造方法，在构造方法中需要解析xml文件，将xml的信息通过反射机制，制造对象，获取set方法等等。

- 接口的撰写

  ```java
  /**
   * myspring的应用上下文接口，有getBean方法，获取Bean对象
   */
  public interface ApplicationContext {
  
      Object getBean(String name);
  }
  ```

- 实现类的撰写

  ```java
  /**
   * @program: Spring6
   * @description: 实现ApplicationContext接口，这个类用于解析xml文件，初始化Bean对象
   * @author: Zhang Nan
   * @create: 2022-11-11 15:44
   **/
  public class ClassPathXmlApplicationContext implements ApplicationContext{
  
      private static final Logger logger = LoggerFactory.getLogger(ClassPathXmlApplicationContext.class);
  
      /**
       * 提供一个属性，用于存储从xml文件中解析出来的Bean对象，这个属性是一个Map集合
       */
      private Map<String,Object> singletonObjects = new HashMap<>();
  
      @Override
      public Object getBean(String name) {
          return singletonObjects.get(name);      //在这里调用getBean，是从缓存中拿取bean对象
      }
  
      /**
       * 在这个构造方法中，解析xml文件，初始化Bean
       * @param configLocation 类路径下的配置文件
       */
      public ClassPathXmlApplicationContext(String configLocation) {
  
          try {
              //获取dom4j核心对象
              SAXReader reader = new SAXReader();
              //获取流，从类路径下加载，转化成流文件
              InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(configLocation);
              //读文件，传入流，获取一个document，这个document就是那个xml文件
              Document document = reader.read(in);
              List<Node> nodes = document.selectNodes("//bean");
  
              //第一次遍历，第一次曝光
              nodes.forEach(node -> {
                  try {
                      Element element = (Element) node;
                      //获取每个bean中的id、class
                      String id = element.attributeValue("id");
                      String className = element.attributeValue("class");
  
                      logger.info("id："+id);
                      logger.info("className："+className);
                      //下面通过反射机制创建对象，并且存入Map集合进行曝光
                      //获取类
                      Class<?> aClass = Class.forName(className);
                      //获取类中无参数构造方法
                      Constructor<?> constructor = aClass.getDeclaredConstructor();
                      //调用无参数构造方法实例化bean
                      Object bean = constructor.newInstance();
                      //第一次曝光，存入Map集合
                      singletonObjects.put(id,bean);
                      logger.info(singletonObjects.toString());
                  } catch (Exception e) {
                      e.printStackTrace();
                  }
              });
  
  
              //再次遍历nodes,将所有的bean标签再遍历一次，给对象赋值
              nodes.forEach(node -> {
                  try {
                      //首先强转node，Element有更多的方法
                      Element element = (Element) node;
                      //获取配置文件中的id,后面的给set赋值会使用到
                      String id = element.attributeValue("id");
                      //获取className
                      String className = element.attributeValue("class");
                      //获取方法，之前需要获取类,这里获取类为后面做准备
                      Class<?> aClass = Class.forName(className);
                      //获取property标签
                      List<Node> propertys = element.selectNodes("property");
                      //获取property标签的所有属性
                      propertys.forEach(property -> {
                          try {
                              Element pro = (Element) property;
                              //获取property的属性name
                              String propertyName = pro.attributeValue("name");
                              //获取参数类型
                              Field field = aClass.getDeclaredField(propertyName);
                              logger.info("属性名：" + propertyName);
                              //获取方法名
                              String setMethodName = "set" + propertyName.toUpperCase().charAt(0) + propertyName.substring(1);
                              //获取方法，需要两个参数，一个方法名，一个参数类型
                              Method setMethod = aClass.getDeclaredMethod(setMethodName, field.getType());
                              //下面调用set方法，首先获取property属性中value、ref
                              String value = pro.attributeValue("value");
                              String ref = pro.attributeValue("ref");
                              //在外面定义一个Object来接收
                              Object actualvalue = null;
                              if (value != null) {
                                  //这里value不为空说明是一个简单类型
                                  //这里我们指定一下我们的框架指定的简单类型是以下这些:
                                  /*
                                      byte  short  int  long  float  double  boolean char
                                      Byte  Short  Integer  Long  Float  Double  Boolean   Character
                                      String
                                   */
                                  //首先获取传入参数的类型字符串
                                  String propertyTypeSimpleName = field.getType().getSimpleName();
                                  //通过switch做判断
                                  switch (propertyTypeSimpleName){
                                      case "byte":
                                          actualvalue = Byte.parseByte(value);
                                          break;
                                      case "short":
                                          actualvalue = Short.parseShort(value);
                                          break;
                                      case "int":
                                          actualvalue = Integer.parseInt(value);
                                          break;
                                      case "long":
                                          actualvalue = Long.parseLong(value);
                                          break;
                                      case "float":
                                          actualvalue = Float.parseFloat(value);
                                          break;
                                      case "double":
                                          actualvalue = Double.parseDouble(value);
                                          break;
                                      case "char":
                                          actualvalue = value.charAt(0);
                                          break;
                                      case "boolean":
                                          actualvalue = Boolean.parseBoolean(value);
                                          break;
                                      case "Byte":
                                          actualvalue = Byte.valueOf(value);
                                          break;
                                      case "Short":
                                          actualvalue = Short.valueOf(value);
                                          break;
                                      case "Integer":
                                          actualvalue = Integer.valueOf(value);
                                          break;
                                      case "Long":
                                          actualvalue = Long.valueOf(value);
                                          break;
                                      case "Float":
                                          actualvalue = Float.valueOf(value);
                                          break;
                                      case "Double":
                                          actualvalue = Double.valueOf(value);
                                          break;
                                      case "Boolean":
                                          actualvalue = Boolean.valueOf(value);
                                          break;
                                      case "Character":
                                          actualvalue = Character.valueOf(value.charAt(0));
                                          break;
                                      case "String":
                                          actualvalue = value;
                                  }
                                  setMethod.invoke(singletonObjects.get(id),actualvalue);
                              }
                              if (ref != null) {
                                  //这里ref不为空，说明是个非简单类型
                                  //这里的get(id)，就是调用方法的对象，而get(ref)是值，这个ref也就是id,可以通过Map的get方法获取
                                  setMethod.invoke(singletonObjects.get(id),singletonObjects.get(ref));
                              }
                          } catch (Exception e) {
                              e.printStackTrace();
                          }
                      });
                  } catch (ClassNotFoundException e) {
                      e.printStackTrace();
                  }
              });
  
  
          } catch (Exception e) {
              e.printStackTrace();
          }
      }
  }
  ```

  


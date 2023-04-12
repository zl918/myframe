package org.myspringframework.core;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return singletonObjects.get(name);
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

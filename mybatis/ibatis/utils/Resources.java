package org.god.ibatis.utils;

/**
 * @program: MyBatis
 * @description: 工具类，用于解析MyBatis的核心配置文件转化成输入流传入build
 * @author: Zhang Nan
 * @create: 2022-10-29 09:40
 **/

import java.io.InputStream;

/**
 * 这个工具类，通过分析程序，是从类的根路径下获取文件
 */
public class Resources {
    /**
     * 构造方法私有化，防止new对象，可以直接调用，这是一种规范写法
     */
    private Resources(){}

    /**
     * 静态方法可以直接通过类调用，加载核心配置文件的
     * @param xmlDocumrent   核心配置文件
     * @return   返回一个流对象
     */
    public static InputStream getResourceAsReader(String xmlDocumrent){
        return ClassLoader.getSystemClassLoader().getResourceAsStream(xmlDocumrent);
    }
}

package org.myspringframework.core;

/**
 * myspring的应用上下文接口，有getBean方法，获取Bean对象
 */
public interface ApplicationContext {

    Object getBean(String name);
}

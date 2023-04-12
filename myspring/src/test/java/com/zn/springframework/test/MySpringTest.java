package com.zn.springframework.test;

import com.zn.spring.bean.UserService;
import org.junit.Test;
import org.myspringframework.core.ApplicationContext;
import org.myspringframework.core.ClassPathXmlApplicationContext;

/**
 * @program: Spring6
 * @description: 测试手写的spring框架
 * @author: Zhang Nan
 * @create: 2022-11-11 16:23
 **/
public class MySpringTest {

    @Test
    public void testMySpring(){
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("myspring.xml");
        Object user = applicationContext.getBean("User");
        System.out.println(user);

        UserService userService = (UserService) applicationContext.getBean("UserService");
        userService.save();
    }
}

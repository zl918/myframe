package com.zn.spring.bean;

/**
 * @program: Spring6
 * @description:
 * @author: Zhang Nan
 * @create: 2022-11-11 15:14
 **/
public class UserService {

    private UserDao userDao;

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public void save(){
        userDao.insert();
    }
}

package com.linmu.collision_warning_system.utils;

import android.content.Context;

import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {
    public static Properties getProperties(Context c,String filename){
        Properties urlProps;
        Properties props = new Properties();
        try {
            //方法一：通过activity中的context攻取setting.properties的FileInputStream
            //注意这地方的参数appConfig在eclipse中应该是appConfig.properties才对,但在studio中不用写后缀
            InputStream in = c.getAssets().open(filename);
//            InputStream in = c.getAssets().open("appConfig");
            //方法二：通过class获取setting.properties的FileInputStream
            //InputStream in = PropertiesUtil.class.getResourceAsStream("/assets/  setting.properties "));
            props.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        urlProps = props;
        return urlProps;
    }
}

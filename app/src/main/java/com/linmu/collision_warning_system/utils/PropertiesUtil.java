package com.linmu.collision_warning_system.utils;

import java.io.InputStream;
import java.util.Properties;
/**
 * @version V1.0
 * @name Properties工具类
 * @author linmu
 * @description 用于读取 properties 文件中的配置
 * @date 2023-04-06 14:03
*/
public class PropertiesUtil {
    public static Properties config = getProperties("CommunicationConfig.properties");
    /**
     * @name 获取 Properties 对象
     * @description TODO
     * @param filename 文件名
     * @return Properties Properties对象
     * @date 2023-04-06 14:04
     */
    public static Properties getProperties(String filename){
        try {
            Properties props = new Properties();
            //方法一：通过activity中的context攻取setting.properties的FileInputStream
            //注意这地方的参数appConfig在eclipse中应该是appConfig.properties才对,但在studio中不用写后缀
//            InputStream in = c.getAssets().open(filename);
            //方法二：通过class获取setting.properties的FileInputStream
            InputStream in = PropertiesUtil.class.getResourceAsStream("/assets/"+filename);
            props.load(in);
            return props;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static String getValue(String key) {
        return config.getProperty(key);
    }
}

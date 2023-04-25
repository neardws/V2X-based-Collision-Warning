package com.linmu.collision_warning_system;

import static org.junit.Assert.assertEquals;

import com.linmu.collision_warning_system.Entry.Coordinate;
import com.linmu.collision_warning_system.services.WarningService;

import org.junit.Test;

/**
 * @author linmu
 * @version V1.0
 * @name warningTest
 * @description 预警测试类
 * @date 2023-04-19 19:05
 **/
public class WarningTest {

    @Test
    public void testGauss() {
        WarningService warningService = WarningService.getInstance();
        Coordinate coordinate1 = new Coordinate(0.0,0.0,0.0);

        for(int i=-10;i<=10;i++) {
            Coordinate coordinate2 = new Coordinate(2.0+0.1*i,0.0,0.0);
            double res = warningService.carGaussianCdf(coordinate1,coordinate2,1.0d);
            System.out.println(res);
        }
        assertEquals(4, 2 + 2);
    }
}

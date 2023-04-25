package com.linmu.collision_warning_system;

import static org.junit.Assert.assertEquals;

import com.baidu.mapapi.model.LatLng;
import com.linmu.collision_warning_system.services.CarManageService;

import org.junit.Test;

public class CarManageServiceTest {

    @Test
    public void testRemoveCar() {
        CarManageService carManageService = CarManageService.getInstance();
        carManageService.addCarInfo("111",new LatLng(0.1d,0.1d),0.0d,0.0f,0.0f);
        carManageService.addCarInfo("222",new LatLng(0.1d,0.1d),0.0d,0.0f,0.0f);
        carManageService.addCarInfo("333",new LatLng(0.1d,0.1d),0.0d,0.0f,0.0f);
        for (int i = 0; i < 30; i++) {
            carManageService.updateCarsLife();
        }
        carManageService.addCarInfo("111",new LatLng(0.1d,0.1d),0.0d,0.0f,0.0f);
        carManageService.addCarInfo("444",new LatLng(0.1d,0.1d),0.0d,0.0f,0.0f);
        System.out.println("剩余车数量:"+carManageService.getCarCount());
        assertEquals(4, carManageService.getCarCount());
        for (int i = 0; i < 80; i++) {
            carManageService.updateCarsLife();
        }
        System.out.println("剩余车数量:"+carManageService.getCarCount());
        assertEquals(2, carManageService.getCarCount());
    }
}

package com.linmu.collision_warning_system.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMapOptions;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.linmu.collision_warning_system.Entry.Car;
import com.linmu.collision_warning_system.R;
import com.linmu.collision_warning_system.services.CarManageService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment {
    private MapView mapView;
    private BaiduMap mBaiduMap;
    private BaiduMapOptions baiduMapOptions;
    private HashMap<String,Polyline> polylineHashMap;
    private HashMap<String, Marker> markerHashMap;
    private final BitmapDescriptor mBitmapCar = BitmapDescriptorFactory.fromResource(R.drawable.vehicle_xhdpi);
    private final BitmapDescriptor mGreenTexture = BitmapDescriptorFactory.fromAsset("Icon_road_green_arrow.png");

    public MapFragment() {
        // Required empty public constructor
    }
    private MapFragment(BaiduMapOptions options) {
        this.baiduMapOptions = options;
    }

    public static MapFragment newInstance(BaiduMapOptions options) {
        return new MapFragment(options);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        polylineHashMap = new HashMap<>();
        markerHashMap = new HashMap<>();
        getParentFragmentManager().setFragmentResultListener("NcsLocationForMap", this, this::doHandleNcsLocation);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.mapView = new MapView((this.requireActivity()),this.baiduMapOptions);
        this.mBaiduMap = this.mapView.getMap();
        // Inflate the layout for this fragment
        return this.mapView;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void onResume() {
        super.onResume();
        // 激活定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 隐藏logo
        View child = mapView.getChildAt(1);
        if (child instanceof ImageView){
            child.setVisibility(View.INVISIBLE);
        }
        // 固定显示缩放比例
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(20.0f);
        mBaiduMap.setMapStatus(msu);

        // 配置设置
        MyLocationConfiguration myLocationConfiguration = new MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.FOLLOWING,
                true,
                mBitmapCar);

        mBaiduMap.setMyLocationConfiguration(myLocationConfiguration);
        this.mapView.onResume();
    }

    public void onPause() {
        super.onPause();
        this.mapView.onPause();
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mapView.onDestroy();
        if (null != mBitmapCar) {
            mBitmapCar.recycle();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (null != mGreenTexture) {
            mGreenTexture.recycle();
        }
        mapView.onDestroy();
        mapView = null;
    }

    private void doHandleNcsLocation(String requestKey,Bundle result) {
        // 当 MapView 被销毁后，停止处理
        if(mapView == null) {
            Log.e("mMapView", "onReceiveLocation: no MapView" );
            return;
        }
        int type = result.getInt("type");
        if(type == 1) {
            Car carSelf = CarManageService.getCarSelf();
            LatLng latLng = carSelf.getLatLng();
            MyLocationData locationData = new MyLocationData.Builder()
                    .direction(carSelf.getDirection())
                    .latitude(latLng.latitude)
                    .longitude(latLng.longitude)
                    .build();
            mBaiduMap.setMyLocationData(locationData);
            drawUpdatePolyLine(carSelf);
        }
        else if(type == 2) {
            List<Car> carList = CarManageService.getInstance().getCarList();
            for(Car car:carList) {
                if(car.getLife() < 8) {
                    continue;
                }
                drawUpdatePolyLine(car);
                drawUpdateMarker(car);
            }
        }
        else {
            Log.e("MyLogTag", "doHandleNcsLocation: type 错误无法解析");
        }
    }
    private Marker initMarker(@NonNull Car car) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(car.getLatLng())
                .rotate(car.getDirection())
                .anchor(0.5f,0.5f)
                .icon(mBitmapCar);
        Marker marker = (Marker) mBaiduMap.addOverlay(markerOptions);
        markerHashMap.put(car.getCarId(), marker);
        return marker;
    }

    private void drawUpdateMarker(@NonNull Car car) {
        Marker marker = markerHashMap.get(car.getCarId());
        if(marker == null) {
            marker = initMarker(car);
        }
        marker.setPosition(car.getLatLng());
        marker.setRotate(car.getDirection());
    }
    /**
     * 初始化路径纹理
     */
    private Polyline initPolyLine(@NonNull Car car) {
        // 初始化需要至少两个点数据
        List<LatLng> polylineList = new ArrayList<>(car.getLatLngDeque());
        // 绘制纹理PolyLine
        PolylineOptions polylineOptions =
                new PolylineOptions().points(polylineList)
                        .width(20)
                        .customTexture(mGreenTexture)
                        .dottedLine(true)
                        .zIndex(3);
        Polyline polyline = (Polyline) mBaiduMap.addOverlay(polylineOptions);
        polylineHashMap.put(car.getCarId(), polyline);
        return polyline;
    }
    /**
     * 更新&绘制路径
     */
    private void drawUpdatePolyLine(@NonNull Car car) {
        List<LatLng> polylineList = new ArrayList<>(car.getLatLngDeque());
        Collections.reverse(polylineList);
        Polyline polyline = polylineHashMap.get(car.getCarId());
        if(polyline == null) {
            polyline = initPolyLine(car);
        }
        polyline.setPoints(polylineList);
    }
}
package com.linmu.collision_warning_system.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMapOptions;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment {
    private MapView mapView;
    private BaiduMapOptions baiduMapOptions;


    public MapFragment() {
        // Required empty public constructor
    }

    private MapFragment(BaiduMapOptions options) {
        this.baiduMapOptions = options;
    }

    public static MapFragment newInstance() {
        return new MapFragment();
    }
    public static MapFragment newInstance(BaiduMapOptions options) {
        return new MapFragment(options);
    }

    public BaiduMap getBaiduMap() {
        return this.mapView == null ? null : this.mapView.getMap();
    }

    public MapView getMapView() {
        return this.mapView;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.mapView = new MapView((this.requireActivity()),this.baiduMapOptions);
        // Inflate the layout for this fragment
        return this.mapView;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void onResume() {
        super.onResume();
        this.mapView.onResume();
        BaiduMap baiduMap = this.getBaiduMap();
        // 激活定位图层
        baiduMap.setMyLocationEnabled(true);
//        // 隐藏logo
//        View child = mapView.getChildAt(1);
//        if (child instanceof ImageView){
//            child.setVisibility(View.INVISIBLE);
//        }
        // 固定显示缩放比例
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(19.0f);
        baiduMap.setMapStatus(msu);

        // 配置设置
        MyLocationConfiguration myLocationConfiguration = new MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.FOLLOWING,
                true,
                null);

        baiduMap.setMyLocationConfiguration(myLocationConfiguration);
    }

    public void onPause() {
        super.onPause();
        this.mapView.onPause();
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mapView.onDestroy();
    }

    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        mapView = null;
    }

}
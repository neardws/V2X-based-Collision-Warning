package com.linmu.collision_warning_system.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.linmu.collision_warning_system.R;

import java.util.ArrayList;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OfflineMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OfflineMapFragment extends Fragment implements MKOfflineMapListener {
    private MKOfflineMap mOffline = null;
    private TextView cidView;
    private TextView stateView;
    private EditText cityNameView;
    // 已下载的离线地图信息列表
    private ArrayList<MKOLUpdateElement> localMapList = null;
    private LocalMapAdapter lAdapter = null;
    private LinearLayout mCityList;
    private LinearLayout mLocalMap;
    public OfflineMapFragment() {
        // Required empty public constructor
    }


    public static OfflineMapFragment newInstance() {
        return new OfflineMapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_offline_map, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View rootView = requireView();
        mOffline = new MKOfflineMap();
        mOffline.init(this);
        initView();

        rootView.findViewById(R.id.searchOfflineMap).setOnClickListener(this::searchOfflineMap);
        rootView.findViewById(R.id.startDownload).setOnClickListener(this::startDownload);
        rootView.findViewById(R.id.stopDownload).setOnClickListener(this::stopDownload);
        rootView.findViewById(R.id.removeOfflineMap).setOnClickListener(this::removeOfflineMap);
        rootView.findViewById(R.id.cityListButton).setOnClickListener(this::clickCityListButton);
        rootView.findViewById(R.id.localButton).setOnClickListener(this::clickLocalMapListButton);

    }

    @Override
    public void onPause() {
        super.onPause();
        int cityId = Integer.parseInt(cidView.getText().toString());
        MKOLUpdateElement temp = mOffline.getUpdateInfo(cityId);
        if (temp != null && temp.status == MKOLUpdateElement.DOWNLOADING) {
            mOffline.pause(cityId);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 退出时，销毁离线地图模块
        mOffline.destroy();
    }
    private void initView() {
        View rootView = requireView();
        cidView = (TextView) rootView.findViewById(R.id.cityId);
        cityNameView = (EditText) rootView.findViewById(R.id.downloadCity);
        stateView = (TextView) rootView.findViewById(R.id.downloadState);
        mCityList = (LinearLayout) rootView.findViewById(R.id.cityList_layout);
        mLocalMap = (LinearLayout) rootView.findViewById(R.id.localMap_layout);

        ListView hotCityList = (ListView) rootView.findViewById(R.id.hotCityList);
        ArrayList<String> hotCities = new ArrayList<>();
        final ArrayList<String> hotCityNames = new ArrayList<>();
        // 获取热闹城市列表
        ArrayList<MKOLSearchRecord> records1 = mOffline.getHotCityList();
        if (records1 != null) {
            for (MKOLSearchRecord r : records1) {
                //V4.5.0起，保证数据不溢出，使用long型保存数据包大小结果
                hotCities.add(r.cityName + "(" + r.cityID + ")" + "   --" + this.formatDataSize(r.dataSize));
                hotCityNames.add(r.cityName);
            }
        }
        ListAdapter hAdapter = (ListAdapter) new ArrayAdapter<>(this.getContext(), android.R.layout.simple_list_item_1, hotCities);
        hotCityList.setAdapter(hAdapter);
        hotCityList.setOnItemClickListener((adapterView, view, i, l) -> cityNameView.setText(hotCityNames.get(i)));

        ListView allCityList = (ListView) rootView.findViewById(R.id.allCityList);
        // 获取所有支持离线地图的城市
        ArrayList<String> allCities = new ArrayList<>();
        final ArrayList<String> allCityNames = new ArrayList<>();
        ArrayList<MKOLSearchRecord> records2 = mOffline.getOfflineCityList();
        if (records2 != null) {
            for (MKOLSearchRecord r : records2) {
                //V4.5.0起，保证数据不溢出，使用long型保存数据包大小结果
                allCities.add(r.cityName + "(" + r.cityID + ")" + "  --" + this.formatDataSize(r.dataSize));
                allCityNames.add(r.cityName);
            }
        }
        ListAdapter aAdapter = (ListAdapter) new ArrayAdapter<>(this.getContext(), android.R.layout.simple_list_item_1, allCities);
        allCityList.setAdapter(aAdapter);
        allCityList.setOnItemClickListener((adapterView, view, i, l) -> cityNameView.setText(allCityNames.get(i)));
        mLocalMap.setVisibility(View.GONE);
        mCityList.setVisibility(View.VISIBLE);

        // 获取已下过的离线地图信息
        localMapList = mOffline.getAllUpdateInfo();
        if (localMapList == null) {
            localMapList = new ArrayList<>();
        }

        ListView localMapListView = (ListView) rootView.findViewById(R.id.localMapList);
        lAdapter = new LocalMapAdapter();
        localMapListView.setAdapter(lAdapter);
    }

    /**
     * 切换至城市列表
     */
    public void clickCityListButton(View view) {
        mLocalMap.setVisibility(View.GONE);
        mCityList.setVisibility(View.VISIBLE);
    }

    /**
     * 切换至下载管理列表
     */
    public void clickLocalMapListButton(View view) {
        mLocalMap.setVisibility(View.VISIBLE);
        mCityList.setVisibility(View.GONE);
    }

    /**
     * 搜索离线城市
     */
    public void searchOfflineMap(View view) {
        ArrayList<MKOLSearchRecord> records = mOffline.searchCity(cityNameView.getText().toString());

        if (records == null || records.size() != 1) {
            Toast.makeText(this.getContext(), "不支持该城市离线地图", Toast.LENGTH_SHORT).show();
            return;
        }
        cidView.setText(String.valueOf(records.get(0).cityID));
    }

    /**
     * 开始下载
     *
     */
    public void startDownload(View view) {
        int cityId = Integer.parseInt(cidView.getText().toString());
        mOffline.start(cityId);
        clickLocalMapListButton(null);

        Toast.makeText(this.getContext(), "开始下载离线地图. cityId: " + cityId, Toast.LENGTH_SHORT).show();
        updateView();
    }

    /**
     * 暂停下载
     *
     */
    public void stopDownload(View view) {
        int cityId = Integer.parseInt(cidView.getText().toString());
        mOffline.pause(cityId);
        Toast.makeText(this.getContext(), "暂停下载离线地图. cityId: " + cityId, Toast.LENGTH_SHORT).show();
        updateView();
    }

    /**
     * 删除离线地图
     *
     */
    public void removeOfflineMap(View view) {
        int cityId = Integer.parseInt(cidView.getText().toString());
        mOffline.remove(cityId);
        Toast.makeText(this.getContext(), "删除离线地图. cityId: " + cityId, Toast.LENGTH_SHORT).show();
        updateView();
    }

    /**
     * 更新状态显示
     */
    public void updateView() {
        localMapList = mOffline.getAllUpdateInfo();
        if (localMapList == null) {
            localMapList = new ArrayList<>();
        }
        lAdapter.notifyDataSetChanged();
    }

    /**
     * V4.5.0起，保证数据不溢出，使用long型保存数据包大小结果
     */
    public String formatDataSize(long size) {
        return size < (1024 * 1024) ? String.format(Locale.CHINA,"%d K", size / 1024) : String.format(Locale.CHINA,"%.1f M", size / (1024 * 1024.0));
    }

    @Override
    public void onGetOfflineMapState(int type, int state) {
        switch (type) {
            case MKOfflineMap.TYPE_DOWNLOAD_UPDATE:
                MKOLUpdateElement update = mOffline.getUpdateInfo(state);
                // 处理下载进度更新提示
                if (update != null) {
                    stateView.setText(String.format(Locale.CHINA,"%s : %d%%", update.cityName, update.ratio));
                    updateView();
                }
                break;

            case MKOfflineMap.TYPE_NEW_OFFLINE:
                // 有新离线地图安装
                Log.d("OfflineDemo", String.format("add offlineMap num:%d", state));
                break;

            case MKOfflineMap.TYPE_VER_UPDATE:
                // 版本更新提示
                // MKOLUpdateElement e = mOffline.getUpdateInfo(state);
                break;

            default:
                break;
        }
    }
    /**
     * 离线地图管理列表适配器
     */
    public class LocalMapAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return localMapList.size();
        }

        @Override
        public Object getItem(int index) {
            return localMapList.get(index);
        }

        @Override
        public long getItemId(int index) {
            return index;
        }

        @Override
        public View getView(int index, View view, ViewGroup parent) {
            ViewHolder holder;
            MKOLUpdateElement e = (MKOLUpdateElement) getItem(index);
            if(view == null) {
                LayoutInflater layoutInflater = LayoutInflater.from(OfflineMapFragment.this.getContext());
                view = layoutInflater.inflate(R.layout.offline_localmap_item, parent,false);
                holder = new ViewHolder();
                holder.setDisplay((Button) view.findViewById(R.id.display));
                holder.setRemove((Button) view.findViewById(R.id.remove));
                holder.setRatio((TextView) view.findViewById(R.id.ratio));
                holder.setTitle((TextView) view.findViewById(R.id.title));
                holder.setUpdate((TextView) view.findViewById(R.id.update));
                view.setTag(holder);
            }
            else {
                holder = (ViewHolder) view.getTag();
            }
            holder.getRatio().setText(String.format(Locale.CHINA,"%d%%",e.ratio));
            holder.getTitle().setText(e.cityName);
            String updateWord = e.update ? "可更新" : "最新";
            holder.getUpdate().setText(updateWord);
            holder.getDisplay().setEnabled(e.ratio == 100);
            holder.getRemove().setOnClickListener(arg0 -> {
                mOffline.remove(e.cityID);
                updateView();
            });

            return view;
        }
        class ViewHolder{
            private Button display;
            private Button remove;
            private TextView title;
            private TextView update;
            private TextView ratio;

            public Button getDisplay() {
                return display;
            }

            public void setDisplay(Button display) {
                this.display = display;
            }

            public Button getRemove() {
                return remove;
            }

            public void setRemove(Button remove) {
                this.remove = remove;
            }

            public TextView getTitle() {
                return title;
            }

            public void setTitle(TextView title) {
                this.title = title;
            }

            public TextView getUpdate() {
                return update;
            }

            public void setUpdate(TextView update) {
                this.update = update;
            }

            public TextView getRatio() {
                return ratio;
            }

            public void setRatio(TextView ratio) {
                this.ratio = ratio;
            }
        }

    }
}
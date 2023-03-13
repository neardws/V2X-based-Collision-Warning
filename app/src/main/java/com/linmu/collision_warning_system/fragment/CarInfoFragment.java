package com.linmu.collision_warning_system.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.linmu.collision_warning_system.R;
import com.linmu.collision_warning_system.services.CarManageService;
import com.txusballesteros.SnakeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.RoundingMode;
import java.text.NumberFormat;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CarInfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CarInfoFragment extends Fragment {

    private SnakeView snakeView;

    public CarInfoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CarInfoFragment.
     */
    public static CarInfoFragment newInstance() {
        return new CarInfoFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener("MyNcsLocationForCarInfo", this, this::doHandleNcsLocation);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_car_info, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View rootView = requireView();
        TextView obuIdValue = rootView.findViewById(R.id.obuIdValue);
        obuIdValue.setText(CarManageService.getCarSelf().getCarId());
        snakeView = (SnakeView) rootView.findViewById(R.id.snake);
        snakeView.setMinValue(0.0f);
        snakeView.setMaxValue(200.0f);
    }

    private void doHandleNcsLocation(String requestKey,Bundle result) {
        String dataString = result.getString("LocationData");
        String obu_id;
        double latitude,longitude,speed;
        try {
            JSONObject data = new JSONObject(dataString);
            obu_id = data.getString("device_id");
            latitude = data.getDouble("lat");
            longitude = data.getDouble("lon");
            speed = data.getDouble("spd");
            boolean latLonValid = data.getBoolean("pos_valid");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        View rootView = requireView();

        if(!obu_id.equals(CarManageService.getCarSelf().getCarId())) {
            Log.w("doHandleNcsLocation", "车辆ID与本车不匹配");
            return;
        }

        // 给速度曲线添加值
        snakeView.addValue((float) speed);
        // 更新速度文本
        TextView speedTextView = rootView.findViewById(R.id.speed);
        NumberFormat nf_speed = NumberFormat.getNumberInstance();
        nf_speed.setMaximumFractionDigits(2);
        nf_speed.setRoundingMode(RoundingMode.UP);
        speedTextView.setText(nf_speed.format(speed));

        // 更新经纬度文本
        TextView coordinateTextView = rootView.findViewById(R.id.location_coordinate);
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(5);
        nf.setRoundingMode(RoundingMode.UP);
        String coordinate = "( " + nf.format(longitude) + " , " + nf.format(latitude) + " )";
        coordinateTextView.setText(coordinate);
    }
}
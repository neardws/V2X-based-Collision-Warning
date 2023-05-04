package com.linmu.collision_warning_system.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.linmu.collision_warning_system.R;
import com.linmu.collision_warning_system.services.NcsService;
import com.linmu.collision_warning_system.services.WarningService;
import com.linmu.collision_warning_system.utils.PropertiesUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LogFragment extends Fragment {
    private List<Long> delayList;
    private int testCount;

    private LogFragment() {
        // Required empty public constructor
    }
    public static LogFragment newInstance() {
        return new LogFragment();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.setFragmentResultListener("NcsLog", this, this::doHandleNcsLog);
        fragmentManager.setFragmentResultListener("NcsTime", this, this::doHandleNcsTime);
        delayList = new ArrayList<>();
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_communication, container, false);

        TextView valueTest = view.findViewById(R.id.valueText);
        valueTest.setText(String.format("%s", WarningService.getInstance().getWarningValue()));

        EditText inputTest = view.findViewById(R.id.inputTest);

        inputTest.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_DONE) {
                String str = v.getText().toString();
                double value = Double.parseDouble(str)/(Math.pow(10.0d,str.length()));
                WarningService.getInstance().setWarningValue(value);
                valueTest.setText(String.format("%s", value));
                Log.w("MyLogTag", "onEditorAction: "+value);
            }
            return false;
        });
        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button sentButton = view.findViewById(R.id.sendButton);
        sentButton.setOnClickListener(this::doOnSendButtonClick);
    }
    private void doOnSendButtonClick(View view) {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        long period = Long.parseLong(PropertiesUtil.getValue("test.period"));
        int testTime = Integer.parseInt(PropertiesUtil.getValue("test.num"));
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            int countTime = testTime;
            @Override
            public void run() {
                countTime -= 1;
                if(countTime < 0) {
                    scheduledExecutorService.shutdown();
                    return;
                }
                NcsService.getInstance().testWifiDelay();
            }
        },0,period, TimeUnit.MILLISECONDS);
    }

    private void doHandleNcsTime(String requestKey, @NonNull Bundle result) {
        testCount += 1;
        long sendTime = result.getLong("sendTime");
        long receiveTime = result.getLong("receiveTime");
        long delay = (receiveTime - sendTime)/2;
        delayList.add(delay);

        // 更新接收消息页
        TextView delayText = this.requireView().findViewById(R.id.delayText);
        StringBuilder stringBuilder = new StringBuilder();
        for(long delayData: delayList) {
            stringBuilder.append(delayData);
            stringBuilder.append(",");
        }
        stringBuilder.append("\n");
        stringBuilder.append("count: ");
        stringBuilder.append(testCount);
        stringBuilder.append(", mean: ");
        double delay_mean = delayList.stream().mapToLong(Long::longValue).average().orElse(0.0d);
        stringBuilder.append(delay_mean);
        stringBuilder.append(", min: ");
        long delay_min = delayList.stream().mapToLong(Long::longValue).min().orElse(0L);
        stringBuilder.append(delay_min);
        stringBuilder.append(", max: ");
        long delay_max = delayList.stream().mapToLong(Long::longValue).max().orElse(0L);
        stringBuilder.append(delay_max);

        delayText.setText(stringBuilder.toString());
    }
    private void doHandleNcsLog(String requestKey, @NonNull Bundle result) {
        String res = result.getString("log");
        int type = result.getInt("type");
        TextView textView;
        if(type == 1) {
            textView = this.requireView().findViewById(R.id.thisCarLogText);
        }
        else if(type == 2) {
            textView = this.requireView().findViewById(R.id.otherCarLogText);
        }
        else {
            textView = this.requireView().findViewById(R.id.distanceText);
        }
        // 更新接收消息页
        textView.setText(res);
    }

}
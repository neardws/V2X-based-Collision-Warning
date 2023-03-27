package com.linmu.collision_warning_system.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.linmu.collision_warning_system.R;
import com.linmu.collision_warning_system.services.CommunicationService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CommunicationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CommunicationFragment extends Fragment {

    private Long lastTime;
    private List<Long> delayList;
    private CommunicationService communicationService;

    private CommunicationFragment() {
        // Required empty public constructor
    }
    public static CommunicationFragment newInstance() {
        return new CommunicationFragment();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.setFragmentResultListener("NcsLog", this, this::doHandleNcsLog);
        fragmentManager.setFragmentResultListener("NcsTime", this, this::doHandleNcsTime);
        lastTime = 0L;
        delayList = new ArrayList<>();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_communication, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button sentButton = view.findViewById(R.id.sendButton);
        sentButton.setOnClickListener(this::doOnSendButtonClick);
    }
    private void doOnSendButtonClick(View view) {
        this.sentAskNcsMessage();
    }
    private void doHandleNcsLog(String requestKey, @NonNull Bundle result) {
        String res = result.getString("log");
        // 更新接收消息页
        TextView receiveTextView = this.requireView().findViewById(R.id.receiveText);
        if(receiveTextView == null) {
            Log.e("doHandleReceiveMessage", "receiveTextView 为空!");
            return;
        }
        receiveTextView.setText(res);
    }

    private void sentAskNcsMessage() {
        JSONObject askNcsState;
        try {
            askNcsState = new JSONObject();
            askNcsState.put("tag",2112);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        communicationService.sentAndReceiveTest(50505,askNcsState,0);
    }

    private void doHandleNcsTime(String requestKey, @NonNull Bundle result) {
        long time = result.getLong("time");
        if(lastTime == 0L) {
            lastTime = time;
            this.sentAskNcsMessage();
            return;
        }
        long delay = (time - lastTime)/2;
        delayList.add(delay);


        // 更新接收消息页
        TextView delayText = this.requireView().findViewById(R.id.delayText);
        StringBuilder stringBuilder = new StringBuilder();
        for(long delayData: delayList) {
            stringBuilder.append(delayData);
            stringBuilder.append(" ");
        }
        delayText.setText(stringBuilder.toString());

        lastTime = 0L;
    }
    public void initCommunication() {
        this.communicationService = CommunicationService.getInstance();
    }
}
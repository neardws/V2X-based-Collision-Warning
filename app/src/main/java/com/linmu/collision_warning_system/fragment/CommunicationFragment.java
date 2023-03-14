package com.linmu.collision_warning_system.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import com.linmu.collision_warning_system.services.CarManageService;
import com.linmu.collision_warning_system.services.CommunicationService;
import com.linmu.collision_warning_system.services.NcsLocationService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CommunicationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CommunicationFragment extends Fragment {

    private EditText inputEditText = null;

    private CommunicationService communicationService;

    private CommunicationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment CommunicationConfigFragment.
     */
    public static CommunicationFragment newInstance() {
        return new CommunicationFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_communication_config, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        inputEditText = view.findViewById(R.id.inputEditText);
        Button sentButton = view.findViewById(R.id.sendButton);
        sentButton.setOnClickListener(this::doOnSendButtonClick);
    }

    private void doOnSendButtonClick(View view) {
        String inputText = inputEditText.getText().toString();
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(inputText);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        // port选择为-1表示随机端口
        communicationService.sentMessage(-1,jsonObject);
    }

    public void initCommunication() {
        this.communicationService = CommunicationService.getInstance();
        // 接受消息的 handler，具体处理放在 doHandleReceiveMessage
        Handler receiverHandler = new Handler(Looper.getMainLooper(), this::doHandleReceiveMessage);
        this.communicationService.setReceiverHandler(receiverHandler);
    }


    private boolean doHandleReceiveMessage(Message msg) {
        boolean handleRes;
        switch (msg.what) {
            case 1111: {
                handleRes = NcsLocationService.getInstance().doHandleReceiveOnceMessage(msg);
                return handleRes;
            }
            case 2222: {
                handleRes = doHandleLocationMessage(msg);
                return handleRes;
            }
        }
        return false;
    }

    private boolean doHandleLocationMessage(Message msg) {
        JSONObject resJsonObject = (JSONObject) msg.obj;
        if(CarManageService.getCarSelf() == null) {
            Log.w("doHandleReceiveMessage", "本车还没有完成初始化! 拒绝处理接收消息!");
            return false;
        }
        Log.i("handleMessage", String.format("doHandleReceiverMessage: %s",resJsonObject.toString()));

        // 解析数据包
        int tag;
        JSONObject data;
        try {
            tag = resJsonObject.getInt("tag");
            if(tag == 2101) {
                data = resJsonObject.getJSONObject("data");
            }
            else if (tag == 2102){
                // TODO 多车位置处理
                return true;
            }
            else {
                Log.e("doHandleReceiveMessage", "无法处理的tag");
                return false;
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        if(this.inputEditText == null) {
            return false;
        }
        // 更新接收消息页
        TextView receiveTextView = this.requireView().findViewById(R.id.receiveText);
        if(receiveTextView == null) {
            Log.e("doHandleReceiveMessage", "receiveTextView 为空!");
            return false;
        }
        receiveTextView.setText(resJsonObject.toString());

        Bundle ncsLocation = new Bundle();

        ncsLocation.putString("LocationData",data.toString());
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.setFragmentResult("MyNcsLocationForMap",ncsLocation);
        fragmentManager.setFragmentResult("MyNcsLocationForCarInfo",ncsLocation);

        return true;
    }
}
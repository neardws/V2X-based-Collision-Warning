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

import com.linmu.collision_warning_system.R;
import com.linmu.collision_warning_system.services.CommunicationService;

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
    public static CommunicationFragment newInstance() {
        return new CommunicationFragment();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener("NcsLog", this, this::doHandleNcsLog);
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
        communicationService.sentAndReceive(jsonObject,0);
    }
    private void doHandleNcsLog(String requestKey,Bundle result) {
        String res = result.getString("log");
        // 更新接收消息页
        TextView receiveTextView = this.requireView().findViewById(R.id.receiveText);
        if(receiveTextView == null) {
            Log.e("doHandleReceiveMessage", "receiveTextView 为空!");
            return;
        }
        receiveTextView.setText(res);
    }
    public void initCommunication() {
        this.communicationService = CommunicationService.getInstance();
    }
}
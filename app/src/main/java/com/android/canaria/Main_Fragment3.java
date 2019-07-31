package com.android.canaria;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/*더보기 fragment*/

public class Main_Fragment3 extends Fragment {

    TextView username_textView, email_textView;
    String user_id, username, user_email;
    Switch alarm_switch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment3, container, false);

        user_id = Function.getString(getContext(), "user_id");
        username = Function.getString(getContext(), "username");
        user_email = Function.getString(getContext(), "email");

        username_textView = (TextView)view.findViewById(R.id.main_fragment3_username_textView);
        email_textView = (TextView)view.findViewById(R.id.main_fragment3_email_textView);
        username_textView.setText(username);
        email_textView.setText(user_email);

        //스위치 버튼
        alarm_switch = (Switch)view.findViewById(R.id.main_fragment3_receiveAlarm_switch);

        //스위치 on/off -> 알림메시지 수신 여부를 결정
        switchAction();

        return view;
    }


    private void switchAction(){

        boolean isChecked = Function.getBoolean(getContext(), "alarm"); //저장된 스위치 on/off상태 불러옴
        alarm_switch.setChecked(isChecked); //스위치 on/off 초기상태 지정


        //스위치 클릭 리스너
        alarm_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                //스위치 ON/OFF에 따라 boolean값 변화
                if (isChecked) {
                    Function.setBoolean(getContext(), true);
                    Toast.makeText(getActivity(), "Notification is on", Toast.LENGTH_SHORT).show();
                }else{
                    Function.setBoolean(getContext(), false);
                    Toast.makeText(getActivity(), "Notification is off", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }


}

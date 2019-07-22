package com.android.canaria;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/*더보기 fragment*/

public class Main_Fragment3 extends Fragment {

    TextView username_textView, email_textView;
    String user_id, username, user_email;

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

        return view;
    }
}

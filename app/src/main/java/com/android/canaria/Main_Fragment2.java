package com.android.canaria;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class Main_Fragment2 extends Fragment {

    RecyclerView rcv;
    ArrayList<RoomListItem> roomItemList;
    RoomListAdapter adapter;
    LinearLayoutManager linearLayoutManager;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment2, container, false);

        rcv = (RecyclerView)view.findViewById(R.id.main_fragment2_rcv);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        rcv.setHasFixedSize(true);
        rcv.setLayoutManager(linearLayoutManager);
        roomItemList = new ArrayList<>();
        adapter = new RoomListAdapter(roomItemList, getActivity());
        rcv.setAdapter(adapter);


        for(int i=0; i<20; i++){
            roomItemList.add(0, new RoomListItem("Name", 3, "HiHi", "12:30"));
        }

        return view;
    }
}

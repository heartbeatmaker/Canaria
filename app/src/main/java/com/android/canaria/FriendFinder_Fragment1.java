package com.android.canaria;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


    /*원래 구조: FriendFinderActivity 안에 2개의 fragment가 있음
    (qr코드로 찾기=FriendFinder_Fragment1 & 이메일로 검색=FriendFinder_Fragment2)
    qr코드로 찾기 Fragment 안에는 2개의 fragment가 있음
    (스캔하기=FriendFinder_Fragment1_ScanFragment $ 내qr코드=FriendFinder_Fragment1_MyCode)


    qr코드 기능 없이 이메일 검색 기능만 활성화함
    */

public class FriendFinder_Fragment1 extends Fragment {

    TextView textView;

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager = getFragmentManager();

    // bottom navigation view 에서 각각의 메뉴를 누르면 나타나는 프래그먼트
    private FriendFinder_Fragment1_ScanFragment fragment_scan = new FriendFinder_Fragment1_ScanFragment();
    private FriendFinder_Fragment1_MyCodeFragment fragment_myCode = new FriendFinder_Fragment1_MyCodeFragment();


    private void openFragment(final Fragment fragment){
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.friendfinder_fragment1_frameLayout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_friend_finder_fragment1, container, false);

        //첫 화면 지정
        openFragment(fragment_scan);


        textView = (TextView)view.findViewById(R.id.friendFinder_fragment1_textView);

        bottomNavigationView = (BottomNavigationView) view.findViewById(R.id.friendFinder_fragment1_bottom_navigation);
        //하단 네비게이션 뷰 클릭 리스너
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {


                        // 어떤 메뉴 아이템이 터치되었는지 확인합니다.
                        switch (item.getItemId()) {

                            case R.id.menuitem_bottombar_scan:

                                textView.setText("Scan QR Code");
                                openFragment(fragment_scan);

                                return true;

                            case R.id.menuitem_bottombar_myCode:

                                textView.setText("Others can add you using this QR code.");
                                openFragment(fragment_myCode);

                                return true;
                        }
                        return false;
                    }
                });


        return view;
    }






}

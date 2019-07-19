package com.android.canaria;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class FriendFinderActivity extends AppCompatActivity {


    /*원래 구조: FriendFinderActivity 안에 2개의 fragment가 있음
    (qr코드로 찾기=FriendFinder_Fragment1 & 이메일로 검색=FriendFinder_Fragment2)
    qr코드로 찾기 Fragment 안에는 2개의 fragment가 있음
    (스캔하기=FriendFinder_Fragment1_ScanFragment $ 내qr코드=FriendFinder_Fragment1_MyCode)


    qr코드 기능 없이 이메일 검색 기능만 활성화함
    */


    ActionBar actionBar;
    ViewPager pager;
    LinearLayout linearLayout;
    Button scanner_btn, addById_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_finder);

        //상단 액션바 설정
        actionBar = getSupportActionBar();
        actionBar.show();
        actionBar.setTitle("Add Friends");
        actionBar.setDisplayHomeAsUpEnabled(true); //뒤로가기 버튼 - Parent 액티비티가 무엇인지 manifest에 선언해줘야 함

        initViewPager();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_scan_actions, menu);

        return super.onCreateOptionsMenu(menu);
    }




    private void initViewPager(){

        pager = (ViewPager)findViewById(R.id.friendFinder_viewPager);
        pagerAdapter adapter = new pagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);

        linearLayout = (LinearLayout)findViewById(R.id.friendFinder_linearLayout);

//        scanner_btn = (Button)findViewById(R.id.friendFinder_scanner_btn);
        addById_btn = (Button)findViewById(R.id.friendFinder_addById_btn);

        pager.setCurrentItem(0);

//        scanner_btn.setOnClickListener(movePageListener);
//        scanner_btn.setTag(0);
//        addById_btn.setOnClickListener(movePageListener);
        addById_btn.setTag(1);
        addById_btn.setSelected(true);

//        scanner_btn.setSelected(true);

        //swipe 처리하는 부분
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int position) {
                int i = 0;
                while(i<2){
                    if(position == i){
                        linearLayout.findViewWithTag(i).setSelected(true);
//                        viewPager_linearLayout.findViewWithTag(i).setBackgroundColor(R.drawable.grad_bg);
                    }
                    else{
                        linearLayout.findViewWithTag(i).setSelected(false);
//                        viewPager_linearLayout.findViewWithTag(i).setBackgroundColor(R.drawable.quantum_ic_clear_grey600_24);
                    }
                    i++;
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) { }
        });
    }


    //page를 이동하지 않으므로 주석처리함
//    View.OnClickListener movePageListener = new View.OnClickListener(){
//
//        @Override
//        public void onClick(View v) {
//            int tag = (int)v.getTag();
//
//            int i= 0;
//            while(i<2){
//                if(tag == i){
//                    linearLayout.findViewWithTag(i).setSelected(true);
////                    viewPager_linearLayout.findViewWithTag(i).setBackgroundColor(R.drawable.grad_bg);
//                }
//                else{
//                    linearLayout.findViewWithTag(i).setSelected(false);
////                    viewPager_linearLayout.findViewWithTag(i).setBackgroundColor(R.drawable.quantum_ic_clear_grey600_24);
//                }
//                i++;
//            }
//            pager.setCurrentItem(tag);
//        }
//    };



    private class pagerAdapter extends FragmentStatePagerAdapter {

        public pagerAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch(position){
                case 0:
                    return new FriendFinder_Fragment2();
//                case 1:
//                    return new FriendFinder_Fragment2();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 1; //페이지 개수만큼 고쳐야함
        }
    }
}

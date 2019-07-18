package com.android.canaria;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class FriendFinderActivity extends AppCompatActivity {

    ViewPager pager;
    LinearLayout linearLayout;
    Button scanner_btn, addById_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_finder);

        initViewPager();
    }


    private void initViewPager(){

        pager = (ViewPager)findViewById(R.id.friendFinder_viewPager);
        pagerAdapter adapter = new pagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);

        linearLayout = (LinearLayout)findViewById(R.id.friendFinder_linearLayout);

        scanner_btn = (Button)findViewById(R.id.friendFinder_scanner_btn);
        addById_btn = (Button)findViewById(R.id.friendFinder_addById_btn);

        pager.setCurrentItem(0);

        scanner_btn.setOnClickListener(movePageListener);
        scanner_btn.setTag(0);
        addById_btn.setOnClickListener(movePageListener);
        addById_btn.setTag(1);

        scanner_btn.setSelected(true);

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

    View.OnClickListener movePageListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            int tag = (int)v.getTag();

            int i= 0;
            while(i<2){
                if(tag == i){
                    linearLayout.findViewWithTag(i).setSelected(true);
//                    viewPager_linearLayout.findViewWithTag(i).setBackgroundColor(R.drawable.grad_bg);
                }
                else{
                    linearLayout.findViewWithTag(i).setSelected(false);
//                    viewPager_linearLayout.findViewWithTag(i).setBackgroundColor(R.drawable.quantum_ic_clear_grey600_24);
                }
                i++;
            }
            pager.setCurrentItem(tag);
        }
    };



    private class pagerAdapter extends FragmentStatePagerAdapter {

        public pagerAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch(position){
                case 0:
                    return new FriendFinder_Fragment1();
                case 1:
                    return new FriendFinder_Fragment2();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}

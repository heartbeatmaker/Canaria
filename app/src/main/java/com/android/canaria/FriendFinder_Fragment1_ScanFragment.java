package com.android.canaria;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


    /*원래 구조: FriendFinderActivity 안에 2개의 fragment가 있음
    (qr코드로 찾기=FriendFinder_Fragment1 & 이메일로 검색=FriendFinder_Fragment2)
    qr코드로 찾기 Fragment 안에는 2개의 fragment가 있음
    (스캔하기=FriendFinder_Fragment1_ScanFragment $ 내qr코드=FriendFinder_Fragment1_MyCode)


    qr코드 기능 없이 이메일 검색 기능만 활성화함
    */

public class FriendFinder_Fragment1_ScanFragment extends Fragment {


    private IntentIntegrator qrScan;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_friend_finder_fragment1_scan_fragment, container, false);

//        qrScan = new IntentIntegrator(getActivity());
//        IntentIntegrator.forSupportFragment(FriendFinder_Fragment1_ScanFragment.this).initiateScan();

        return view;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(getActivity(), "Cancelled", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(getActivity(), "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                String barcode = result.getContents();

                Log.d("tag", "barcode="+barcode);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}

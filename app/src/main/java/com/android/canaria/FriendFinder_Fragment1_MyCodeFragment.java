package com.android.canaria;

import android.graphics.Bitmap;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.journeyapps.barcodescanner.BarcodeEncoder;


    /*원래 구조: FriendFinderActivity 안에 2개의 fragment가 있음
    (qr코드로 찾기=FriendFinder_Fragment1 & 이메일로 검색=FriendFinder_Fragment2)
    qr코드로 찾기 Fragment 안에는 2개의 fragment가 있음
    (스캔하기=FriendFinder_Fragment1_ScanFragment $ 내qr코드=FriendFinder_Fragment1_MyCode)


    qr코드 기능 없이 이메일 검색 기능만 활성화함
    */

public class FriendFinder_Fragment1_MyCodeFragment extends Fragment {

//    private IntentIntegrator qrScan;
    ImageView qrImage_imageView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_friend_finder_fragment1_my_code_fragment, container, false);

        qrImage_imageView = (ImageView)view.findViewById(R.id.friendFinder_qr_imageView);

        String text = "https://park-duck.tistory.com";

        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try{
            BitMatrix bitMatrix = multiFormatWriter.encode(text, BarcodeFormat.QR_CODE,200,200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qrImage_imageView.setImageBitmap(bitmap);

        }catch (Exception e){
            Log.d("tag"+getClass().getName(), "error: "+e);
        }

        return view;
    }
}

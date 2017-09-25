package com.example.bluecanvas;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLogin :
                finish();
                break;
            case R.id.btnJoin :
                // 회원가입 화면 실행
                LoadJoinActivity();
                break;
        }
    }

    // 회원가입 화면 실행
    public void LoadJoinActivity() {
        // 인텐트 객체를 생성하고 회원가입 액티비티 클래스를 지정
        Intent intent = new Intent(getApplicationContext(), JoinActivity.class);
        // 인트로 액티비티 실행
        startActivity(intent);
    }

}

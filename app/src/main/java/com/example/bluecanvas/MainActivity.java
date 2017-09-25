package com.example.bluecanvas;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    final int REQ_CODE_SELECT_IMAGE = 1432;
    final int REQ_CODE_ACTIVITY_INTRO = 1433;
    final int REQ_CODE_ACTIVITY_LOGIN = 1434;
    final String mServerPath = "http://topsan.pe.hu/tizen/";
    final String mJsonFile = "file_list.json";
    //final String mFtpAddr = "31.170.165.170";     // Old Addres
    final String mFtpAddr = "31.220.110.139";       // 2016.11.04 에 바뀐 주소
    final String mFtpId = "u833683917";
    final String mFtpPw = "qwe123";
    final String mFtpDataFolder = "tizen";

    ListView mListImage = null;
    ArrayList<MyItem> mArImage;
    int mImageDownloadIndex = 0;            // 이미지 다운로드 인덱스 번호
    Bitmap mBmp = null;
    boolean userPermissionOk = false;       // 사용자 권한 승인 여부

    // ListView 의 항목 데이터 클래스 정의
    public class MyItem {
        View mLayoutItem = null;
        CheckBox check = null;
        String mName = null;
        String mImagePath = null;
        String mType = null;
        int mSize = 0;

        MyItem(String strName) {
            mName = strName;
        }

        // 생성자 함수에서 멤버변수 초기화
        MyItem(String strName, String strType, int nSize) {
            mName = strName;
            mType = strType;
            mSize = nSize;
        }
        // 생성자 함수에서 멤버변수 초기화
        MyItem(String strName, String strImagePath, String strType, int nSize) {
            mName = strName;
            mImagePath = strImagePath;
            mType = strType;
            mSize = nSize;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ListView 위젯에 초기화
        initListView();

        // 인트로 화면 실행
        LoadIntroActivity();
    }

    // 인트로 화면 실행
    public void LoadIntroActivity() {
        // 인텐트 객체를 생성하고 인트로 액티비티 클래스를 지정
        Intent intent = new Intent(getApplicationContext(), IntroActivity.class);
        // 인텐트에 배경 이미지 파일을 지정
        intent.putExtra("BackImage", R.drawable.intro_bg);
        // 인트로 액티비티 실행
        startActivityForResult(intent, REQ_CODE_ACTIVITY_INTRO);
    }

    // 로그인 화면 실행
    public void LoginIntroActivity() {
        // 인텐트 객체를 생성하고 로그인 액티비티 클래스를 지정
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        // 인트로 액티비티 실행
        startActivityForResult(intent, REQ_CODE_ACTIVITY_LOGIN);
    }

    // 메모리 Read/Write 권한을 체크
    private boolean checkUserPermission() {
        // 안드로이드 마시멜로우 이후 버전이라면 사용자에게 Permission 을 획득한다
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // 사용자가 Permission 을 부여하지 않았다면 권한을 요청하는 팝업창을 표시
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED){
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    Toast.makeText(this, "No Permission to use the Read&Write Storage", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE}, 111);
                return false;
            }
        }
        userPermissionOk = true;
        return true;
    }

    // 사용자 권한 요청 결과 이벤트 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode){
            // 메모리 Read/Write 권한 요청 결과 일때
            case 111:
                // 사용자가 권한 부여를 거절 했을때
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "Cannot run application because Storage permission have not been granted", Toast.LENGTH_SHORT).show();
                }
                // 사용자가 권한을 부여했을때
                else {
                    Toast.makeText(this, "Storage permission is granted!", Toast.LENGTH_SHORT);
                    userPermissionOk = true;
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    // ListView 위젯에 초기화
    public void initListView() {
        // ArrayList 배열 객체를 생성
        mArImage = new ArrayList<MyItem>();
        // 항목에 해당하는 객체를 4개 생성해서 ArrayList 배열에 저장
        MyItem mi;
        mi = new MyItem("New York");
        mArImage.add(mi);
        mi = new MyItem("Pary");
        mArImage.add(mi);
        mi = new MyItem("Lundon");
        mArImage.add(mi);
        mi = new MyItem("Seoul");
        mArImage.add(mi);

        // 어댑터 객체를 생성해서 ListView 에 지정
        MyListAdapter MyAdapter =
                new MyListAdapter(this, R.layout.custom_list_item, mArImage);

        // ListView 위젯의 핸들을 구해서 멤버변수에 저장
        mListImage = (ListView)findViewById(R.id.listImage);
        // ListView 의 어댑터를 지정
        mListImage.setAdapter(MyAdapter);
        // ListView 위젯의 이벤트 리스너를 지정
        mListImage.setOnItemClickListener(mItemClickListener);

    }

    // ListView 항목 선택 이벤트 리스너를 생성하고 이벤트 함수를 재정의
    AdapterView.OnItemClickListener mItemClickListener =
            new AdapterView.OnItemClickListener() {

                // ListView 항목을 선택하면 선택된 항목의 정보를 TextView 에 표시
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            // ArrayList 에서 선택 항목의 데이터를 구한다
            MyItem myItem = mArImage.get(position);
            Toast.makeText(getApplicationContext(), "Select : " + position + " - " + myItem.mName, Toast.LENGTH_SHORT).show();
            //mTextMessage.setText("Select : " + position + " - " + strItem);
        }
    };

    // ListView 와 데이터 배열을 연결해주는 커스텀 어댑터 클래스를 정의
    public class MyListAdapter extends BaseAdapter {
        Context mMaincon;
        LayoutInflater mInflater;
        ArrayList<MyItem> mArSrc;
        int layout;

        // 생성자 함수에서 멤버변수 초기화
        MyListAdapter(Context context, int alayout, ArrayList<MyItem> aarSrc) {
            mMaincon = context;
            mInflater = (LayoutInflater)context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            mArSrc = aarSrc;
            layout = alayout;
        }

        // 항목 개수를 반환
        public int getCount() {
            return mArSrc.size();
        }

        // 특정 항목의 텍스트 데이터를 반환
        public String getItem(int position) {
            return mArSrc.get(position).mName;
        }

        // 특정 항목의 ID 를 반환
        public long getItemId(int position) {
            return position;
        }

        // ListView 아이템 내부 각각의 엘리먼트에 데이터를 입력
        public View getView(int position, View convertView, ViewGroup parent) {
            //final int pos = position;
            /*if (convertView == null) {
                convertView = mInflater.inflate(layout, parent, false);
            }*/
            MyItem mi = mArImage.get(position);
            // 항목 Layout 이 아직 생성되지 않았다면 생성한다
            if( mi.mLayoutItem == null ) {
                mi.mLayoutItem = mInflater.inflate(layout, null);
                mi.check = (CheckBox)mi.mLayoutItem.findViewById(R.id.checkbox);
            }

            // ImageView 에 리소스 이미지 지정
            ImageView img = (ImageView)mi.mLayoutItem.findViewById(R.id.img);
            if( mArSrc.get(position).mImagePath == null || mArSrc.get(position).mImagePath.length() < 4 )
                img.setImageResource(R.drawable.icon_home);
            else {
                //img.setImageResource(R.drawable.icon_message);
                //Uri uri = Uri.parse(mArSrc.get(position).mImagePath);
                //img.setImageURI( uri );
                /*try {
                    //URL url = new URL(mArSrc.get(position).mImagePath);
                    URL url = new URL("http://topsan.pe.hu/tizen/bicky03.jpg");
                    Bitmap bitmap = BitmapFactory.decodeStream(url.openStream());
                    img.setImageBitmap(bitmap);
                } catch (MalformedURLException e) {
                }
                catch (IOException e) {
                }*/
                File picture = new File( mArSrc.get(position).mImagePath );
                if (picture.exists()) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap myBitmap = BitmapFactory.decodeFile(picture.getAbsolutePath(), options);
                    img.setImageBitmap(myBitmap);
                }
            }

            // 1번째 TextView 에 데이터 입력
            TextView textView1 =
                    (TextView)mi.mLayoutItem.findViewById(R.id.text1);
            textView1.setText(mArSrc.get(position).mName);

            //return convertView;
            return mi.mLayoutItem;
        }
    };

    // 버튼 이벤트 함수
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnUpload :
                onBtnUpload();
                break;
            case R.id.btnDel :
                onBtnDel();
                break;
        }
    }

    // 서버에서 이미지 목록을 가져온다
    public void getImageListServer() {
        // 네트워크 접속 여부 반환
        if( Util.isNetConnect(this) == false ) {
            Toast.makeText(getApplicationContext(), "Network can not use!", Toast.LENGTH_SHORT).show();
            return;
        }

        String addr = mServerPath + mJsonFile;
        // 스레드에 주소를 전달해서 데이터 요청 시작
        new HttpReqTask().execute(addr);
    }

    // 스레드 클래스 재정의
    private class HttpReqTask extends AsyncTask<String,String,String> {
        @Override // 스레드 동작 수행 함수
        protected String doInBackground(String... arg) {
            String response = "";
            // 파라미터가 1개라면 웹사이트에 데이터 요청
            if( arg.length == 1 ) {
                return (String)Util.getHttpConnResult(arg[0]);
            }
            return response;
        }

        // 스레드 동작이 완료되면 결과를 화면에 표시
        protected void onPostExecute(String result) {
            //Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
            //mTextMessage.setText(result);
            // 이미지 목록 JSON 코드를 파싱
            parseJson_ImageList(result);
        }
    }

    // 이미지 목록 JSON 코드를 파싱
    public void parseJson_ImageList(String strJson) {
        try {
            // 이미지 정보 목록을 모두 삭제
            mArImage.clear();

            // JSON 구문을 파싱해서 JSONArray 객체를 생성
            JSONArray jAr = new JSONArray(strJson);
            for(int i=0; i < jAr.length(); i++) {
                // 개별 객체를 하나씩 추출
                JSONObject joImage = jAr.getJSONObject(i);
                // 객체에서 데이터를 추출
                String strName = joImage.getString("name");
                String strType = joImage.getString("type");
                int nSize = joImage.getInt("size");

                MyItem mi = new MyItem(strName, "", strType, nSize);
                mArImage.add(mi);
            }
        } catch (JSONException e) {
            Log.d("tag", "Parse Error");
        }

        // ListView 갱신
        MyListAdapter MyAdapter = (MyListAdapter)mListImage.getAdapter();
        MyAdapter.notifyDataSetChanged();

        if( mArImage.size() > 0 ) {
            mImageDownloadIndex = 0;            // 이미지 다운로드 인덱스 번호
            // 서버에서 개별 이미지 다운로드 수행
            startDownloadImage();
        }
    }

    // 서버에서 이미지 다운로드를 수행하는 스레드
    private class ImageDownloadTask extends AsyncTask<String,String,String> {
        @Override // 스레드 주업무를 수행하는 함수
        protected String doInBackground(String... arg) {
            boolean result = false;
            if( arg.length == 1 ) {
                // 서버에서 전달 받은 데이터를 Bitmap 이미지에 저장
                mBmp = Util.loadWebImage(arg[0]);
                if( mBmp != null )
                    result = true;
            }
            else
                // 서버에서 다운로드 한 데이터를 파일로 저장
                result = Util.downloadFile(getApplicationContext(), arg[0], arg[1]);

            if( result )
                return "True";
            return "";
        }

        // 스레드의 업무가 끝났을 때 결과를 처리하는 함수
        protected void onPostExecute(String result) {
            if( result.length() > 0 ) {
                // 다운받은 이미지의 전체 경로를 이미지 정보 배열에 저장
                MyItem mi = mArImage.get( mImageDownloadIndex );
                mi.mImagePath = Util.getLocalFilePath(getApplicationContext(), mi.mName);
                // ListView 갱신
                MyListAdapter MyAdapter = (MyListAdapter)mListImage.getAdapter();
                MyAdapter.notifyDataSetChanged();
            }

            mImageDownloadIndex ++;
            // 서버에서 개별 이미지 다운로드 수행
            startDownloadImage();
        }
    }

    // 서버에서 개별 이미지 다운로드 수행
    public void startDownloadImage() {
        // 이번에 다운로드할 이미지 인덱스 번호가 범위를 벗어나면 함수 탈출
        if( mImageDownloadIndex < 0 || mImageDownloadIndex >= mArImage.size() )
            return;

        MyItem mi = mArImage.get( mImageDownloadIndex );
        /*// 로컬 데이터 폴더 파일의 전체 경로를 반환
        String localImagePath = Util.getLocalFilePath(this, mi.mName);
        File file = new File(localImagePath);
        if( file.exists() ) {*/
        // 이미지가 이미 존재한다면
        if( Util.isFileExistLocal( this, mi.mName ) ) {
            mImageDownloadIndex ++;
            //mi.mImagePath = localImagePath;
            mi.mImagePath = Util.getLocalFilePath(this, mi.mName);

            // ListView 갱신
            MyListAdapter MyAdapter = (MyListAdapter)mListImage.getAdapter();
            MyAdapter.notifyDataSetChanged();

            // 서버에서 개별 이미지 다운로드 수행
            startDownloadImage();
            return;
        }

        // 이미지가 로컬에 존재하지 않는다면
        String strImageUrl = mServerPath + mi.mName;
        // 서버에서 이미지 다운로드를 수행하는 스레드
        new ImageDownloadTask().execute(strImageUrl, mi.mName);
    }

    // 'Upload' 버튼 이벤트 함수
    public void onBtnUpload() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQ_CODE_SELECT_IMAGE);
    }

    // 파일을 FTP 서버에 업로드
    public boolean upload2Ftp(String fileName, boolean delFile) {
        try {
            FTPClient mFTP = new FTPClient();

            //mFTP.connect("31.170.165.170", 21);  // ftp로 접속mFtpAddr
            mFTP.connect(mFtpAddr, 21);  // ftp로 접속
            //mFTP.login("u833683917", "qwe123"); // ftp 로그인 계정/비번 mFtpId
            mFTP.login(mFtpId, mFtpPw); // ftp 로그인 계정/비번
            mFTP.setFileType(FTP.BINARY_FILE_TYPE); // 바이너리 파일
            mFTP.setBufferSize(1024 * 1024); // 버퍼 사이즈
            mFTP.enterLocalPassiveMode(); //패시브 모드로 접속

            // 업로드 경로 수정 (선택 사항 )
            mFTP.mkd(mFtpDataFolder); // public아래로 files 디렉토리를 만든다
            mFTP.cwd(mFtpDataFolder); // public/files 로 이동 (이 디렉토리로 업로드가 진행)

            //String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
            //String filePaht = sdPath + fileName;
            File file = new File(fileName);
            if (file.isFile()) {
                FileInputStream ifile = new FileInputStream(file);
                if( delFile )
                    mFTP.deleteFile(file.getName());
                mFTP.rest(file.getName());  // ftp에 해당 파일이있다면 이어쓰기
                mFTP.appendFile(file.getName(), ifile); // ftp 해당 파일이 없다면 새로쓰기
            }

            mFTP.disconnect(); // ftp disconnect

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Toast.makeText(getBaseContext(), "resultCode : "+resultCode,Toast.LENGTH_SHORT).show();

        // 이미지 선택 결과 이벤트 수신 일때
        if(requestCode == REQ_CODE_SELECT_IMAGE) {
            if(resultCode== Activity.RESULT_OK) {
                try {
                    //Uri에서 이미지 이름을 얻어온다.
                    String name_Str = getImageNameToUri(data.getData());
                    Toast.makeText(this, "Sel Image: " + name_Str, Toast.LENGTH_SHORT).show();
                    //mTextView1.setText(name_Str);
                    // 이미지 파일을 FTP 서버에 업로드
                    new FileUploadTask().execute(name_Str);
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        // 인트로 화면 종료 이벤트 일때
        else if( requestCode == REQ_CODE_ACTIVITY_INTRO ) {
            //Toast.makeText(this, "Intro Activity closed", Toast.LENGTH_SHORT).show();
            // 로그인 화면 실행
            LoginIntroActivity();
        }
        // 로그인 화면 종료 이벤트 일때
        else if( requestCode == REQ_CODE_ACTIVITY_LOGIN ) {
            // 서버에서 이미지 목록을 가져온다
            getImageListServer();

            // 메모리 Read/Write 권한을 체크
            checkUserPermission();
        }
    }

    // Uri 에서 파일명을 구해서 반환
    public String getImageNameToUri(Uri data)
    {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(data, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();

        String imgPath = cursor.getString(column_index);
        return imgPath;
    }

    // 이미지 정보 배열의 데이터를 JSON 형식으로 반환
    public String imageArray2Json() {
        String strJson = "[", strItem = "";
        int listCount = mArImage.size();
        for(int i=0; i < listCount; i++) {
            MyItem mi = mArImage.get(i);
            strItem = "{\"name\":\"" + mi.mName + "\", \"type\":\"" + mi.mType + "\", \"size\":" + mi.mSize + "}";
            if( i > 0 )
                strJson += "\n,";
            strJson += strItem;
        }
        strJson += "]";
        return strJson;
    }

    // 이미지 목록에 항목 정보 추가
    public void addImageInfo2Array(String filePath) {
        // 전체 경로에서 파일명 추출
        String strName = filePath;
        int pos = filePath.lastIndexOf("/");
        if( pos > 0 )
            strName = filePath.substring(pos + 1);

        // 파일명에서 확장자명 추출
        String strType = "non";
        String lowerName = strName.toLowerCase();
        pos = lowerName.lastIndexOf(".");
        if( pos > 0 )
            strType = lowerName.substring(pos + 1);

        // 파일 크기를 구한다
        File file = new File( filePath );
        int nSize = (int)file.length();

        // 이미지 정보 객체 생성
        MyItem mi = new MyItem(strName, strType, nSize);
        // 이미지 정보 객체를 배열에 추가
        mArImage.add(mi);
    }

    // JSON 코드를 로컬 공간에 저장
    public boolean saveJson2Local(String strJson) {
        // 로컬 데이터 폴더 파일의 전체 경로를 반환
        //String localImagePath = Util.getLocalFilePath(this, mJsonFile);
        // /files 폴더에 텍스트 파일 저장
        boolean bSaved = Util.WriteTextFile(this, mJsonFile, strJson);
        return bSaved;
    }

    // 파일 업로드 스레드 클래스 재정의
    private class FileUploadTask extends AsyncTask<String,String,String> {
        @Override // 스레드 동작 수행 함수
        protected String doInBackground(String... arg) {
            boolean result = false;
            // 파일을 FTP 서버에 업로드
            if( arg.length == 1 )       // 파라미터가 1개 일때는 파일 이어서 쓰기
                result = upload2Ftp(arg[0], false);
            else                        // 파라미터가 2개 일때는 지우고 새로 쓰기
                result = upload2Ftp(arg[0], true);
            String strResult = result ? arg[0] : "";
            return strResult;
        }

        // 스레드 동작이 완료되면 결과를 화면에 표시
        protected void onPostExecute(String result) {
            // 파일 업로드에 실패했을때
            if( result == null || result.length() < 1 ) {
                Toast.makeText(getApplicationContext(), "File upload failed. Please try again. " + result, Toast.LENGTH_SHORT).show();
                return;
            }

            // JSON 파일 업로드 했을때
            if( result.endsWith("json") ) {
                mImageDownloadIndex = mArImage.size() - 1;            // 이미지 다운로드 인덱스 번호
                // 서버에서 개별 이미지 다운로드 수행
                startDownloadImage();
                return;
            }
            // 파일 업로드에 성공했을때 - 이미지 목록을 항목 정보 추가 & JSON 파일을 생성 & FTP 서버에 업로드
            onFileUploadSucceeded( result );
        }
    }

    // 파일 업로드에 성공했을때 - 이미지 목록을 항목 정보 추가 & JSON 파일을 생성 & FTP 서버에 업로드
    public void onFileUploadSucceeded(String filePath) {
        // 이미지 목록에 항목 정보 추가
        addImageInfo2Array(filePath);

        // 배열에 저장된 이미지 목록 정보를 FTP 서버에 JSON 파일로 저장
        localArray2ServerJson();
    }

    // 배열에 저장된 이미지 목록 정보를 FTP 서버에 JSON 파일로 저장
    public void localArray2ServerJson() {
        // 이미지 정보 배열의 데이터를 JSON 형식으로 반환
        String strJson = imageArray2Json();
        // JSON 코드를 로컬 공간에 저장
        boolean bSaved = saveJson2Local(strJson);
        if( bSaved == false ) {
            Toast.makeText(getApplicationContext(), "JSON file save failed. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 로컬 데이터 폴더 파일의 전체 경로를 반환
        String localImagePath = Util.getLocalFilePath(this, mJsonFile);
        // JSON 파일을 FTP 서버에 업로드
        new FileUploadTask().execute(localImagePath, "fileDel");
    }

    // 'Del' 버튼 이벤트 함수
    public void onBtnDel() {
        // 이미지 배열에서 체크된 항목을 찾아서 삭제
        int listCount = mArImage.size();
        for(int i=listCount-1; i >= 0; i--) {
            MyItem mi = mArImage.get(i);
            if( mi.check == null || mi.check.isChecked() == false )
                continue;
            mArImage.remove(i);
        }

        // ListView 갱신
        MyListAdapter MyAdapter = (MyListAdapter)mListImage.getAdapter();
        MyAdapter.notifyDataSetChanged();

        // 배열에 저장된 이미지 목록 정보를 FTP 서버에 JSON 파일로 저장
        localArray2ServerJson();
    }

}

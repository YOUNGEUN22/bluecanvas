package com.example.bluecanvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by JDG-GRAM on 2016-11-03.
 */
public class Util {

    // /files 폴더에 텍스트 파일 저장
    public static boolean WriteTextFile(Context context, String strFileName, String strBuf) {
        try {
            // 파일을 오픈한다
            File file = context.getFileStreamPath(strFileName);
            // 파일 출력 스트림을 구한다
            FileOutputStream fos = new FileOutputStream(file);
            // 텍스트 인코딩 방식을 지정한다
            Writer out = new OutputStreamWriter(fos, "UTF-8");
            // 데이터를 파일에 저장
            out.write(strBuf);
            // 파일을 닫는다
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    // 로컬 데이터 폴더에 파일이 존재하는지 판단
    public static boolean isFileExistLocal(Context context, String fileName) {
        // 로컬 데이터 폴더 파일의 전체 경로를 반환
        String localImagePath = Util.getLocalFilePath(context, fileName);
        // 이미지가 이미 존재한다면
        File file = new File(localImagePath);
        return file.exists();
    }

    // 서버에서 전달 받은 데이터를 Bitmap 이미지에 저장
    public static Bitmap loadWebImage(String strUrl) {
        Bitmap bmp = null;
        try {
            // 스트림 데이터를 Bitmap 에 저장
            InputStream is = new URL(strUrl).openStream();
            bmp = BitmapFactory.decodeStream(is);
            is.close();
        } catch(Exception e) {
            Log.d("tag", "Image Stream error.");
            return null;
        }
        return bmp;
    }

    // HTTP 요청 결과 반환
    public static String getHttpConnResult(String strUrl) {
        String line, result = new String();

        try {
            // Http 클라이언트 생성
            URL url = new URL(strUrl);
            HttpURLConnection conn = (HttpURLConnection)
                    url.openConnection();
            // 접속 정보 설정
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // 접속 시작
            conn.connect();

            // 데이터 추출
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is));

            while((line = reader.readLine()) != null) {
                result += line + '\n';
                if( result.length() > 2000 ) break;
            }
            // 접속 종료
            reader.close();
            conn.disconnect();
        }
        catch(Exception e) {
            Log.d("tag", "HttpURLConnection error");
        }
        return result;
    }

    // 로컬 데이터 폴더 파일의 전체 경로를 반환
    public static String getLocalFilePath(Context context, String fileName) {
        String sdRootPath = Environment.getDataDirectory().getAbsolutePath();
        String filePath = sdRootPath + "/data/" + context.getPackageName() + "/files/" + fileName;
        return filePath;
    }

    // 네트워크 접속 여부 반환
    public static boolean isNetConnect(Context context) {
        try {
            // 네트워크 접속 관리자 핸들을 구한다
            ConnectivityManager conMgr = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            // 네트워크 정보를 구한다
            NetworkInfo netInfo = conMgr.getActiveNetworkInfo();

            // 네트워크 접속 여부를 구한다
            if (netInfo != null && netInfo.isConnected())
                return true;
        } catch (Exception e) {
            Log.d("tag", "Connection state error");
        }
        return false;
    }

    // 서버에서 다운로드 한 데이터를 파일로 저장
    public static boolean downloadFile(Context context, String strUrl, String fileName) {
        try {
            URL url = new URL(strUrl);
            // 서버와 접속하는 클라이언트 객체 생성
            HttpURLConnection conn = (HttpURLConnection)
                    url.openConnection();
            // 입력 스트림을 구한다
            InputStream is = conn.getInputStream();
            // 파일 저장 스트림을 생성
            FileOutputStream fos = context.openFileOutput(fileName, 0);

            // 입력 스트림을 파일로 저장
            byte[] buf = new byte[1024];
            int count;
            while( (count = is.read(buf)) > 0 ) {
                fos.write(buf, 0, count);
            }
            // 접속 해제
            conn.disconnect();
            // 파일을 닫는다
            fos.close();
        } catch (Exception e) {
            Log.d("tag", "Image download error.");
            return false;
        }
        return true;
    }

}

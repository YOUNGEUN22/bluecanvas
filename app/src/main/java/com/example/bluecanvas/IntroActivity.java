/* 만든이 : 정동근 (Jung, Dong-Geun)   */
/*    Mail : topofsan@naver.com        */

package com.example.bluecanvas;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.os.*;
import android.view.*;

public class IntroActivity extends Activity {
	final int TIMER_INTERVER = 40;
	final int TIMER_COUNT_MAX = 20;
	int mTimerIndex = 0;
	
	int mScreenWidth = 0, mScreenHeight = 0;
	float mFontSize = 20;
	Bitmap mBmpBack = null;
	Rect mRtCanvas = new Rect(0, 0, 10, 10);
    ViewCanvas mViewCanvas = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		// 타이틀바 감춘다
		requestWindowFeature( Window.FEATURE_NO_TITLE );

		// 캔버스 뷰를 생성
		mViewCanvas = new ViewCanvas(this);
        setContentView(mViewCanvas);
        
        Intent intent = getIntent();
        int id = intent.getIntExtra("BackImage", 0);
        if( id > 0 )
        	// 이미지 파일을 로딩해서 Bitmap 으로 반환하는 함수
        	mBmpBack = loadImage(id);
        
        // 종료 타이머
        initialize();
    }
    
    private void initialize() {
    	timerFadeIn.sendEmptyMessageDelayed(0, TIMER_INTERVER);
    }
    
	Handler timerFadeIn = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//finish();
			mTimerIndex ++;
			
			if( mTimerIndex < TIMER_COUNT_MAX) {
				mViewCanvas.invalidate();
				timerFadeIn.sendEmptyMessageDelayed(0, TIMER_INTERVER);
			}
			else {
				timerFreeze.sendEmptyMessageDelayed(0, 1500);
			}
		}
    };
    
    Handler timerFreeze = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			timerFadeOut.sendEmptyMessageDelayed(0, TIMER_INTERVER);
		}
    };

	Handler timerFadeOut = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mTimerIndex --;
			
			if( mTimerIndex > 0) {
				mViewCanvas.invalidate();
				timerFadeOut.sendEmptyMessageDelayed(0, TIMER_INTERVER);
			}
			else {
				finish();    // 액티비티 종료
			}
		}
    };
    
	// 이미지 파일을 로딩해서 Bitmap 으로 반환하는 함수
	public Bitmap loadImage(int resImage) {
		Resources res = getResources();
		Bitmap bitmap = BitmapFactory.decodeResource(res, resImage);

		return bitmap;
	}

	// Activity 가 화면에 표시되는 이벤트
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		
		WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
		Display dsp = wm.getDefaultDisplay();

		// 화면 크기를 구한다
		mScreenWidth = dsp.getWidth();
		mScreenHeight = dsp.getHeight();
		mRtCanvas.right = mScreenWidth;
		mRtCanvas.bottom = mScreenHeight;

		// 폰트 크기를 계산
		mFontSize = 21.f / 480.f * (float)mScreenWidth;
	}

    protected class ViewCanvas extends View {
    	public ViewCanvas(Context context) {
    		super(context);
    	}

    	public void onDraw(Canvas canvas) {
    		super.onDraw(canvas);
    		if( mScreenWidth < 1 )
    			return;

    		if( mBmpBack != null )
    			// 이미지를 원본 비율대로 지정된 영역에 최대한 크게 출력
    			drawImage(canvas, mBmpBack, mRtCanvas);
    		
    		int alpha = (int)(255.f * (float)(TIMER_COUNT_MAX - mTimerIndex) / (float)TIMER_COUNT_MAX);
    		int color = Color.argb(alpha, 255, 255, 255); //저장
    		Paint paint = new Paint();
    		paint.setStyle(Style.FILL); // 채우기 속성 지정
    		paint.setColor(color); // 채우기 컬러 지정
    		canvas.drawRect(mRtCanvas, paint);
    	}
    	
    	// 이미지를 원본 비율대로 지정된 영역에 최대한 크게 출력
    	public void drawImage(Canvas canvas, Bitmap bitmap, Rect rtImage) {
    		// 이미지의 크기를 구한다
    		int imageWidth = 0, imageHeight = 0;
    		imageWidth = bitmap.getWidth();
    		imageHeight = bitmap.getHeight();
    		Rect rtSrc = new Rect(0, 0, imageWidth, imageHeight);
    		
    		// 이미지와 화면 영역의 가로/세로 비율을 구한다
    		float imageRatio = 0.f, screenRatio = 0.f;
    		imageRatio = (float)imageHeight / (float)imageWidth;
    		screenRatio = (float)rtImage.height() / (float)rtImage.width();
    		
    		// 이미지가 화면보다 수직으로 더 길다면
    		if( imageRatio > screenRatio ) {
    			rtSrc.bottom = (int)((float)imageWidth * screenRatio);
    		}
    		// 이미지가 화면보다 수평으로 더 길다면
    		else {
    			imageWidth = (int)((float)imageHeight / screenRatio);
    			rtSrc.left = (rtSrc.width() - imageWidth) / 2;
    			rtSrc.right = rtSrc.left + imageWidth;
    		}
    		
    		// 이미지를 캔버스에 그린다
    		canvas.drawBitmap(bitmap, rtSrc, rtImage, null);
    	}
    }
}



    /**
    OsciPrime an Open Source Android Oscilloscope
    Copyright (C) 2012  Manuel Di Cerbo, Nexus-Computing GmbH Switzerland
    Copyright (C) 2012  Andreas Rudolf, Nexus-Computing GmbH Switzerland

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    */

package ch.nexuscomputing.android.osciprimeics;

import java.util.HashMap;
import java.util.Map;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

public class OsciPrimeOverlayHelpView extends RelativeLayout{

	private View mRootPane;
	
	private ScrollView mBarL;
	private ScrollView mBarR;
	private ViewGroup mHolder;
	private ViewGroup mButtonHolder;
	private ImageButton mButtonLB;
	private ImageButton mButtonRB;
	
	private final Typeface tf;
	
	enum Mode{
		STEP_1,
		STEP_2
	}
	
	private Mode mMode = Mode.STEP_1;
	
	
	public void reset(){
		mMode = Mode.STEP_1;
	}
	private Button mBtGotIt;
	private static final HashMap<Integer, String> sOverlayMapLeft = new HashMap<Integer, String>();
	private static final HashMap<Integer, String> sOverlayMapRight = new HashMap<Integer, String>();
	
	private static float DENSITY;
	
	static{
		sOverlayMapLeft.put(R.id.config, "Preferences");
		sOverlayMapLeft.put(R.id.btTriggerSettings, "Trigger Settings");
		sOverlayMapLeft.put(R.id.btOffsetOverlay, "Offset Overlay");
		sOverlayMapLeft.put(R.id.btMeasureOverlay, "Measurement Overlay");
		sOverlayMapLeft.put(R.id.btDebugOverlay, "Debug Overlay");
		sOverlayMapLeft.put(R.id.btSource, "Input Selection");
		sOverlayMapLeft.put(R.id.btCalibrate, "Calibration (Zero Signal)");
	}
	
	static{
		sOverlayMapRight.put(R.id.btRunStop, "Run/Stop");
		sOverlayMapRight.put(R.id.btSingleShot, "Single Shot");
		sOverlayMapRight.put(R.id.btInterleaveUp, "Interleave Up");
		sOverlayMapRight.put(R.id.btInterleaveDown, "Interleave Down");
		sOverlayMapRight.put(R.id.btScreenshot, "Export Screenshot");
		sOverlayMapRight.put(R.id.btHelp, "Help Overlay");
		sOverlayMapRight.put(R.id.btNews, "OsciPrime News");
	}

	private final OsciPrimeApplication mApplication;
	public OsciPrimeOverlayHelpView(Context context, OsciPrimeApplication app) {
		super(context);
		DENSITY = context.getResources().getDisplayMetrics().density;
		mBtGotIt = new Button(context);
		mBtGotIt.setText("Got it!");
		mBtGotIt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if(mMode == Mode.STEP_1){
					mMode = Mode.STEP_2;
					mRootPane.findViewById(R.id.panL).setVisibility(View.INVISIBLE);
					mRootPane.findViewById(R.id.panR).setVisibility(View.INVISIBLE);
					mRootPane.findViewById(R.id.panD).setVisibility(View.VISIBLE);
					
					mRootPane.findViewById(R.id.btUB).setVisibility(View.INVISIBLE);
					mRootPane.findViewById(R.id.btU).setVisibility(View.INVISIBLE);
					
					invalidate();
				}else{
					mMode = Mode.STEP_1;
					setVisibility(View.GONE);
					mRootPane.findViewById(R.id.panL).setVisibility(View.VISIBLE);
					mRootPane.findViewById(R.id.panR).setVisibility(View.VISIBLE);
					mRootPane.findViewById(R.id.panD).setVisibility(View.VISIBLE);
					
					mRootPane.findViewById(R.id.btRB).setVisibility(View.VISIBLE);
					mRootPane.findViewById(R.id.btLB).setVisibility(View.VISIBLE);
					mRootPane.findViewById(R.id.btUB).setVisibility(View.VISIBLE);
				}
			} 
		});
		//mBtGotIt.setBackgroundColor(Color.parseColor("#003399"));
		mBtGotIt.setTextColor(Color.WHITE);
		int unitsize = (int) (30);
		mBtGotIt.setTextSize(unitsize);
		mBtGotIt.setPadding(unitsize, unitsize, unitsize,unitsize);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
		mBtGotIt.setLayoutParams(params);
		addView(mBtGotIt);
		
		tf = Typeface.createFromAsset(context.getAssets(),"Chewy.ttf");;
		mApplication = app;
		setWillNotDraw(false);
		setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				invalidate();
				if(mHolder != null){
					mHolder.dispatchTouchEvent(event);
				}
				
				if(mButtonHolder != null){
					mButtonHolder.dispatchTouchEvent(event);
				}
				return true;
			} 
		});
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		drawFrame(canvas);
	};
	
	private void drawFrame(Canvas canvas){
		if(mRootPane == null)
			return;
		

		Paint paint = new Paint();
		paint.setStyle(Style.FILL);
		paint.setColor(mApplication.pColorBackground);
		paint.setTypeface(tf);
		paint.setAntiAlias(true);
		
		
		int x0 = mBarL.getWidth(); 
		int x1 = (int) mBarR.getLeft();
		if(mMode == Mode.STEP_1){
			x0 = mBarL.getWidth(); 
			x1 = (int) mBarR.getLeft();
		}else{
			x0 = 0; 
			x1 = getWidth();
		}

		
		canvas.drawRect(x0, 0, x1, getHeight(), paint);
		
		paint.setStyle(Style.STROKE);
		paint.setAlpha(255);
		paint.setTextSize(DENSITY*20);
		paint.setTextAlign(Align.LEFT);
		paint.setColor(mApplication.pColorMeasure);
		 
		
		if(mMode == Mode.STEP_1){
			for(Map.Entry<Integer, String> m: sOverlayMapLeft.entrySet()){
				View v = mRootPane.findViewById(m.getKey());
				if(v != null){
					float y = v.getTop()-mBarL.getScrollY();
					canvas.drawText(m.getValue(), mButtonLB.getLeft()+(mButtonLB.getWidth()/2), (int)(y+v.getHeight()*.66), paint);
				}
			}
			
			paint.setTextAlign(Align.RIGHT);
			for(Map.Entry<Integer, String> m: sOverlayMapRight.entrySet()){
				View v = mRootPane.findViewById(m.getKey());
				if(v != null){
					float y = v.getTop()-mBarR.getScrollY();
					canvas.drawText(m.getValue(), mButtonRB.getLeft()+(mButtonRB.getWidth()/2), (int)(y+v.getHeight()*.66), paint);
				}
			}
			
		}else if(mMode == Mode.STEP_2){
			View btdown = mRootPane.findViewById(R.id.btUB);
			paint.setTextAlign(Align.RIGHT);
			canvas.drawText("CH 1 Attenuation", btdown.getLeft()-60, btdown.getBottom()-btdown.getHeight()/2, paint);
			paint.setTextAlign(Align.LEFT);
			canvas.drawText("CH 2 Attenuation", btdown.getRight()+60, btdown.getBottom()-btdown.getHeight()/2, paint);
			paint.setTextAlign(Align.CENTER);
			canvas.drawText("Probe Tuning", btdown.getLeft()+btdown.getWidth()/2, btdown.getBottom()-btdown.getHeight()/2, paint);
		}
		
		paint.setColor(mApplication.pInterfaceColor);
		paint.setStyle(Style.FILL);
		canvas.drawRect(mBtGotIt.getLeft(), mBtGotIt.getTop(), mBtGotIt.getRight(), mBtGotIt.getBottom(), paint);
		
	}
	
	public void setRootPane(View rootPane){
		mRootPane = rootPane;
		mBarL = (ScrollView)mRootPane.findViewById(R.id.panL);
		mBarR = (ScrollView)mRootPane.findViewById(R.id.panR);
		mHolder = (ViewGroup) mRootPane.findViewById(R.id.holder);
		mButtonHolder = (ViewGroup) mRootPane.findViewById(R.id.buttonHolder);
		mButtonLB = (ImageButton) mRootPane.findViewById(R.id.btLB);
		mButtonRB = (ImageButton) mRootPane.findViewById(R.id.btRB);
	}

}


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

package ch.nexuscomputing.android.osciprimeics.draw;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path.Direction;
import android.graphics.Path;
import android.graphics.RectF;
import ch.nexuscomputing.android.osciprimeics.OsciPrimeApplication;

public class HandleDrawable {

	public static interface ICallback {
		public void onUpdated(HandleDrawable drawable);
	}

	public boolean isFocused = false;
	public boolean isFocusable = true;
	public final int layerFlags;
	public float x, y, touchX, touchY;
	protected float offX, offY;
	protected boolean lockX, lockY;
	protected final Path mPath = new Path();
	protected final Paint mPaint = new Paint();
	public final RectF box = new RectF();
	protected final ICallback mCallback;
	protected final OsciPrimeApplication mApplication;
	protected final int mColor;
	public final int mType;
	
	public static final int COLOR_MEAS = 0;
	public static final int COLOR_CH1 = 1;
	public static final int COLOR_CH2 = 2;
	
	public static final int TYPE_GENERIC = 0;
	public static final int TYPE_HANDLE_CH1 = 1;
	public static final int TYPE_HANDLE_CH2 = 2;
	public static final int TYPE_HANDLE_TRIGGER_CH1 = 3;
	public static final int TYPE_HANDLE_TRIGGER_CH2 = 4;
	public static final int TYPE_HANDLE_WINDOW = 5;

	public HandleDrawable(Path src, ICallback callback, float x, float y,
			float offX, float offY, float w, float h, OsciPrimeApplication app,
			int layerFlags, boolean lockX, boolean lockY, int color, int type) {
		this.layerFlags = layerFlags;
		mType = type;
		mColor = color;
		mApplication = app;
		mCallback = callback;
		mPaint.setStyle(Style.FILL);
		mPaint.setColor(mApplication.pColorMeasure);
		mPaint.setAntiAlias(true);
		mPath.set(src);
		this.x = x;
		this.y = y;
		this.offX = offX;
		this.offY = offY;
		this.lockX = lockX;
		this.lockY = lockY;
		box.left = offX;
		box.top = offY;
		box.right = w + offX;
		box.bottom = h + offY;
	}

	public void update() {
		mCallback.onUpdated(this);
	}

	public void draw(Canvas canvas) {
		if ((layerFlags & mApplication.pActiveOverlay) == 0)
			return;
		if(mType == TYPE_HANDLE_CH1)
			if(!mApplication.pShowCh1)
				return;
		if(mType == TYPE_HANDLE_CH2)
			if(!mApplication.pShowCh2)
				return;
		if(mType == TYPE_HANDLE_TRIGGER_CH1)
			if(mApplication.pTriggerChannel == OsciPrimeApplication.CH2)
				return;
		if(mType == TYPE_HANDLE_TRIGGER_CH2)
			if(mApplication.pTriggerChannel == OsciPrimeApplication.CH1)
				return;
		
		canvas.save();
		canvas.translate(x + offX, y + offY);
		if(mColor == COLOR_CH1)
			mPaint.setColor(mApplication.pColorCh1);
		if(mColor == COLOR_CH2)
			mPaint.setColor(mApplication.pColorCh2);
		if(mColor == COLOR_MEAS)
			mPaint.setColor(mApplication.pColorMeasure);
		canvas.drawPath(mPath, mPaint);
		canvas.restore();
	}

	public void set(float currentX, float currentY) {
		if(!lockX)
			this.x = currentX - (box.right + box.left) / 2;
		
		if(!lockY)
			this.y = currentY - (box.bottom + box.top) / 2;
	}
}
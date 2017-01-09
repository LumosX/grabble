package eu.zerovector.grabble;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

// Lifted from http://stackoverflow.com/a/30873744
// Modified it a little bit to better serve my needs
class ProgressDrawable extends Drawable {
    private int numSegments = 6;
    private final int mForeground;
    private final int mBackground;
    private final Paint mPaint = new Paint();
    private final RectF mSegment = new RectF();

    public ProgressDrawable(int fgColor, int bgColor) {
        mForeground = fgColor;
        mBackground = bgColor;
    }

    public ProgressDrawable(int fgColor, int bgColor, int segments) {
        mForeground = fgColor;
        mBackground = bgColor;
        numSegments = segments;
    }

    public int getNumSegments() {
        return numSegments;
    }

    public void setNumSegments(int segments) {
        numSegments = segments;
    }

    @Override
    protected boolean onLevelChange(int level) {
        invalidateSelf();
        return true;
    }

    @Override
    public void draw(Canvas canvas) {
        float level = getLevel() / 10000f;
        Rect b = getBounds();
        float gapWidth = b.height() / 2f;
        float segmentWidth = (b.width() - (numSegments - 1) * gapWidth) / numSegments;
        mSegment.set(0, 0, segmentWidth, b.height());
        mPaint.setColor(mForeground);

        for (int i = 0; i < numSegments; i++) {
            float loLevel = i / (float) numSegments;
            float hiLevel = (i + 1) / (float) numSegments;
            if (loLevel <= level && level <= hiLevel) {
                float middle = mSegment.left + numSegments * segmentWidth * (level - loLevel);
                canvas.drawRect(mSegment.left, mSegment.top, middle, mSegment.bottom, mPaint);
                mPaint.setColor(mBackground);
                canvas.drawRect(middle, mSegment.top, mSegment.right, mSegment.bottom, mPaint);
            } else {
                canvas.drawRect(mSegment, mPaint);
            }
            mSegment.offset(mSegment.width() + gapWidth, 0);
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
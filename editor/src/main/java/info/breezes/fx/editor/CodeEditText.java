/*
 * Copyright 2015. Qiao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.breezes.fx.editor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;

import info.breezes.toolkit.log.Log;

/**
 * An editText widget with line numbers
 */
public class CodeEditText extends EditText {

    private boolean showLineNumber;
    private int lineNumberColor;
    private String fontFamily;

    private Rect mRect;
    private Paint lineNumberPaint;
    private int numberMargin;

    public CodeEditText(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public CodeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public CodeEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }


    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs != null && !isInEditMode()) {
            TypedArray ta;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ta = context.obtainStyledAttributes(attrs, R.styleable.CodeEditText, defStyleAttr, defStyleRes);
            } else {
                ta = context.obtainStyledAttributes(attrs, R.styleable.CodeEditText);
            }
            fontFamily = ta.getString(R.styleable.CodeEditText_fontFamily);
            if (!TextUtils.isEmpty(fontFamily)) {
                setTypeface(Typeface.createFromAsset(context.getAssets(), fontFamily));
            } else {
                setTypeface(Typeface.MONOSPACE);
            }
            showLineNumber = ta.getBoolean(R.styleable.CodeEditText_showLineNumber, true);
            lineNumberColor = ta.getColor(R.styleable.CodeEditText_numberColor, Color.BLACK);
            ta.recycle();
        }
        numberMargin = (int) (context.getResources().getDisplayMetrics().density * 3 + 0.5);
        mRect = new Rect();
        lineNumberPaint = new Paint();
        lineNumberPaint.setStyle(Paint.Style.STROKE);
        lineNumberPaint.setTypeface(getTypeface());
        lineNumberPaint.setAntiAlias(true);
        lineNumberPaint.setFakeBoldText(false);
        lineNumberPaint.setSubpixelText(true);
        PathEffect effect = new DashPathEffect(new float[]{4, 4, 4, 4}, 1);
        lineNumberPaint.setPathEffect(effect);
        lineNumberPaint.setColor(lineNumberColor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (showLineNumber) {
            int height = getHeight();
            int line_height = getLineHeight();
            int count = height / line_height;
            if (getLineCount() > count) {
                count = getLineCount();//for long text with scrolling
            }
            Rect r = mRect;
            Paint paint = lineNumberPaint;
            String maxNumber = String.format("%0" + String.valueOf(count).length() + "d", 8);
            paint.setTextSize(getTextSize());
            paint.getTextBounds(maxNumber, 0, maxNumber.length(), mRect);
            int lineNumberWidth = r.width() + 2 * numberMargin;
            setPadding(lineNumberWidth + numberMargin, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long st = System.currentTimeMillis();
        if (showLineNumber) {
            int height = getHeight();
            int line_height = getLineHeight();
            int count = height / line_height;
            if (getLineCount() > count) {
                count = getLineCount();//for long text with scrolling
            }
            Rect r = mRect;
            Paint paint = lineNumberPaint;
            String maxNumber = String.format("%0" + String.valueOf(count).length() + "d", 8);
            paint.getTextBounds(maxNumber, 0, maxNumber.length(), mRect);
            int baseLeft = getScrollX();
            int lineNumberWidth = r.width() + 2 * numberMargin;
            int baseline = getLineBounds(0, r);//first line
            //draw split line
            canvas.drawLine(baseLeft + lineNumberWidth, 0, baseLeft + lineNumberWidth, count * line_height, paint);
            getLocalVisibleRect(r);
            int startLine = r.top / line_height;
            baseline += startLine * line_height;
            int endLine = (r.bottom + line_height - 1) / line_height;
            Rect tRect = new Rect();
            PathEffect pathEffect = paint.getPathEffect();
            paint.setPathEffect(null);
            for (int i = startLine; i < endLine; i++) {
                String number = String.valueOf(i + 1);
                paint.getTextBounds(number, 0, number.length(), tRect);
                canvas.drawText(number, baseLeft + lineNumberWidth - tRect.width() - numberMargin, baseline + 1, paint);
                baseline += line_height;
            }
            paint.setPathEffect(pathEffect);
        }
        Log.d(null, "C Cost:" + (System.currentTimeMillis() - st));
        st = System.currentTimeMillis();
        super.onDraw(canvas);
        Log.d(null, "S Cost:" + (System.currentTimeMillis() - st));
    }
}


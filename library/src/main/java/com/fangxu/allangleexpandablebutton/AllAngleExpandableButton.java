package com.fangxu.allangleexpandablebutton;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dear33 on 2016/9/8.
 */
public class AllAngleExpandableButton extends View implements ValueAnimator.AnimatorUpdateListener {

    private List<ButtonData> buttonDatas;
    private Map<ButtonData, ButtonAnimInfo> animInfoMap;

    private static final int DEFAULT_EXPAND_ANIMATE_DURATION = 225;
    private static final int DEFAULT_BUTTON_GAP_DP = 50;

    private boolean expanded = false;

    private float startAngle;
    private float endAngle;

    private RectF buttonOval;
    private PointF buttonCenter;
    private int buttonRadius;
    private int width;
    private int height;

    private Paint paint;
    private Paint textPaint;

    private boolean animating = false;
    private boolean maskAttached = false;
    private ValueAnimator updateValueAnimator;
    private float animateProgress;
    private MaskView maskView;
    private AngleCalculator angleCalculator;
    private int buttonGap = DEFAULT_BUTTON_GAP_DP;

    public AllAngleExpandableButton(Context context) {
        this(context, null);
    }

    public AllAngleExpandableButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllAngleExpandableButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        initPaint();
    }

    private void initPaint() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (buttonOval == null || buttonDatas == null || buttonDatas.isEmpty()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        ButtonData buttonData = buttonDatas.get(0);
        int desiredWidth = DimenUtil.dp2px(getContext(), buttonData.getButtonSizeDp());
        int desiredHeight = DimenUtil.dp2px(getContext(), buttonData.getButtonSizeDp());
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width, height;
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawButton(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        initButtonInfo();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return !animating;
            case MotionEvent.ACTION_UP:
                if (expanded) {
                    collapse();
                } else {
                    expand();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        if (valueAnimator == updateValueAnimator) {
            animateProgress = (float) valueAnimator.getAnimatedValue();
            Log.i("animateProgress-->", "" + animateProgress);
        }
        invalidate();
        if (maskAttached) {
            maskView.updateButtons();
            maskView.invalidate();
        }
    }

    private void expand() {
        checkUpdateAnimator();
        updateValueAnimator = ValueAnimator.ofFloat(0, 1);
        updateValueAnimator.setDuration(DEFAULT_EXPAND_ANIMATE_DURATION);
        updateValueAnimator.addUpdateListener(this);
        updateValueAnimator.addListener(new Animator.AnimatorListener() {
            boolean canceled = false;

            @Override
            public void onAnimationStart(Animator animator) {
                animating = true;
                attachMask();
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animating = false;
                if (!canceled) {
                    expanded = true;
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                canceled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        updateValueAnimator.start();
    }

    private void checkUpdateAnimator() {
        if (updateValueAnimator != null) {
            updateValueAnimator.cancel();
        }
    }

    private void collapse() {
        checkUpdateAnimator();
        updateValueAnimator = ValueAnimator.ofFloat(1, 0);
        updateValueAnimator.setDuration(DEFAULT_EXPAND_ANIMATE_DURATION);
        updateValueAnimator.addUpdateListener(this);
        updateValueAnimator.addListener(new Animator.AnimatorListener() {
            boolean canceled = false;

            @Override
            public void onAnimationStart(Animator animator) {
                animating = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animating = false;
                if (!canceled) {
                    expanded = false;
                    detachMask();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                canceled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        updateValueAnimator.start();
    }

    private void attachMask() {
        if (maskView == null) {
            maskView = new MaskView(getContext(), this);
        }

        if (!maskAttached) {
            ViewGroup root = (ViewGroup) getRootView();
            root.addView(maskView);
            maskAttached = true;
            maskView.initButtonRect();
        }
    }

    private void detachMask() {
        if (maskAttached) {
            ViewGroup root = (ViewGroup) getRootView();
            root.removeView(maskView);
            maskAttached = false;
            for (int i = 0; i < buttonDatas.size(); i++) {
                ButtonData buttonData = buttonDatas.get(i);
                RectF rectF = animInfoMap.get(buttonData).getRectF();
                int size = DimenUtil.dp2px(getContext(), buttonData.getButtonSizeDp());
                rectF.left = 0;
                rectF.right = size;
                rectF.top = 0;
                rectF.bottom = size;
            }
        }
        invalidate();
    }

    private void drawButton(Canvas canvas) {
        if (buttonOval == null || buttonDatas == null || buttonDatas.isEmpty()) {
            return;
        }

        ButtonData buttonData = buttonDatas.get(0);
        drawButton(canvas, paint, buttonData);
    }

    private void drawButton(Canvas canvas, Paint paint, ButtonData buttonData) {
        paint.setColor(buttonData.getBackgroundColor());
        RectF rectF = animInfoMap.get(buttonData).getRectF();
        canvas.drawOval(rectF, paint);
        if (buttonData.isIconData()) {
            if (buttonData.getIcon() == null) {
                throw new IllegalArgumentException("iconData is true, icon drawable cannot be null");
            }
            Drawable drawable = buttonData.getIcon();
            int left = (int) rectF.left + (int) buttonData.getPadding();
            int right = (int) rectF.right - (int) buttonData.getPadding();
            int top = (int) rectF.top + (int) buttonData.getPadding();
            int bottom = (int) rectF.bottom - (int) buttonData.getPadding();
            drawable.setBounds(left, top, right, bottom);
            drawable.draw(canvas);
        } else {
            if (buttonData.getText() == null) {
                throw new IllegalArgumentException("iconData is false, text cannot be null");
            }
            String text = buttonData.getText();
            textPaint = getTextPaint(buttonData.getTextSizeSp(), buttonData.getTextColor());
            canvas.drawText(text, rectF.centerX(), rectF.centerY() - (textPaint.ascent() + textPaint.descent()) / 2, textPaint);
        }
    }

    private Paint getTextPaint(int sizeSp, int color) {
        if (textPaint == null) {
            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setTextAlign(Paint.Align.CENTER);
        }

        textPaint.setTextSize(DimenUtil.sp2px(getContext(), sizeSp));
        textPaint.setColor(color);
        return textPaint;
    }

    private void initButtonInfo() {
        float innerWidth = width - (getPaddingLeft() + getPaddingRight());
        float innerHeight = height - (getPaddingTop() + getPaddingBottom());
        buttonRadius = (int) (Math.min(innerWidth / 2, innerHeight / 2));
        buttonCenter = new PointF(getPaddingLeft() + innerWidth / 2, getPaddingTop() + innerHeight / 2);
        buttonOval = new RectF(buttonCenter.x - buttonRadius, buttonCenter.y - buttonRadius
                , buttonCenter.x + buttonRadius, buttonCenter.y + buttonRadius);
    }

    public AllAngleExpandableButton setStartAngle(float startAngle) {
        this.startAngle = startAngle;
        return this;
    }

    public AllAngleExpandableButton setEndAngle(float endAngle) {
        this.endAngle = endAngle;
        return this;
    }

    public AllAngleExpandableButton setButtonDatas(List<ButtonData> buttonDatas) {
        if (buttonDatas == null) {
            return this;
        }
        this.buttonDatas = new ArrayList<>(buttonDatas);
        animInfoMap = new HashMap<>(this.buttonDatas.size());
        for (ButtonData buttonData : buttonDatas) {
            ButtonAnimInfo info = new ButtonAnimInfo();
            info.set(getContext(), buttonData);
            animInfoMap.put(buttonData, info);
        }
        angleCalculator = new AngleCalculator(startAngle, endAngle, buttonDatas.size() - 1);
        return this;
    }

    @SuppressLint("ViewConstructor")
    private static class MaskView extends View {
        private AllAngleExpandableButton allAngleExpandableButton;
        private Rect rawButtonRect = new Rect();
        private RectF initialSubButtonRectF = new RectF();
        private Paint paint;

        public MaskView(Context context, AllAngleExpandableButton button) {
            super(context);
            allAngleExpandableButton = button;

            paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);

//            setBackgroundColor(0x66ff0066);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            View root = getRootView();
            setMeasuredDimension(root.getWidth(), root.getHeight());
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawButtons(canvas, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return allAngleExpandableButton.expanded;
                case MotionEvent.ACTION_UP:
                    int index = getTouchedButtonIndex(event.getX(), event.getY());
                    Log.i("testOnTouch", "index=" + index);
                    allAngleExpandableButton.collapse();
                    break;
            }
            return super.onTouchEvent(event);
        }

        private int getTouchedButtonIndex(float x, float y) {
            for (int i = 0; i < allAngleExpandableButton.buttonDatas.size(); i++) {
                ButtonData buttonData = allAngleExpandableButton.buttonDatas.get(i);
                ButtonAnimInfo animInfo = allAngleExpandableButton.animInfoMap.get(buttonData);
                RectF rectF = animInfo.getRectF();
                if (x >= rectF.left && x <= rectF.right && y >= rectF.top && y<=rectF.bottom) {
                    return i;
                }
            }
            return -1;
        }

        private void initButtonRect() {
            allAngleExpandableButton.getGlobalVisibleRect(rawButtonRect);
            for (int i = 0; i < allAngleExpandableButton.buttonDatas.size(); i++) {
                ButtonData buttonData = allAngleExpandableButton.buttonDatas.get(i);
                ButtonAnimInfo animInfo = allAngleExpandableButton.animInfoMap.get(buttonData);
                RectF rectF = animInfo.getRectF();
                int buttonRadius = DimenUtil.dp2px(getContext(), buttonData.getButtonSizeDp()) / 2;
                if (i == 0) {
                    rectF.left = rawButtonRect.left;
                    rectF.right = rawButtonRect.right;
                    rectF.top = rawButtonRect.top;
                    rectF.bottom = rawButtonRect.bottom;
                } else {
                    float leftTmp = rectF.left;
                    float topTmp = rectF.top;
                    rectF.left = leftTmp + rawButtonRect.centerX() - buttonRadius;
                    rectF.right = leftTmp + rawButtonRect.centerX() + buttonRadius;
                    rectF.top = topTmp + rawButtonRect.centerY() - buttonRadius;
                    rectF.bottom = topTmp + rawButtonRect.centerY() + buttonRadius;
                    initialSubButtonRectF.set(rectF);
                }
            }
        }

        private void updateButtons() {
            List<ButtonData> buttonDatas = allAngleExpandableButton.buttonDatas;
            ButtonData buttonData = allAngleExpandableButton.buttonDatas.get(0);
            int radiusMain = DimenUtil.dp2px(getContext(), buttonData.getButtonSizeDp()) / 2;
            for (int i = 1; i < buttonDatas.size(); i++) {
                buttonData = buttonDatas.get(i);
                ButtonAnimInfo animInfo = allAngleExpandableButton.animInfoMap.get(buttonData);
                RectF rectF = animInfo.getRectF();
                int desX;
                int desY;
                int radiusCurrent = DimenUtil.dp2px(getContext(), buttonData.getButtonSizeDp()) / 2;
                int radius = radiusMain + radiusCurrent + DimenUtil.dp2px(getContext(), allAngleExpandableButton.buttonGap);
                if (allAngleExpandableButton.expanded) {
                    rectF.left = rectF.left - (rectF.left - initialSubButtonRectF.left) * (1 - allAngleExpandableButton.animateProgress);
                    rectF.right = rectF.right - (rectF.right - initialSubButtonRectF.right) * (1 - allAngleExpandableButton.animateProgress);
                    rectF.top = rectF.top - (rectF.top - initialSubButtonRectF.top) * (1 - allAngleExpandableButton.animateProgress);
                    rectF.bottom = rectF.bottom - (rectF.bottom - initialSubButtonRectF.bottom) * (1 - allAngleExpandableButton.animateProgress);
                } else {
                    desX = allAngleExpandableButton.angleCalculator.getDesX(radius, i);
                    desY = allAngleExpandableButton.angleCalculator.getDesY(radius, i);
                    rectF.left = initialSubButtonRectF.left + desX * allAngleExpandableButton.animateProgress;
                    rectF.right = initialSubButtonRectF.right + desX * allAngleExpandableButton.animateProgress;
                    rectF.top = initialSubButtonRectF.top - desY * allAngleExpandableButton.animateProgress;
                    rectF.bottom = initialSubButtonRectF.bottom - desY * allAngleExpandableButton.animateProgress;
                }
            }
        }

        private void drawButtons(Canvas canvas, Paint paint) {
            for (int i = allAngleExpandableButton.buttonDatas.size() - 1; i >= 0; i--) {
                ButtonData buttonData = allAngleExpandableButton.buttonDatas.get(i);
                allAngleExpandableButton.drawButton(canvas, paint, buttonData);
            }
        }
    }
}

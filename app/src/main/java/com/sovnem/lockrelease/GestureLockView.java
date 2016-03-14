package com.sovnem.lockrelease;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedHashSet;

/**
 * 视图结构为3X3的矩阵点集合，点周围一定区域为有效范围s，这个有效范围不能大于相邻两个点的距离的一半。
 * 手指落在这个有效范围s内，则判定该touch对这个点有效，手势路径就会将这个点计算在内 手指落在点内则会有一个该点逐渐放大然后缩小的动画效果
 * 手指从一个点移出的时候在有效范围s内
 * ，手指到该点之间的线段是不显示的，当手指移出范围s，到手指离该点2s之间过程手指与该点之间的线段逐渐显示,所以综上来说s是小于点点间距的三分之一的
 * 如果手势密码错误则已经连接的线和点都变成橘黄色，并持续三秒钟再复原,如果在这三秒钟内有手势操作则立即复原
 *
 * @author monkey-d-wood
 */
public class GestureLockView extends View {
    private static final int ANIM_DURATION = 150;// 点选中时候的动画时间
    private static final int RECOVER_DURATION = 3000;// 错误路径持续时长
    private int NORMAL_COLOR;// 正常的颜色
    private int ERROR_COLOR;// 手势错误颜色
    private final int row = 3;// 矩阵的边数

    private PWDPath ppath;// 密码路径
    private PPoint[][] points = new PPoint[3][3];
    private boolean isRight = true;

    public GestureLockView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    public GestureLockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public GestureLockView(Context context) {
        super(context);
        init(null);
    }

    private void init(AttributeSet attrs) {
        //		NORMAL_COLOR = Color.WHITE;
        //		ERROR_COLOR = Color.parseColor("#F54F20");
        if (attrs != null) {
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.lockview);
            NORMAL_COLOR = ta.getColor(R.styleable.lockview_normalColor, Color.WHITE);
            ERROR_COLOR = ta.getColor(R.styleable.lockview_wrongColor, Color.parseColor("#F54F20"));
            Log.i("info", "颜色啊");
        }
        ppath = new PWDPath();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawPoints(canvas);
        ppath.draw(canvas, isRight);
    }

    private void drawPoints(Canvas canvas) {
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < row; j++) {
                points[i][j].draw(canvas, true);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {//该view为矩形
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        w = h = Math.min(w, h);
        setMeasuredDimension(w, h);
    }

    private int width;//记录view的边长

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        resetPoints(width);
    }

    /**
     * 重置点的属性
     */
    private void resetPoints(int w) {
        initSizeParams(w);
        initPoints();
    }

    // 根据view的尺寸确定
    private float pDis;// 两点之间的间距 为边长的三分之一
    private float pointR;// 点的初始半径 为pDis的二十分之一
    private float pointREx;// 点的动画最大半径 为点半径的2.5倍
    private float safeDis;// 点的‘领域’半径 为pDis的十分之三
    private float fadeDis;// 路径完全显示的半径 为safeDis的二倍
    private float mPadding;// view中点的矩形和view的外边界距离 为宽度的六分之一
    private float lineW;// 画出的线的宽度 为点半径的二分之一

    /**
     * 确定上边提到的参数的值
     */
    private void initSizeParams(int w) {
        mPadding = w / 6.0f;
        pDis = w / 3.0f;
        pointR = pDis / 17;
        pointREx = pointR * 2.5f;
        safeDis = pDis * 3.0f / 10;
        fadeDis = safeDis * 2.0f;
        lineW = pointR / 1.8f;
        ppath.setLineWidth(lineW);
    }

    private void initPoints() {
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < row; j++) {
                points[i][j] = new PPoint(i * pDis + mPadding, j * pDis + mPadding, pointR);
            }
        }
    }

    /**
     * 显示主体的点的实体类
     *
     * @author monkey-d-wood
     */
    private class PPoint {
        float x, y, r;
        boolean animating;
        boolean selected;
        Paint nPointpPaint;// 正常点画笔
        Paint ePointpPaint;// 错误的点得画笔

        public PPoint(float x, float y, float r) {
            super();
            this.x = x;
            this.y = y;
            this.r = r;

            nPointpPaint = new Paint();
            nPointpPaint.setColor(NORMAL_COLOR);
            nPointpPaint.setStyle(Style.FILL_AND_STROKE);

            ePointpPaint = new Paint();
            ePointpPaint.setColor(ERROR_COLOR);
            ePointpPaint.setStyle(Style.FILL_AND_STROKE);
        }

        public void draw(Canvas canvas, boolean a) {
            canvas.drawCircle(x, y, r, a ? nPointpPaint : ePointpPaint);
        }

        // 判定是否应该连接该点
        public boolean isInCtrl(float x, float y) {
            return (x - this.x) * (x - this.x) + (this.y - y) * (this.y - y) <= safeDis * safeDis;
        }

        public boolean equals(PPoint o) {
            return x == o.x && y == o.y;
        }

        public PPoint setIndex(int i, int j) {
            this.i = i;
            this.j = j;
            return this;
        }

        int i, j;

        public float distanceTo(PPoint p2) {
            return (float) Math.sqrt((this.x - p2.x) * (this.x - p2.x) + (this.y - p2.y) * (this.y - p2.y));
        }
    }

    /**
     * 的到触点所在的‘领域’内的点, 如果不在领域内则返回null
     */
    private PPoint getControllerPoint(float x, float y) {
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < row; j++) {
                if (points[i][j].isInCtrl(x, y)) {
                    return points[i][j].setIndex(i, j);
                }
            }
        }
        return null;
    }

    /**
     * 复原view状态
     */
    private void reset() {
        ppath = new PWDPath();
        resetPoints(width);
        isRight = true;
        postInvalidate();
    }

    private Handler handler = new Handler();
    private Runnable task = new Runnable() {

        @Override
        public void run() {
            reset();
        }
    };
    private PPoint down;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isRight) {
                handler.removeCallbacks(task);
                reset();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    Result result = new Result();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float eX = event.getX();
        float eY = event.getY();
        int action = event.getAction();
        down = getControllerPoint(eX, eY);
        if (null != down) {//如果运动到了一个可吸附点的领域内
            if (!down.animating && !down.selected) {
                doAnimation(down);
            }
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (null != down && !down.selected) {
                    ppath.moveTo(down);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (null != down && !down.selected) {
                    if (ppath.last == null) {
                        ppath.moveTo(down);
                    } else {
                        ppath.lineTo(down);
                    }
                }
                if (ppath.last != null) {
                    ppath.startTo(eX, eY);
                }
                invalidate();

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                ppath.pathStart.reset();
                if (null != down && !down.selected) {
                    ppath.lineTo(down);
                    invalidate();
                }
                if (ppath.getPwd() != null && ppath.getPwd().length() > 0) {
                    if (null != callback) {
                        callback.onFinish(ppath.getPwd(), result);
                    }
                    if (result.isRight) {
                        reset();
                    } else {
                        isRight = false;
                        invalidate();
                        handler.postDelayed(task, RECOVER_DURATION);
                    }
                }
                break;
        }
        return true;
    }

    public class Result {
        private boolean isRight;

        public boolean isRight() {
            return isRight;
        }

        public void setRight(boolean isRight) {
            this.isRight = isRight;
        }

    }

    /**
     * 在这个点上做运动 o 动画
     */
    private void doAnimation(final PPoint down) {
        ValueAnimator va = ValueAnimator.ofFloat(pointR, pointREx);
        va.setDuration(ANIM_DURATION);
        va.setRepeatCount(1);
        va.setRepeatMode(ValueAnimator.REVERSE);
        va.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = Float.parseFloat(animation.getAnimatedValue().toString());
                down.r = value;
                invalidate();
            }
        });
        va.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                down.animating = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                down.animating = false;
                down.selected = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });
        va.start();
    }

    /**
     * 手势密码的抽象，包括路径和路径上的点 点集合就是路径上已经‘吸附’的点，路径有两个 一个path 是点与点之间的连接的路径
     * 另外一个pathStart是
     * 手指移动的位置和path最后一个‘吸附’的点之间的路径，这个路径不断随着手指移动而变化,并且还有透明度等需要处理所以单独出来
     *
     * @author monkey-d-wood
     */
    private class PWDPath {
        // ArrayList<PPoint> pwdps;
        Path path;// 正常的连接的路径 即点与点之间的连接path
        Path pathStart;// 从一个点出发 终点任意的path
        PPoint last;
        private Paint nPaint,// 正常颜色线的画笔
                ePaint, // 错误颜色线的画笔
                endPaint;// 手指移动的时候多余部分的线的画笔
        LinkedHashSet<PPoint> pwdps;

        public PWDPath() {
            pwdps = new LinkedHashSet<GestureLockView.PPoint>();
            path = new Path();
            pathStart = new Path();

            nPaint = new Paint();
            nPaint.setColor(NORMAL_COLOR);
            nPaint.setStyle(Style.STROKE);

            endPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            endPaint.setColor(getColor(1.0f));
            endPaint.setStyle(Style.STROKE);
            endPaint.setStrokeCap(Cap.ROUND);

            ePaint = new Paint();
            ePaint.setColor(ERROR_COLOR);
            ePaint.setStyle(Style.STROKE);

        }

        public String getPwd() {
            StringBuilder sb = new StringBuilder();
            for (PPoint p : pwdps) {
                int k = p.i + 1 + p.j * row;
                sb.append(k);
            }
            return sb.toString();
        }

        /**
         * f范围0~1
         * 根据f值得到一个相应透明度的颜色
         */
        private int getColor(float f) {
            return Color.argb((int) (255 * f), Color.red(NORMAL_COLOR), Color.green(NORMAL_COLOR), Color.blue(NORMAL_COLOR));
        }

        public void setLineWidth(float lineW) {
            nPaint.setStrokeWidth(lineW);
            ePaint.setStrokeWidth(lineW);
            endPaint.setStrokeWidth(lineW);
        }

        /**
         * 移动到可以吸附的点，两个路径都需要接入这个点，两个路径都需要将这个点设为起始点
         */
        public void moveTo(PPoint p) {
            path.reset();
            path.moveTo(p.x, p.y);
            pwdps = new LinkedHashSet<GestureLockView.PPoint>();

            exAdd(p);
            pwdps.add(p);

            pathStart.reset();
            pathStart.moveTo(p.x, p.y);
            last = p;
        }

        /**
         * 处理两个连接点之间有一个未连接点得情况
         *
         * @param p
         */
        private void exAdd(PPoint p) {
            int i1 = null == last ? p.i : last.i;
            int i2 = p.i;
            int j1 = null == last ? p.j : last.j;
            int j2 = p.j;
            if (Math.abs(i1 - i2) == 2 && j1 == j2) {
                if (!points[(i1 + i2) / 2][j1].selected) {
                    doAnimation(points[(i1 + i2) / 2][j1]);
                    pwdps.add(points[(i1 + i2) / 2][j1].setIndex((i1 + i2) / 2, j1));
                }
            } else if (Math.abs(j1 - j2) == 2 && i1 == i2) {
                if (!points[i1][(j1 + j2) / 2].selected) {
                    doAnimation(points[i1][(j1 + j2) / 2]);
                    pwdps.add(points[i1][(j1 + j2) / 2].setIndex(i1, (j1 + j2) / 2));
                }
            } else if (Math.abs(i1 - i2) == 2 && Math.abs(j1 - j2) == 2) {
                if (!points[(i1 + i2) / 2][(j1 + j2) / 2].selected) {
                    doAnimation(points[(i1 + i2) / 2][(j1 + j2) / 2]);
                    pwdps.add(points[(i1 + i2) / 2][(j1 + j2) / 2].setIndex((i1 + i2) / 2, (j1 + j2) / 2));
                }
            }
        }

        /**
         * 意思同path的lineTo方法，path需要执行lineTo方法，而pathStart则仍然需要设置成起始点
         */
        public void lineTo(PPoint p) {
            path.lineTo(p.x, p.y);
            exAdd(p);
            pwdps.add(p);
            pathStart.reset();
            pathStart.moveTo(p.x, p.y);
            last = p;
        }

        PPoint touchP;

        /**
         * 这是在吸附了一个点之后，手指移动
         * 这个时候需要重新初始化该path，起始点为pwdpath连接到的最后一个点，终点为传入的触摸事件发生的位置
         */
        public void startTo(float x, float y) {
            pathStart.reset();
            pathStart.moveTo(last.x, last.y);
            pathStart.lineTo(x, y);
            touchP = new PPoint(x, y, 0);
        }

        /**
         * 绘制密码路径,正确的时候不画点，错误的时候画点
         */
        public void draw(Canvas c, boolean isRight) {
            if (isRight) {
                c.drawPath(path, nPaint);
            } else {
                c.drawPath(path, ePaint);
                for (PPoint point : pwdps) {
                    point.draw(c, isRight);
                }
            }
            float factor = 1;
            if (null != touchP) {//如果正在触摸，根据触摸点 到上一个连接的点的距离计算 多出线头的透明度
                float dis = touchP.distanceTo(last);
                if (dis > fadeDis) {
                    factor = 1;
                } else if (dis >= safeDis) {
                    factor = (dis - safeDis) * 1.0f / safeDis;
                } else {
                    factor = 0;
                }
            }
            endPaint.setColor(getColor(factor));
            c.drawPath(pathStart, endPaint);
        }
    }

    private GestureLockCallback callback;

    public void setCallback(GestureLockCallback callback) {
        this.callback = callback;
    }

    /**
     * 手势密码回调接口
     *
     * @author monkey-d-wood
     */
    public interface GestureLockCallback {
        public void onFinish(String pwdString, Result result);
    }

}

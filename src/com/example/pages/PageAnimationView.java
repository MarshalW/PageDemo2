package com.example.pages;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.opengles.GL;

/**
 * Created with IntelliJ IDEA.
 * User: marshal
 * Date: 13-5-9
 * Time: 下午12:05
 * To change this template use File | Settings | File Templates.
 */
public class PageAnimationView extends TextureView implements TextureView.SurfaceTextureListener {

    //动画时长
    private long duration = 500;

    private View targetView;

    private RenderRunnable renderRunnable;

    private Animator.AnimatorListener animatorListener;

    public PageAnimationView(Context context) {
        super(context);
        this.init();
    }

    private void init() {
//        this.setAlpha(0.2f);
        this.setOpaque(false);
        this.setSurfaceTextureListener(this);
    }

    public void setTargetView(View targetView) {
        this.targetView = targetView;
        targetView.setVisibility(INVISIBLE);
    }

    public void startAnimation() {
        if(renderRunnable==null){
            return;
        }

        targetView.setDrawingCacheEnabled(true);
        Bitmap texture = Bitmap.createBitmap(targetView.getDrawingCache());
//        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
//        Bitmap texture = Bitmap.createBitmap(1, 1, conf); // this creates a MUTABLE bitmap
//        Canvas canvas = new Canvas(texture);
        renderRunnable.setTexture(texture);
        targetView.setDrawingCacheEnabled(false);

        ValueAnimator animator = ValueAnimator.ofFloat(1f, -1f);
        animator.setDuration(this.duration);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                renderRunnable.setFactor((Float) valueAnimator.getAnimatedValue());
                renderRunnable.render();
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                targetView.setVisibility(VISIBLE);
                PageAnimationView.this.setVisibility(INVISIBLE);
            }
        });

        if (animatorListener != null) {
            animator.addListener(animatorListener);
        }

        animator.start();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        renderRunnable = new RenderRunnable(surfaceTexture);
        renderRunnable.setSize(width, height);
        new Thread(renderRunnable).start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(final SurfaceTexture surfaceTexture) {
//        Log.e("pagedemo","====>>>>>surface destroyed, thread: "+Thread.currentThread());
//
//        renderRunnable.handler.post(new Runnable() {
//            @Override
//            public void run() {
//                surfaceTexture.release();
//            }
//        });

        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    class RenderRunnable implements Runnable {

        static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        static final int EGL_OPENGL_ES2_BIT = 4;

        private EGL10 egl;
        private EGLDisplay eglDisplay;
        private EGLConfig eglConfig;
        private EGLContext eglContext;
        private EGLSurface eglSurface;
//        private GL gl;

        SurfaceTexture surfaceTexture;

        Handler handler;

        CurlRenderer renderer;

        int width, height;

        private boolean isEnd;

        RenderRunnable(SurfaceTexture surfaceTexture) {
            this.surfaceTexture = surfaceTexture;
        }

        public void setSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public void setFactor(float factor) {
            renderer.setPositionFactor(factor);
        }

        public void setTexture(Bitmap texture) {
            renderer.getMesh().getTexturePage().setTexture(texture);
        }

        @Override
        public void run() {

            ActivityManager actvityManager = (ActivityManager) ((Activity)getContext()).getSystemService( Activity.ACTIVITY_SERVICE );
            ActivityManager.MemoryInfo mInfo = new ActivityManager.MemoryInfo ();
            actvityManager.getMemoryInfo( mInfo );
//            Log.v("pagedemo",">>>>>>momory: " + mInfo.availMem/1024/1024+"M");

            initGL();

            renderer = new CurlRenderer();
            this.renderer.getMesh().setFlipTexture(false);
            this.renderer.setMargins(.01f, .01f, .01f, .01f);

            renderer.onSurfaceCreated(null, null);
            renderer.onSurfaceChanged(null, width, height);

            Looper.prepare();
            handler = new Handler();
            Looper.loop();

//            Log.d("pagedemo","render thread quit");
        }

        public void render() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(isEnd){
                        return;
                    }

                    renderer.onDrawFrame(null);

                    if (!egl.eglSwapBuffers(eglDisplay, eglSurface)) {
                        //throw new RuntimeException("Cannot swap buffers");
//                        Log.e("pagedemo","Cannot swap buffers");
                    }
                }
            });
        }

        private void initGL() {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//
//            }
            egl = (EGL10) EGLContext.getEGL();

            eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed "
                        + GLUtils.getEGLErrorString(egl.eglGetError()));
            }

            int[] version = new int[2];
            if (!egl.eglInitialize(eglDisplay, version)) {
                throw new RuntimeException("eglInitialize failed " +
                        GLUtils.getEGLErrorString(egl.eglGetError()));
            }

            eglConfig = chooseEglConfig();
            if (eglConfig == null) {
                throw new RuntimeException("eglConfig not initialized");
            }

            eglContext = createContext(egl, eglDisplay, eglConfig);

            eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, null);

            if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
                int error = egl.eglGetError();
                if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                    Log.e("page", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                    return;
                }
                throw new RuntimeException("createWindowSurface failed "
                        + GLUtils.getEGLErrorString(error));
            }

            if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                throw new RuntimeException("eglMakeCurrent failed "
                        + GLUtils.getEGLErrorString(egl.eglGetError()));
            }

//            gl = eglContext.getGL();

//            Log.d("pagedemo",">>>>init gl, thread: "+Thread.currentThread());

        }

        EGLConfig chooseEglConfig() {
            int[] configsCount = new int[1];
            EGLConfig[] configs = new EGLConfig[1];
            int[] configSpec = getConfig();
            if (!egl.eglChooseConfig(eglDisplay, configSpec, configs, 1, configsCount)) {
                throw new IllegalArgumentException("eglChooseConfig failed " +
                        GLUtils.getEGLErrorString(egl.eglGetError()));
            } else if (configsCount[0] > 0) {
                return configs[0];
            }
            return null;
        }

        private int[] getConfig() {
            return new int[]{
                    EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 0,
                    EGL10.EGL_STENCIL_SIZE, 0,
                    EGL10.EGL_NONE
            };
        }

        EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
            int[] attrib_list =
                    {
                            EGL_CONTEXT_CLIENT_VERSION, 2,
                            EGL10.EGL_NONE};
            return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
        }

        void quit() {
            if(handler==null){
                return;
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
//                    Log.d("pagedemo",">>>>>quit renderer");
//                    renderer.getMesh().resetTexture();
                    handler.getLooper().quit();
                    isEnd=true;

                    surfaceTexture.release();

                    egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE,EGL10.EGL_NO_SURFACE,EGL10.EGL_NO_CONTEXT);
                    egl.eglDestroySurface(eglDisplay, eglSurface);
                    egl.eglDestroyContext(eglDisplay, eglContext);
//                    egl.eglTerminate(eglDisplay);
                }
            });
        }
    }

    public void onPause() {
        if(renderRunnable!=null){
            renderRunnable.quit();
        }
    }

    public void setAnimationListener(Animator.AnimatorListener listener) {
        this.animatorListener = listener;
    }
}

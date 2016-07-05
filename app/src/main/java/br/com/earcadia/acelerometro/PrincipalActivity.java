package br.com.earcadia.acelerometro;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PrincipalActivity extends AppCompatActivity implements SensorEventListener {
    private Sensor mSensor;
    private float gravidade[] = new float[]{0, 0};
    private ControleBolaView mControleBolaView;
    private float mSensorX, mSensorY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);


        if (mSensor != null) {
            mControleBolaView = new ControleBolaView(this);
            setContentView(mControleBolaView);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mControleBolaView.resume();
    }

    @Override
    protected void onPause() {
        mControleBolaView.pause();
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        mSensorX = -sensorEvent.values[0];
        mSensorY = sensorEvent.values[1];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public class ControleBolaView extends SurfaceView implements Runnable {

        Thread mThread = null;
        Bitmap mBola;
        float mMassa;
        SurfaceHolder mSuperficie;
        float mVelocidadeX, mVelocidadeY;
        float mPosicaoX = 0, mPosicaoY = 0;
        final float mMaxVelocidade = 40f;
        final double mMaxiVelocidadeXY = Math.sqrt(2) * mMaxVelocidade;
        boolean mSomHabilitado;
        MediaPlayer mMediaPlayer;
        boolean mEstaRodando;


        public ControleBolaView(Context context) {
            super(context);

            setKeepScreenOn(true);
            mBola = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            mSuperficie = getHolder();
            mMediaPlayer = MediaPlayer.create(getContext(), R.raw.quebro);

        }


        @Override
        public void run() {
            while (mEstaRodando) {
                if (mSuperficie.getSurface().isValid()) {
                    Canvas canvas = mSuperficie.lockCanvas();

                    if (canvas != null) {
                        float larguraValida = getWidth() - mBola.getWidth();
                        float alturaValida = getHeight() - mBola.getHeight();

                        mSomHabilitado = true;
                        mMassa = 5;
                        boolean colidiu = false;


                        //Calcula possição para X
                        mVelocidadeX += mSensorX / mMassa;
                        mVelocidadeX = constrain(-mMaxVelocidade, mVelocidadeX, mMaxVelocidade);
                        mPosicaoX = mPosicaoX + mVelocidadeX;


                        //Teste de colisão
                        if (mPosicaoX <= 0 || mPosicaoX >= larguraValida) {
                            mVelocidadeX = -0.8f * mVelocidadeX;
                            colidiu = true;
                        }

                        //Garante que objeto não sai da tela
                        mPosicaoX = constrain(0, mPosicaoX, larguraValida);


                        //Calcula possição para Y
                        mVelocidadeY += mSensorY / mMassa;
                        mVelocidadeY = constrain(-mMaxVelocidade, mVelocidadeY, mMaxVelocidade);
                        mPosicaoY = mPosicaoY + mVelocidadeY;


                        //Teste de colisão
                        if (mPosicaoY <= 0 || mPosicaoY >= alturaValida) {
                            mVelocidadeY = -0.8f * mVelocidadeY;
                            colidiu = true;
                        }

                        //Garante que objeto não sai da tela
                        mPosicaoY = constrain(0, mPosicaoY, alturaValida);


                        //Desenha objeto na tela
                        canvas.drawColor(Color.BLACK);
                        canvas.drawBitmap(mBola, (int) mPosicaoX, (int) mPosicaoY, null);

                        mSuperficie.unlockCanvasAndPost(canvas);

                        //Toca o som de batida
                        if (colidiu)
                            if (mSomHabilitado && colidiu) {
                                float volume = (float) Math.pow(Math.hypot((double) mVelocidadeX, (double) mVelocidadeY) / mMaxiVelocidadeXY, 2);
                                mMediaPlayer.setVolume(volume, volume);

                                if (mMediaPlayer.isPlaying())
                                    mMediaPlayer.seekTo(0);
                                else
                                    mMediaPlayer.start();
                            }
                    }
                }
            }

        }

        private float constrain(float min, float valor, float max) {
            return Math.max(Math.min(valor, max), min);
        }


        public void resume() {
            mEstaRodando = true;
            mThread = new Thread(this);
            mThread.start();
        }

        public void pause() {
            mEstaRodando = false;

            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mThread = null;
            }
        }
    }
}

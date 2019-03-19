package com.fxyan.ring;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.smurn.jply.Element;
import org.smurn.jply.ElementReader;
import org.smurn.jply.PlyReaderFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity
        extends AppCompatActivity
        implements GLSurfaceView.Renderer {

    Context context;
    GLSurfaceView surfaceView;
    CopyOnWriteArrayList<PlyModel> models = new CopyOnWriteArrayList<>();

    CompositeDisposable disposables = new CompositeDisposable();

    float[] mvpMatrix = new float[16];
    float[] mvMatrix = new float[16];
    float[] modelMatrix = new float[16];
    float[] viewMatrix = new float[16];
    float[] projectionMatrix = new float[16];

    private int programHandle;

    private Map<String, PlyModel> map = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        surfaceView = findViewById(R.id.surfaceView);

        surfaceView.setEGLContextClientVersion(2);

        surfaceView.setRenderer(this);

        readPlyFile("戒臂.ply");
        readPlyFile("花托.ply");
        readPlyFile("主石.ply");
    }

    @Override
    protected void onResume() {
        super.onResume();
        surfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        surfaceView.onPause();
    }

    private void updateModel(boolean isChecked, String path) {
        PlyModel model = map.get(path);
        if (isChecked) {
            if (model == null) {
                readPlyFile(path);
            } else {
                models.add(model);
            }
        } else {
            models.remove(model);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.8f, 0.8f, 0.8f, 1f);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        programHandle = GLESUtils.createAndLinkProgram("ply.vert", "ply.frag");

        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_MIRRORED_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_MIRRORED_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.kb);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        for (PlyModel model : models) {
            model.onSurfaceCreated(gl, config);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 50f, 0f, 0f, -5f, 0f, 1f, 0f);

        float ratio = (float) width / height;

        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 1f, 100f);
        for (PlyModel model : models) {
            model.onSurfaceChanged(gl, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(programHandle);

        Matrix.setIdentityM(modelMatrix, 0);
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
        Matrix.rotateM(modelMatrix, 0, angleInDegrees, 1, 1, 1);
        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0);

        for (PlyModel model : models) {
            model.onDrawFrame(viewMatrix, mvMatrix, mvpMatrix, programHandle);
        }
    }

    private void readPlyFile(String path) {
        Single.create((SingleOnSubscribe<PlyModel>) emitter -> {
            PlyReaderFile reader = null;
            try {
                reader = new PlyReaderFile(getAssets().open(path));

                float[] vertex = readVertex(reader);

                int[] index = readFace(reader);

                float[] texCoord = genTexCoord(vertex);

                emitter.onSuccess(new PlyModel(context, vertex, index, texCoord));
            } catch (IOException e) {
                emitter.onError(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        emitter.onError(e);
                    }
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new SingleObserver<PlyModel>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposables.add(d);
                    }

                    @Override
                    public void onSuccess(PlyModel plyModel) {
                        map.put(path, plyModel);
                        models.add(plyModel);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("fxYan", String.format("路径为 %s 的文件解析失败", path));
                    }
                });
    }

    private float[] readVertex(PlyReaderFile reader) throws IOException {
        float[] vertex;
        ElementReader elementReader = reader.nextElementReader();
        int PER_VERTEX_SIZE = 6;
        vertex = new float[elementReader.getCount() * PER_VERTEX_SIZE];
        for (int i = 0; i < elementReader.getCount(); i++) {
            Element element = elementReader.readElement();
            vertex[i * PER_VERTEX_SIZE] = (float) element.getDouble("x");
            vertex[i * PER_VERTEX_SIZE + 1] = (float) element.getDouble("y");
            vertex[i * PER_VERTEX_SIZE + 2] = (float) element.getDouble("z");
            vertex[i * PER_VERTEX_SIZE + 3] = (float) element.getDouble("nx");
            vertex[i * PER_VERTEX_SIZE + 4] = (float) element.getDouble("ny");
            vertex[i * PER_VERTEX_SIZE + 5] = (float) element.getDouble("nz");
        }
        elementReader.close();
        return vertex;
    }

    private int[] readFace(PlyReaderFile reader) throws IOException {
        int[] index;
        ElementReader elementReader = reader.nextElementReader();
        int PER_FACE_VERTEX_COUNT = 3;
        index = new int[elementReader.getCount() * PER_FACE_VERTEX_COUNT];
        for (int i = 0; i < elementReader.getCount(); i++) {
            Element element = elementReader.readElement();
            int[] vertex_indices = element.getIntList("vertex_indices");
            index[i * PER_FACE_VERTEX_COUNT] = vertex_indices[0];
            index[i * PER_FACE_VERTEX_COUNT + 1] = vertex_indices[1];
            index[i * PER_FACE_VERTEX_COUNT + 2] = vertex_indices[2];
        }
        elementReader.close();
        return index;
    }

    private float[] genTexCoord(float[] vertex) {
        float[] texCoord = new float[vertex.length / 3];
        for (int i = 0; i < texCoord.length; i += 6) {
            texCoord[i] = 0.0f;
            texCoord[i + 1] = 0.0f;
            if (i + 2 < texCoord.length) {
                texCoord[i + 2] = 1.0f;
                texCoord[i + 3] = 0.0f;
            }
            if (i + 4 < texCoord.length) {
                texCoord[i + 4] = 0.5f;
                texCoord[i + 5] = 1.0f;
            }
        }
        return texCoord;
    }

}

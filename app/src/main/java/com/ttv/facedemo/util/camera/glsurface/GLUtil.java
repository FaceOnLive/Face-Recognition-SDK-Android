package com.ttv.facedemo.util.camera.glsurface;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.IntBuffer;

public class GLUtil {
    private static final String TAG = "GLUtil";

    static final float[] SQUARE_VERTICES = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
    };
    /**
     * 0,1***********1,1
     * *             *
     * *             *
     * *             *
     * *             *
     * *             *
     * 0,0***********1,0
     */
    static final float[] COORD_VERTICES = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
    };

    /**
     * 1,1***********1,0
     * *             *
     * *             *
     * *             *
     * *             *
     * *             *
     * 0,1***********0,0
     */
    static final float[] ROTATE_90_COORD_VERTICES = {
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, 0.0f
    };

    /**
     * 1,0***********0,0
     * *             *
     * *             *
     * *             *
     * *             *
     * *             *
     * 1，1***********0,1
     */
    static final float[] ROTATE_180_COORD_VERTICES = {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
    };

    /**
     * 0,0***********0,1
     * *             *
     * *             *
     * *             *
     * *             *
     * *             *
     * 1,0***********1,1
     */
    static final float[] ROTATE_270_COORD_VERTICES = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };

    /**
     * 1,1***********0,1
     * *             *
     * *             *
     * *             *
     * *             *
     * *             *
     * 1,0***********0,0
     */
    static final float[] MIRROR_COORD_VERTICES = {
            1.0f, 1.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f
    };

    /**
     * 0,1***********0,0
     * *             *
     * *             *
     * *             *
     * *             *
     * *             *
     * 1,1***********1,0
     */
    static final float[] ROTATE_90_MIRROR_COORD_VERTICES = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };
    /**
     * 0,0***********1,0
     * *             *
     * *             *
     * *             *
     * *             *
     * *             *
     * 0,1***********1,1
     */
    static final float[] ROTATE_180_MIRROR_COORD_VERTICES = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };
    /**
     * 1,0***********1,1
     * *             *
     * *             *
     * *             *
     * *             *
     * *             *
     * 0,0***********0,1
     */
    static final float[] ROTATE_270_MIRROR_COORD_VERTICES = {
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            0.0f, 1.0f
    };

    static int createShaderProgram(String fragmentShaderCode, String vertexShaderCode) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        if (vertexShader == 0 || fragmentShader == 0) {
            return 0;
        }
        int mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        IntBuffer linked = IntBuffer.allocate(1);
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linked);
        if (linked.get(0) == 0) {
            return 0;
        }
        return mProgram;
    }

    static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader == 0) {
            Log.e(TAG, "loadShader: failed to create shader");
            checkGlErrorIfOccur("create shader " + shaderType);
            return 0;
        }
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":" + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
            checkGlErrorIfOccur("glGetShaderiv " + shaderType);
        }
        return shader;
    }

    private static void checkGlErrorIfOccur(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String errorMsg = String.format("error 0x%h occurred: %s", error, op);
            Log.e(TAG, errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    static float[] getCoordVerticesByPreviewParams(boolean isMirror, int rotateDegree) {
        float[] coordVertice = GLUtil.COORD_VERTICES;
        if (isMirror) {
            switch (rotateDegree) {
                case 0:
                    coordVertice = GLUtil.MIRROR_COORD_VERTICES;
                    break;
                case 90:
                    coordVertice = GLUtil.ROTATE_90_MIRROR_COORD_VERTICES;
                    break;
                case 180:
                    coordVertice = GLUtil.ROTATE_180_MIRROR_COORD_VERTICES;
                    break;
                case 270:
                    coordVertice = GLUtil.ROTATE_270_MIRROR_COORD_VERTICES;
                    break;
                default:
                    break;
            }
        } else {
            switch (rotateDegree) {
                case 0:
                    coordVertice = GLUtil.COORD_VERTICES;
                    break;
                case 90:
                    coordVertice = GLUtil.ROTATE_90_COORD_VERTICES;
                    break;
                case 180:
                    coordVertice = GLUtil.ROTATE_180_COORD_VERTICES;
                    break;
                case 270:
                    coordVertice = GLUtil.ROTATE_270_COORD_VERTICES;
                    break;
                default:
                    break;
            }
        }
        return coordVertice.clone();
    }
}

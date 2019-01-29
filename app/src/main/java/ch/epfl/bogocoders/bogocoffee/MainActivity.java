package ch.epfl.bogocoders.bogocoffee;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.Logger;
import com.microsoft.aad.adal.PromptBehavior;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int PERMISSION_ALL = 1;

    private SoundAnalyser soundAnalyser = null;

    private ToggleButton mAnalyseButton = null;

    private TextureView textureView = null;

    private ToggleButton mAutoButton = null;

    private Button mStatButton = null;
    private Button mSendButton = null;
    private SharedPreferences mSharedPref = null;
    private SharedPreferences.Editor editor = null;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;

    private SensorManager mSensorManager;
    private Sensor mLight;

    private String detected = "Unknown";

    private boolean lightOn = false;

    // Requesting permission to RECORD_AUDIO
    private String [] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
        }
    }

    private void startAuto() {
        try {
            while (mAutoButton.isChecked()) {
                while (detected.equals("Unknown")) {
                    while (!lightOn) {
                        Log.e(LOG_TAG, "lights off");
                        Thread.sleep(1000);
                    }
                    takePicture();
                    Thread.sleep(1000);
                }

                onAnalyse(true);
                SoundAnalyser.CoffeeType ctype = soundAnalyser.getDetected();
                while (ctype == SoundAnalyser.CoffeeType.Unknown) {
                    ctype = soundAnalyser.getDetected();
                    Thread.sleep(100);
                }
                Log.e(LOG_TAG, "Coffee detected is a " + detected + "with a " + ctype +" type");
                detected = "Unknown";
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            if (!mAutoButton.isChecked()) {
                onAnalyse(false);
                detected = "Unknown";
            }
        }
    }

    private void onAnalyse(boolean start) {
        if (start) {
            soundAnalyser.start();
        } else {
            soundAnalyser.stop();
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.activity_main);

        Log.e(LOG_TAG, "onCreate");

        ActivityCompat.requestPermissions(this, permissions, PERMISSION_ALL);

        soundAnalyser = new SoundAnalyser(this);

        mAnalyseButton     = findViewById(R.id.analyse);
        mSendButton        = findViewById(R.id.send);
        mStatButton        = findViewById(R.id.stats);
        mAutoButton        = findViewById(R.id.auto);
        textureView        = findViewById(R.id.preview);

        mAutoButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            Thread thread = null;
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSendButton.setClickable(false);
                    mSendButton.setEnabled(false);
                    mAnalyseButton.setClickable(false);
                    mAnalyseButton.setEnabled(false);
                    mAnalyseButton.setChecked(false);
                    thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            startAuto();
                        }
                    });
                    thread.start();
                } else {
                    thread.interrupt();
                    mSendButton.setClickable(true);
                    mSendButton.setEnabled(true);
                    mAnalyseButton.setClickable(true);
                    mAnalyseButton.setEnabled(true);
                }
            }
        });

        mAnalyseButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onAnalyse(isChecked);
            }
        });

        mStatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(LOG_TAG, "Stats Button");
                Intent myIntent = new Intent(MainActivity.this, StatsActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(LOG_TAG, "Send : onClick");
                takePicture();
            }
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
//                Log.e(LOG_TAG, "Light :" + event.values[0]);
                lightOn = event.values[0] > 100;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, mLight, SensorManager.SENSOR_DELAY_NORMAL);

        connectToPowerBi();

        textureView.setSurfaceTextureListener(textureListener);
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) { openCamera(); }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) { }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //This is called when the camera is open
            Log.e(LOG_TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private AuthenticationCallback<AuthenticationResult> getAuthInteractiveCallback() {
        return new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                if (authenticationResult == null || TextUtils.isEmpty(authenticationResult.getAccessToken())
                        || authenticationResult.getStatus() != AuthenticationResult.AuthenticationStatus.Succeeded) {
                    Log.e(LOG_TAG, "Authentication Result is invalid");
                    mAcquireTokenHandler.sendEmptyMessage(MSG_INTERACTIVE_SIGN_IN_PROMPT_AUTO);
                    return;
                }
                /* Successfully got a token, call graph now */
                Log.e(LOG_TAG, "Successfully authenticated");
                Log.e(LOG_TAG, "ID Token: " + authenticationResult.getIdToken());
                Log.e(LOG_TAG, "Access Token: " + authenticationResult.getAccessToken());
                mAuthResult = authenticationResult;
                sIntSignInInvoked.set(false);
                sendToPowerBi("", 1);
            }

            @Override
            public void onError(Exception exc) {
                Log.e(LOG_TAG, "Authentication failed: " + exc.toString());
                sIntSignInInvoked.set(false);
            }
        };
    }

    /* Handler to do an interactive sign in and acquire token */
    private Handler mAcquireTokenHandler;
    /* Boolean variable to ensure invocation of interactive sign-in only once in case of multiple  acquireTokenSilent call failures */
    private static AtomicBoolean sIntSignInInvoked = new AtomicBoolean();
    /* Constant to send message to the mAcquireTokenHandler to do acquire token with Prompt.Auto*/
    private static final int MSG_INTERACTIVE_SIGN_IN_PROMPT_AUTO = 1;
    /* Constant to send message to the mAcquireTokenHandler to do acquire token with Prompt.Always */
    private static final int MSG_INTERACTIVE_SIGN_IN_PROMPT_ALWAYS = 2;
    private AuthenticationContext mAuthContext;
    private AuthenticationResult mAuthResult;
    private static final String url_capsule = "https://api.powerbi.com/v1.0/myorg/groups/f465833c-2d2c-40a0-ab99-955c04908d50/datasets/d4753956-95e1-4f84-9869-56a91917c228/tables/Capsule/rows";
    private static final String url_tank = "https://api.powerbi.com/v1.0/myorg/groups/f465833c-2d2c-40a0-ab99-955c04908d50/datasets/d4753956-95e1-4f84-9869-56a91917c228/tables/Tank/rows";
    private static final String url_container = "https://api.powerbi.com/v1.0/myorg/groups/f465833c-2d2c-40a0-ab99-955c04908d50/datasets/d4753956-95e1-4f84-9869-56a91917c228/tables/Container/rows";
    private static final String json_capsule = "{\"rows\": [{\"CoffeeType\": \"%s\", \"Quantity\":%d}]}";
    private static final String json_tank = "{\"rows\": [{\"ml\": %d}]}";
    private static final String json_container = "{\"rows\": [{\"UsedCapsule\": %d}]}";
    private static final String clientID = "68b7a579-f21f-409d-9620-0a14ea65e814";
    private static final String redirectUri = "https://dev.powerbi.com/Apps/SignInRedirect";
    private static final String resourceUri = "https://analysis.windows.net/powerbi/api";
    private static final String authorityUri = "https://login.windows.net/common/oauth2/authorize";

    public Activity getActivity() {
        return this;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(LOG_TAG, "onActivityResults");
        super.onActivityResult(requestCode, resultCode, data);
        mAuthContext.onActivityResult(requestCode, resultCode, data);
    }

    protected void connectToPowerBi() {
        Logger.getInstance().setExternalLogger(new Logger.ILogger() {
            @Override
            public void Log(String tag, String message, String additionalMessage,
                            Logger.LogLevel level, ADALError errorCode) {
                Log.e(LOG_TAG, message + " " + additionalMessage);
            }
        });

        mAuthContext = new AuthenticationContext(getApplicationContext(),
                authorityUri,false);

        mAcquireTokenHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                if( sIntSignInInvoked.compareAndSet(false, true)) {
                    if (msg.what == MSG_INTERACTIVE_SIGN_IN_PROMPT_AUTO){
                        mAuthContext.acquireToken(getActivity(), resourceUri, clientID, redirectUri, PromptBehavior.Auto, getAuthInteractiveCallback());
                    }else if(msg.what == MSG_INTERACTIVE_SIGN_IN_PROMPT_ALWAYS){
                        mAuthContext.acquireToken(getActivity(), resourceUri, clientID, redirectUri, PromptBehavior.Always, getAuthInteractiveCallback());
                    }
                }
            }
        };
        mAcquireTokenHandler.sendEmptyMessage(MSG_INTERACTIVE_SIGN_IN_PROMPT_AUTO);
    }

    protected void sendToPowerBi(String coffeetype, int coffeeSize) {
        new Thread(new Runnable() {

            @Override
            public void run() {

                OkHttpClient client = new OkHttpClient();

                String json = String.format(Locale.FRANCE, json_capsule, "EspressoForte", 10);
                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);

                Request request = new Request.Builder()
                        .url(url_capsule)
                        .addHeader("Authorization", "Bearer " + mAuthResult.getAccessToken())
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build();


                Response response = null;
                try {
                    Call call = client.newCall(request);
                    response = call.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (response == null) {
                    Log.e(LOG_TAG, "Unable to upload to server. (null)");
                } else if(!response.isSuccessful()) {
                    Log.e(LOG_TAG, "Unable to upload to server. (not Successful)");
                } else {
                    Log.e(LOG_TAG, "Upload was successful.");
                    try {
                        JSONObject reader = new JSONObject(response.body().string());
                        Log.e(LOG_TAG, reader.toString());
                        JSON_to_CoffeeType(reader);
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    protected void sendToRecon(final byte[] image) {
        new Thread(new Runnable() {

            @Override
            public void run() {

                OkHttpClient client = new OkHttpClient();

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("images_file", "pic.jpg",
                                RequestBody.create(MediaType.parse("image/jpeg"), image))
                        .addFormDataPart("owners", "me")
                        .addFormDataPart("threshold", "0.7")
                        .build();

                Request request = new Request.Builder()
                        .url("https://southcentralus.api.cognitive.microsoft.com/customvision/v2.0/Prediction/5ab5e3e9-ded8-4e1f-bbec-c4b77517a849/image")
                        .addHeader("Prediction-Key", "40dfbb9286d243b9b7af566c05631264")
                        .addHeader("Content-Type", "application/octet-stream")
                        .post(requestBody)
                        .build();


                Response response = null;
                try {
                    Call call = client.newCall(request);
                    response = call.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (response == null) {
                    Log.e(LOG_TAG, "Unable to upload to server. (null)");
                } else if(!response.isSuccessful()) {
                    Log.e(LOG_TAG, "Unable to upload to server. (not Successful)");
                } else {
                    Log.e(LOG_TAG, "Upload was successful.");
                    try {
                        JSONObject reader = new JSONObject(response.body().string());
                        Log.e(LOG_TAG, reader.toString());
                        JSON_to_CoffeeType(reader);
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(LOG_TAG, "cameraDevice is null");
            return;
        }
        try {
            int width = 640;
            int height = 480;
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    try (Image image = reader.acquireLatestImage()) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        sendToRecon(bytes);
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, "CameraAccessException");
            e.printStackTrace();
        }
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) { }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_ALL);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(LOG_TAG, "openCamera X");
    }

    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(LOG_TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(LOG_TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.e(LOG_TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void JSON_to_CoffeeType(JSONObject o) {
        String classe = "Unknown";
        try {
            JSONObject prediction = o.getJSONArray("predictions").getJSONObject(0);
            if (prediction.getDouble("probability") > 0.50) {
                classe = prediction.getString("tagName");
            }
            Log.e(LOG_TAG, "Classe : " + classe);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        editor = mSharedPref.edit();
        editor.putInt(classe, mSharedPref.getInt(classe,0) + 1);
        editor.apply();
        detected = classe;
    }
}


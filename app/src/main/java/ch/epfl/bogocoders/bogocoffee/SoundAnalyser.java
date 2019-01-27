package ch.epfl.bogocoders.bogocoffee;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class SoundAnalyser {

    private static final String LOG_TAG = "SoundAnalyser";
    private final Activity parent;

    private boolean running = false;

    private AudioDispatcher dispatcher = null;

    public enum CoffeeType {Unknown, Ristretto, Espresso, Lungo};
    private CoffeeType detected = CoffeeType.Unknown;

    public SoundAnalyser(Activity parent) {
        this.parent = parent;
    }

    private AudioDispatcher dispatcherFactory () {

        final AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050,1024,0);

//        dispatcher.addAudioProcessor(new BandPass(50, 50, 22050));
//        dispatcher.addAudioProcessor(new LowPassFS(100, 22050));
//        dispatcher.addAudioProcessor(new LowPassSP(100, 22050));
//        dispatcher.addAudioProcessor(new IIRFilter(100, 22050) {
//            @Override
//            protected void calcCoeff() {
//
//            }
//        });
//        dispatcher.addAudioProcessor(new AudioProcessor() {
//            double time = System.currentTimeMillis();
//            @Override
//            public boolean process(AudioEvent audioEvent) {
//                if (System.currentTimeMillis() - time > 2000) {
//                    Log.e(LOG_TAG, "Buffer size : " + audioEvent.getBufferSize());
//                    float[] buff = audioEvent.getFloatBuffer();
//                    StringBuffer vals = new StringBuffer();
//                    vals.append("{");
//                    for(float val : buff) {
//                        vals.append(",");
//                        vals.append(val);
//                    }
//                    vals.append("}");
//                    Log.e(LOG_TAG, "Buffer values : " + vals.toString());
//                    time = System.currentTimeMillis();
//                }
//
//                return true;
//            }
//
//            @Override
//            public void processingFinished() {
//
//            }
//        });
        detected = CoffeeType.Unknown;
        dispatcher.addAudioProcessor (
                new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024,
                        new PitchDetectionHandler() {
                            double timeLapsed = System.currentTimeMillis();
                            double timeStart = -1;
                            @Override
                            public void handlePitch(PitchDetectionResult pitchDetectionResult,
                                                    AudioEvent audioEvent) {
                                final float pitchInHz = pitchDetectionResult.getPitch();
                                if (System.currentTimeMillis() - timeLapsed > 500) {
                                    Log.e(LOG_TAG, "Pitch : " + pitchInHz);
                                    timeLapsed = System.currentTimeMillis();
//                                    parent.runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            if (49 < pitchInHz && pitchInHz <= 51) {
//                                                Toast.makeText(parent, "cafe", Toast.LENGTH_SHORT).show();
//                                            } else {
//                                                Toast.makeText(parent, "pas cafe", Toast.LENGTH_SHORT).show();
//                                            }
//                                        }
//                                    });
                                    if (49 < pitchInHz && pitchInHz <= 51) {
                                        timeStart = (timeStart == -1)? System.currentTimeMillis() : timeStart;
                                    } else {
                                        if (timeStart != -1) {
                                            double time = (System.currentTimeMillis() - timeStart)/1000;
                                            if( 10 < time && time < 12) {
                                                detected = CoffeeType.Ristretto;
                                            } else if( 14 < time && time < 16 ) {
                                                detected = CoffeeType.Espresso;
                                            } else if( 33 < time && time < 35 ) {
                                                detected = CoffeeType.Lungo;
                                            }
                                            dispatcher.stop();
                                            running = false;
                                        }
                                    }
                                }
                            }
                        }));
        return dispatcher;
    }

    public void start() {
        if (!running) {
            Log.e(LOG_TAG, "Start sound analyser");
            dispatcher = dispatcherFactory();
            new Thread(dispatcher, "Audio Dispatcher").start();
            running = true;
        }
    }

    public void stop() {
        if(running) {
            Log.e(LOG_TAG, "Stop sound analyser");
            dispatcher.stop();
            running = false;
        }
    }

    public CoffeeType getDetected() {
        return detected;
    }
}

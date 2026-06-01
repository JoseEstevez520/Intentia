package io.yourPath.audio;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

public class WavGenerator {

    private static final int SAMPLE_RATE = 22050;
    private static final Random random = new Random(42);

    public static byte[] click() {
        int samples = (int)(0.035f * SAMPLE_RATE);
        short[] data = new short[samples];
        for (int i = 0; i < samples; i++) {
            float t = (float)i / SAMPLE_RATE;
            float attack = Math.min(t * 200f, 1f);
            float decay = (float)Math.exp(-t * 80f);
            float envelope = attack * decay;
            float noise = (random.nextFloat() * 2f - 1f) * 0.5f;
            float tone = (float)Math.sin(2 * Math.PI * 600 * t) * 0.5f;
            float sample = (noise + tone) * envelope;
            data[i] = (short)(sample * Short.MAX_VALUE * 0.3);
        }
        return toWavBytes(data, SAMPLE_RATE);
    }

    public static byte[] paso(boolean izquierda) {
        int samples = (int)(0.05f * SAMPLE_RATE);
        short[] data = new short[samples];
        for (int i = 0; i < samples; i++) {
            float t = (float)i / SAMPLE_RATE;
            float envelope = (float)Math.exp(-t * 80f);
            float noise = (random.nextFloat() * 2f - 1f) * 0.85f;
            float thump = (float)Math.sin(2 * Math.PI * 45 * t) * 0.15f;
            float sample = (noise + thump) * envelope;
            data[i] = (short)(sample * Short.MAX_VALUE * 0.005);
        }
        return toWavBytes(data, SAMPLE_RATE);
    }

    public static byte[] interact() {
        int samples = (int)(0.06f * SAMPLE_RATE);
        short[] data = new short[samples];
        for (int i = 0; i < samples; i++) {
            float t = (float)i / SAMPLE_RATE;
            float freq = 200 + t * 3000f;
            float envelope = (float)Math.exp(-t * 50f);
            float sample = (float)Math.sin(2 * Math.PI * freq * t) * envelope;
            data[i] = (short)(sample * Short.MAX_VALUE * 0.2);
        }
        return toWavBytes(data, SAMPLE_RATE);
    }

    public static byte[] typewriter() {
        int samples = (int)(0.006f * SAMPLE_RATE);
        short[] data = new short[samples];
        for (int i = 0; i < samples; i++) {
            float t = (float)i / SAMPLE_RATE;
            float envelope = (float)Math.exp(-t * 1200f);
            float noise = (random.nextFloat() * 2f - 1f) * 0.7f;
            float thump = (float)Math.sin(2 * Math.PI * 120 * t) * 0.3f;
            float sample = (noise + thump) * envelope;
            data[i] = (short)(sample * Short.MAX_VALUE * 0.18);
        }
        return toWavBytes(data, SAMPLE_RATE);
    }

    public static byte[] page() {
        int samples = (int)(0.08f * SAMPLE_RATE);
        short[] data = new short[samples];
        for (int i = 0; i < samples; i++) {
            float t = (float)i / SAMPLE_RATE;
            float freq = 400 - t * 2500f;
            float noise = (random.nextFloat() * 2f - 1f) * 0.3f;
            float envelope = (float)Math.exp(-t * 30f);
            float sample = ((float)Math.sin(2 * Math.PI * freq * t) * 0.7f + noise) * envelope;
            data[i] = (short)(sample * Short.MAX_VALUE * 0.25);
        }
        return toWavBytes(data, SAMPLE_RATE);
    }

    public static byte[] confirm() {
        int samples = (int)(0.1f * SAMPLE_RATE);
        short[] data = new short[samples];
        for (int i = 0; i < samples; i++) {
            float t = (float)i / SAMPLE_RATE;
            float freq = 300 + t * 3000f;
            float envelope = (float)Math.exp(-t * 20f);
            float sample = (float)Math.sin(2 * Math.PI * freq * t) * envelope;
            data[i] = (short)(sample * Short.MAX_VALUE * 0.35);
        }
        return toWavBytes(data, SAMPLE_RATE);
    }

    public static byte[] deny() {
        int samples = (int)(0.15f * SAMPLE_RATE);
        short[] data = new short[samples];
        for (int i = 0; i < samples; i++) {
            float t = (float)i / SAMPLE_RATE;
            float freq = 300 - t * 1500f;
            float envelope = (float)Math.exp(-t * 15f);
            float sample = (float)Math.sin(2 * Math.PI * freq * t) * envelope;
            data[i] = (short)(sample * Short.MAX_VALUE * 0.3);
        }
        return toWavBytes(data, SAMPLE_RATE);
    }

    private static byte[] toWavBytes(short[] data, int sampleRate) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            int dataSize = data.length * 2;

            dos.writeBytes("RIFF");
            dos.writeInt(Integer.reverseBytes(36 + dataSize));
            dos.writeBytes("WAVE");
            dos.writeBytes("fmt ");
            dos.writeInt(Integer.reverseBytes(16));
            dos.writeShort(Short.reverseBytes((short)1));
            dos.writeShort(Short.reverseBytes((short)1));
            dos.writeInt(Integer.reverseBytes(sampleRate));
            dos.writeInt(Integer.reverseBytes(sampleRate * 2));
            dos.writeShort(Short.reverseBytes((short)2));
            dos.writeShort(Short.reverseBytes((short)16));
            dos.writeBytes("data");
            dos.writeInt(Integer.reverseBytes(dataSize));
            for (short s : data) {
                dos.writeShort(Short.reverseBytes(s));
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}

package com.bytehealers.healverse.service;

import java.io.InputStream;

public interface SpeechToTextService {
    /**
     * Convert audio input stream to text
     * @param audioStream the audio input stream
     * @param audioFormat the format of the audio (wav, mp3, webm)
     * @return the transcribed text
     */
    String convertSpeechToText(InputStream audioStream, String audioFormat);
    
    /**
     * Convert base64 encoded audio to text
     * @param audioData base64 encoded audio data
     * @param audioFormat the format of the audio (wav, mp3, webm)
     * @return the transcribed text
     */
    String convertSpeechToText(String audioData, String audioFormat);
}
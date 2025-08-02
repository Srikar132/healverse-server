package com.bytehealers.healverse.service;

public interface TextToSpeechService {
    /**
     * Convert text to speech and return as base64 encoded audio
     * @param text the text to convert
     * @param voiceType the voice type (optional)
     * @param audioFormat the desired audio format (wav, mp3)
     * @return base64 encoded audio data
     */
    String convertTextToSpeech(String text, String voiceType, String audioFormat);
    
    /**
     * Convert text to speech with default settings
     * @param text the text to convert
     * @return base64 encoded audio data in WAV format
     */
    String convertTextToSpeech(String text);
}
package com.def.max.Models;

public class TranscriptionResultsItemsAlternatives
{
    private String confidence;
    private String content;

    public TranscriptionResultsItemsAlternatives()
    {

    }

    public TranscriptionResultsItemsAlternatives(String confidence, String content) {
        this.confidence = confidence;
        this.content = content;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

package com.def.max.Models;

import java.util.List;

public class TranscriptionResults
{
    private List<TranscriptionResultsTranscripts> transcripts;
    private List<TranscriptionResultsItems> items;

    public TranscriptionResults() {
    }

    public TranscriptionResults(List<TranscriptionResultsTranscripts> transcripts, List<TranscriptionResultsItems> items) {
        this.transcripts = transcripts;
        this.items = items;
    }

    public List<TranscriptionResultsTranscripts> getTranscripts() {
        return transcripts;
    }

    public void setTranscripts(List<TranscriptionResultsTranscripts> transcripts) {
        this.transcripts = transcripts;
    }

    public List<TranscriptionResultsItems> getItems() {
        return items;
    }

    public void setItems(List<TranscriptionResultsItems> items) {
        this.items = items;
    }
}

package com.def.max.Models;

import java.util.List;

public class TranscriptionResultsItems
{
    private String start_time;
    private String end_time;

    private List<TranscriptionResultsItemsAlternatives> alternatives;

    private String type;

    public TranscriptionResultsItems() {
    }

    public TranscriptionResultsItems(String start_time, String end_time, List<TranscriptionResultsItemsAlternatives> alternatives, String type) {
        this.start_time = start_time;
        this.end_time = end_time;
        this.alternatives = alternatives;
        this.type = type;
    }

    public String getStart_time() {
        return start_time;
    }

    public void setStart_time(String start_time) {
        this.start_time = start_time;
    }

    public String getEnd_time() {
        return end_time;
    }

    public void setEnd_time(String end_time) {
        this.end_time = end_time;
    }

    public List<TranscriptionResultsItemsAlternatives> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<TranscriptionResultsItemsAlternatives> alternatives) {
        this.alternatives = alternatives;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

package com.def.max.Models;

public class TranscriptionModel
{
    private String jobName;
    private String accountId;

    private TranscriptionResults results;

    private String status;

    public TranscriptionModel() {
    }

    public TranscriptionModel(String jobName, String accountId, TranscriptionResults results, String status) {
        this.jobName = jobName;
        this.accountId = accountId;
        this.results = results;
        this.status = status;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public TranscriptionResults getResults() {
        return results;
    }

    public void setResults(TranscriptionResults results) {
        this.results = results;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

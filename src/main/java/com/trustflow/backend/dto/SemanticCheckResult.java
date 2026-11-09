package com.trustflow.backend.dto;

public class SemanticCheckResult {
    private double similarity_score;
    private boolean is_tampered;

    public SemanticCheckResult() {}

    public SemanticCheckResult(double similarityScore, boolean isTampered) {
        this.similarity_score = similarityScore;
        this.is_tampered = isTampered;
    }

    public double getSimilarity_score() { return similarity_score; }
    public void setSimilarity_score(double similarity_score) { this.similarity_score = similarity_score; }

    public boolean isIs_tampered() { return is_tampered; }
    public void setIs_tampered(boolean is_tampered) { this.is_tampered = is_tampered; }
}

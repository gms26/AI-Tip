package com.aitip.dto;

import java.time.LocalDateTime;
import java.util.List;

public class TipCoachResponse {
    
    private LocalDateTime generatedAt;
    private TipCoachFocus focus;
    private int focusPriority;
    private String headline;
    private String summary;
    private String nextAction;

    private TipProfileResponse profileSummary;
    private List<TipGoalProgressResponse> goalSummary;
    private TipBudgetStatusResponse budgetSummary;
    private TipForecastResponse forecastSummary;
    private List<TipRecommendation> recommendations;
    private DataQualitySummary dataQualitySummary;
    private String aiExplanation;

    public static class DataQualitySummary {
        private long highSeverityCount;
        private long warningCount;
        private long infoCount;

        public DataQualitySummary(long highSeverityCount, long warningCount, long infoCount) {
            this.highSeverityCount = highSeverityCount;
            this.warningCount = warningCount;
            this.infoCount = infoCount;
        }

        public long getHighSeverityCount() { return highSeverityCount; }
        public void setHighSeverityCount(long highSeverityCount) { this.highSeverityCount = highSeverityCount; }
        public long getWarningCount() { return warningCount; }
        public void setWarningCount(long warningCount) { this.warningCount = warningCount; }
        public long getInfoCount() { return infoCount; }
        public void setInfoCount(long infoCount) { this.infoCount = infoCount; }
    }

    public TipCoachResponse() {
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public TipCoachFocus getFocus() {
        return focus;
    }

    public void setFocus(TipCoachFocus focus) {
        this.focus = focus;
    }

    public int getFocusPriority() {
        return focusPriority;
    }

    public void setFocusPriority(int focusPriority) {
        this.focusPriority = focusPriority;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    public TipProfileResponse getProfileSummary() {
        return profileSummary;
    }

    public void setProfileSummary(TipProfileResponse profileSummary) {
        this.profileSummary = profileSummary;
    }

    public List<TipGoalProgressResponse> getGoalSummary() {
        return goalSummary;
    }

    public void setGoalSummary(List<TipGoalProgressResponse> goalSummary) {
        this.goalSummary = goalSummary;
    }

    public TipBudgetStatusResponse getBudgetSummary() {
        return budgetSummary;
    }

    public void setBudgetSummary(TipBudgetStatusResponse budgetSummary) {
        this.budgetSummary = budgetSummary;
    }

    public TipForecastResponse getForecastSummary() {
        return forecastSummary;
    }

    public void setForecastSummary(TipForecastResponse forecastSummary) {
        this.forecastSummary = forecastSummary;
    }

    public List<TipRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<TipRecommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public DataQualitySummary getDataQualitySummary() {
        return dataQualitySummary;
    }

    public void setDataQualitySummary(DataQualitySummary dataQualitySummary) {
        this.dataQualitySummary = dataQualitySummary;
    }

    public String getAiExplanation() {
        return aiExplanation;
    }

    public void setAiExplanation(String aiExplanation) {
        this.aiExplanation = aiExplanation;
    }
}

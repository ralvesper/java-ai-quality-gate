package com.ralvesper.qualitygate;

public interface AiReviewProvider {
    AiReviewResult review(ReviewContext context) throws Exception;
}

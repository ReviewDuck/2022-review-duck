package com.reviewduck.acceptance;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.reviewduck.dto.request.AnswerRequest;
import com.reviewduck.dto.request.QuestionRequest;
import com.reviewduck.dto.request.ReviewFormCreateRequest;
import com.reviewduck.dto.request.ReviewRequest;
import com.reviewduck.dto.response.QuestionResponse;
import com.reviewduck.dto.response.ReviewFormCodeResponse;
import com.reviewduck.dto.response.ReviewFormResponse;
import com.reviewduck.dto.response.ReviewsFindResponse;

public class ReviewAcceptanceTest extends AcceptanceTest {

    private final Long invalidReviewId = 99L;

    @Test
    @DisplayName("회고를 수정한다.")
    void editReview() {
        Long reviewId = saveReviewAndGetId();

        //when, then
        ReviewRequest editRequest = new ReviewRequest("제이슨",
            List.of(new AnswerRequest(1L, "editedAnswer1"), new AnswerRequest(2L, "editedAnswer2")));

        put("/api/reviews/" + reviewId, editRequest)
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    @DisplayName("존재하지 않는 회고를 수정할 수 없다.")
    void failToEditReview() {
        // when, then
        ReviewRequest createRequest = new ReviewRequest("제이슨",
            List.of(new AnswerRequest(1L, "answer1"), new AnswerRequest(2L, "answer2")));
        put("/api/reviews/" + invalidReviewId, createRequest)
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("회고를 삭제한다.")
    void deleteReview() {
        // given
        String reviewTitle = "title";
        List<QuestionRequest> questions = List.of(new QuestionRequest("question1"),
            new QuestionRequest("question2"));
        String code = createReviewFormAndGetCode(reviewTitle, questions);
        ReviewRequest createRequest = new ReviewRequest("제이슨",
            List.of(new AnswerRequest(1L, "answer1"), new AnswerRequest(2L, "answer2")));
        post("/api/review-forms/" + code, createRequest);

        ReviewsFindResponse response = get("/api/review-forms/" + code + "/reviews")
            .extract()
            .as(ReviewsFindResponse.class);

        Long reviewId = saveReviewAndGetId();

        //when, then
        delete("/api/reviews/" + reviewId)
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    @DisplayName("존재하지 않는 회고를 삭제할 수 없다.")
    void failToDeleteReview() {
        // when, then
        delete("/api/reviews/" + invalidReviewId)
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    private Long saveReviewAndGetId() {
        String reviewTitle = "title";
        List<QuestionRequest> questions = List.of(new QuestionRequest("question1"),
            new QuestionRequest("question2"));

        String code = createReviewFormAndGetCode(reviewTitle, questions);
        List<QuestionResponse> questionsInReviewForm = get("/api/review-forms/" + code)
            .extract()
            .as(ReviewFormResponse.class).getQuestions();

        ReviewRequest createRequest = new ReviewRequest("제이슨",
            List.of(new AnswerRequest(questionsInReviewForm.get(0).getQuestionId(), "answer1"),
                new AnswerRequest(questionsInReviewForm.get(1).getQuestionId(), "answer2")));
        post("/api/review-forms/" + code, createRequest);

        return get("/api/review-forms/" + code + "/reviews")
            .extract()
            .as(ReviewsFindResponse.class).getReviews()
            .get(0)
            .getReviewId();
    }

    private String createReviewFormAndGetCode(String reviewTitle, List<QuestionRequest> questions) {
        // given
        ReviewFormCreateRequest request = new ReviewFormCreateRequest(reviewTitle, questions);

        // when, then
        return post("/api/review-forms", request)
            .extract()
            .as(ReviewFormCodeResponse.class)
            .getReviewFormCode();
    }
}

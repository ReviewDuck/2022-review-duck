package com.reviewduck.acceptance;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.reviewduck.dto.request.AnswerRequest;
import com.reviewduck.dto.request.QuestionRequest;
import com.reviewduck.dto.request.QuestionUpdateRequest;
import com.reviewduck.dto.request.ReviewFormCreateRequest;
import com.reviewduck.dto.request.ReviewFormUpdateRequest;
import com.reviewduck.dto.request.ReviewRequest;
import com.reviewduck.dto.response.ReviewFormCodeResponse;
import com.reviewduck.dto.response.ReviewFormResponse;
import com.reviewduck.dto.response.ReviewResponse;
import com.reviewduck.dto.response.ReviewsFindResponse;

public class ReviewFormAcceptanceTest extends AcceptanceTest {
    private final String invalidCode = "aaaaaaaa";

    @Test
    @DisplayName("회고 폼을 생성한다.")
    void createReviewForm() {
        // given
        String reviewTitle = "title";
        List<QuestionRequest> questions = List.of(new QuestionRequest("question1"),
            new QuestionRequest("question2"));
        ReviewFormCreateRequest request = new ReviewFormCreateRequest(reviewTitle, questions);

        // when, then
        post("/api/review-forms", request).statusCode(HttpStatus.CREATED.value())
            .assertThat().body("reviewFormCode", notNullValue());
    }

    @Test
    @DisplayName("회고폼을 조회한다.")
    void findReviewForm() {
        // given
        String reviewTitle = "title";
        List<QuestionRequest> questions = List.of(new QuestionRequest("question1"),
            new QuestionRequest("question2"));
        String reviewFormCode = createReviewFormAndGetCode(reviewTitle, questions);

        // when
        ReviewFormResponse response = get("/api/review-forms/" + reviewFormCode)
            .statusCode(HttpStatus.OK.value())
            .extract()
            .as(ReviewFormResponse.class);

        // then
        assertAll(
            () -> assertThat(response.getReviewTitle()).isEqualTo(reviewTitle),
            () -> assertThat(response.getQuestions()).hasSize(2)
        );
    }

    @Test
    @DisplayName("회고폼 조회에 실패한다.")
    void failToFindReviewForm() {
        // when, then
        get("/api/review-forms/" + "AAAAAAAA")
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("회고폼을 수정한다.")
    void updateReviewForm() {
        // given
        List<QuestionRequest> createQuestions = List.of(new QuestionRequest("question1"),
            new QuestionRequest("question2"));
        String createReviewFormCode = createReviewFormAndGetCode("title", createQuestions);

        // when, then
        String newReviewTitle = "new title";
        List<QuestionUpdateRequest> updateQuestions = List.of(new QuestionUpdateRequest(1L, "new question1"));
        ReviewFormUpdateRequest updateRequest = new ReviewFormUpdateRequest(newReviewTitle, updateQuestions);
        put("/api/review-forms/" + createReviewFormCode, updateRequest)
            .statusCode(HttpStatus.OK.value())
            .assertThat().body("reviewFormCode", equalTo(createReviewFormCode));

        ReviewFormResponse getResponse = get("/api/review-forms/" + createReviewFormCode)
            .extract()
            .as(ReviewFormResponse.class);
        assertAll(
            () -> assertThat(getResponse.getReviewTitle()).isEqualTo(newReviewTitle),
            () -> assertThat(getResponse.getQuestions()).hasSize(1),
            () -> assertThat(getResponse.getQuestions().get(0).getQuestionId()).isEqualTo(1L),
            () -> assertThat(getResponse.getQuestions().get(0).getQuestionValue()).isEqualTo("new question1")
        );
    }

    @Test
    @DisplayName("존재하지 않는 회고폼을 수정할 수 없다.")
    void updateInvalidReviewForm() {
        // when, then
        List<QuestionUpdateRequest> updateQuestions = List.of(new QuestionUpdateRequest(1L, "new question1"));
        ReviewFormUpdateRequest updateRequest = new ReviewFormUpdateRequest("newTitle", updateQuestions);

        put("/api/review-forms/aaaaaaaa", updateRequest)
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    private String createReviewFormAndGetCode(String reviewTitle, List<QuestionRequest> questions) {
        ReviewFormCreateRequest request = new ReviewFormCreateRequest(reviewTitle, questions);

        return post("/api/review-forms", request)
            .extract()
            .as(ReviewFormCodeResponse.class)
            .getReviewFormCode();
    }

    @Test
    @DisplayName("회고를 생성한다.")
    void createReview() {
        // given
        String reviewTitle = "title";
        List<QuestionRequest> questions = List.of(new QuestionRequest("question1"),
            new QuestionRequest("question2"));
        String code = createReviewFormAndGetCode(reviewTitle, questions);

        // when, then
        // 질문조회
        assertReviewTitleFromFoundReviewForm(code, reviewTitle);

        // 리뷰생성
        ReviewRequest createRequest = new ReviewRequest("제이슨",
            List.of(new AnswerRequest(1L, "answer1"), new AnswerRequest(2L, "answer2")));
        post("/api/review-forms/" + code, createRequest)
            .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    @DisplayName("존재하지 않는 질문에 대해 답변을 작성하면 회고 작성에 실패한다.")
    void createReviewWithNotExistQuestion() {
        // given
        String reviewTitle = "title";
        List<QuestionRequest> questions = List.of(new QuestionRequest("question1"));
        String code = createReviewFormAndGetCode(reviewTitle, questions);

        // when, then
        // 질문조회
        assertReviewTitleFromFoundReviewForm(code, reviewTitle);

        // 리뷰생성
        ReviewRequest createRequest = new ReviewRequest("제이슨",
            List.of(new AnswerRequest(1L, "answer1"), new AnswerRequest(2L, "answer2")));
        post("/api/review-forms/" + code, createRequest)
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("회고 폼에 없는 질문에 대해 답변을 작성하면 회고 작성에 실패한다.")
    void createReviewWithQuestionNotInReviewForm() {
        // given
        String reviewTitle = "title";
        List<QuestionRequest> questions = List.of(new QuestionRequest("question1"));
        String code = createReviewFormAndGetCode(reviewTitle, questions);

        List<QuestionRequest> dummyQuestions = List.of(new QuestionRequest("dummy question"));
        createReviewFormAndGetCode("dummy title", dummyQuestions);

        // when, then
        // 질문조회
        assertReviewTitleFromFoundReviewForm(code, reviewTitle);

        // 리뷰생성
        Long questionIdInReviewForm = 1L;
        Long questionIdNotInReviewForm = 2L;
        ReviewRequest createRequest = new ReviewRequest("제이슨",
            List.of(new AnswerRequest(questionIdInReviewForm, "answer1"),
                new AnswerRequest(questionIdNotInReviewForm, "answer2")));
        post("/api/review-forms/" + code, createRequest)
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("특정 회고 폼에 속한 회고 전체를 조회한다.")
    void findReviews() {
        // given
        String reviewTitle = "title";
        List<QuestionRequest> questions = List.of(new QuestionRequest("question1"),
            new QuestionRequest("question2"));
        String code = createReviewFormAndGetCode(reviewTitle, questions);

        ReviewRequest createRequest = new ReviewRequest("제이슨",
            List.of(new AnswerRequest(1L, "answer1"), new AnswerRequest(2L, "answer2")));
        post("/api/review-forms/" + code, createRequest);

        // when
        ReviewsFindResponse response = get("/api/review-forms/" + code + "/reviews")
            .statusCode(HttpStatus.OK.value())
            .extract()
            .as(ReviewsFindResponse.class);

        ReviewResponse reviewResponse = response.getReviews().get(0);

        // then
        assertAll(
            () -> assertThat(response.getReviewFormTitle()).isEqualTo(reviewTitle),
            () -> assertThat(reviewResponse.getNickname()).isEqualTo("제이슨"),
            () -> assertThat(reviewResponse.getAnswers()).hasSize(2)
        );
    }

    @Test
    @DisplayName("존재하지 회고 폼 코드에 대해 회고를 조회할 수 없다.")
    void findReviewsWithInvalidCode() {
        // when, then
        get("/api/review-forms/" + invalidCode + "/reviews")
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    private void assertReviewTitleFromFoundReviewForm(String code, String reviewTitle) {
        ReviewFormResponse reviewFormResponse = get("/api/review-forms/" + code)
            .statusCode(HttpStatus.OK.value())
            .extract()
            .as(ReviewFormResponse.class);
        assertThat(reviewFormResponse.getReviewTitle()).isEqualTo(reviewTitle);
    }
}

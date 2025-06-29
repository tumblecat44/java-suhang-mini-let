package com.dgsw.javasuhangminilet.review.service;

import com.dgsw.javasuhangminilet.auth.entity.UserEntity;
import com.dgsw.javasuhangminilet.auth.repository.AuthRepository;
import com.dgsw.javasuhangminilet.review.dto.ReviewDTO;
import com.dgsw.javasuhangminilet.review.dto.request.UpdateReviewRequest;
import com.dgsw.javasuhangminilet.review.dto.response.ReviewResponse;
import com.dgsw.javasuhangminilet.review.entity.ReviewEntity;
import com.dgsw.javasuhangminilet.review.repository.ReviewRepository;
import com.dgsw.javasuhangminilet.util.BaseResponse;
import com.dgsw.javasuhangminilet.util.ResponseCode;
import com.dgsw.javasuhangminilet.util.TokenClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final AuthRepository authRepository;
    private final TokenClient tokenClient;

    public BaseResponse<ReviewResponse> addReview(ReviewDTO dto) {
        Optional<UserEntity> user;
        try {
            user = getUserFromToken(dto.getToken());
        } catch (Exception e) {
            return BaseResponse.error(ResponseCode.FORBIDDEN, "접근이 불가능합니다.");
        }
        if (user.isEmpty()) {
            return BaseResponse.error(ResponseCode.NOT_FOUND, "그런 사람 또 없습니다.");
        }
        ReviewEntity savedEntity = reviewRepository.save(
                ReviewEntity.builder()
                        .user(user.get())
                        .title(dto.getTitle())
                        .content(dto.getContent())
                        .build()
        );
        ReviewResponse response = new ReviewResponse(
                savedEntity.getId(),
                savedEntity.getUser().getId(),
                savedEntity.getTitle(),
                savedEntity.getContent()
        );
        return BaseResponse.success(response);
    }


    public BaseResponse<List<ReviewResponse>> getAllReviews(String token) {
        try {
            getUserFromToken(token);
        } catch (Exception e) {
            return BaseResponse.error(ResponseCode.FORBIDDEN, "권한이 없습니다.");
        }
        try {
            List<ReviewResponse> reviews = reviewRepository.findAll().stream()
                    .map(entity -> new ReviewResponse(
                            entity.getId(),
                            entity.getUser().getId(),
                            entity.getTitle(),
                            entity.getContent()
                    ))
                    .collect(Collectors.toList());
            return BaseResponse.success(reviews);
        } catch (Exception e) {
            return BaseResponse.error(ResponseCode.FORBIDDEN, "권한이 없습니다.");
        }

    }

    public BaseResponse<ResponseCode> updateReview(Long id, UpdateReviewRequest dto, String token) {
        Optional<ReviewEntity> optional = reviewRepository.findById(id);
        if (optional.isEmpty()) {
            return BaseResponse.error(ResponseCode.NOT_FOUND, "존재하지 않음");
        }
        ReviewEntity existingReview = optional.get();
        if (existingReview.getUser().getId() != getUserFromToken(token).get().getId()) {
            return BaseResponse.error(ResponseCode.FORBIDDEN, "본인 글만 수정 가능합니다.");
        }
        existingReview.setTitle(dto.getTitle());
        existingReview.setContent(dto.getContent());
        reviewRepository.save(existingReview);
        return BaseResponse.success(ResponseCode.SUCCESS, "updated");
    }

    public BaseResponse<ResponseCode> deleteReview(Long id, String token) {
        Optional<ReviewEntity> optional = reviewRepository.findById(id);
        if (optional.isEmpty()) {
            return BaseResponse.error(ResponseCode.NOT_FOUND, "존재하지 않음");
        }
        ReviewEntity existingReview = optional.get();
        if(existingReview.getUser().getId() != getUserFromToken(token).get().getId()){
            return BaseResponse.error(ResponseCode.FORBIDDEN, "본인 글만 삭제 가능합니다.");
        }
        reviewRepository.delete(existingReview);
        return BaseResponse.success(ResponseCode.SUCCESS, "deleted");
    }

    private Optional<UserEntity> getUserFromToken(String token) {
        try {
            Long userId = tokenClient.extractUserIdFromToken(token);
            return authRepository.findById(userId);
        } catch (Exception e) {
            log.error("토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }
    }
}

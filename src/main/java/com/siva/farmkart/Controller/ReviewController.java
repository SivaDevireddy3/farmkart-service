package com.siva.farmkart.Controller;

import com.siva.farmkart.Entity.Mango;
import com.siva.farmkart.Entity.Review;
import com.siva.farmkart.Repos.MangoRepository;
import com.siva.farmkart.Repos.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private MangoRepository mangoRepository;

    // ── PUBLIC: Get approved reviews for a mango ─────────────────────────────

    @GetMapping("/mango/{mangoId}")
    public ResponseEntity<List<Review>> getForMango(@PathVariable Long mangoId) {
        return ResponseEntity.ok(
                reviewRepository.findByMango_IdAndApprovedTrueOrderByCreatedAtDesc(mangoId)
        );
    }

    // ── PUBLIC: Submit a review ───────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody Map<String, Object> body) {
        try {
            Object mangoIdObj = body.get("mangoId");
            if (mangoIdObj == null)
                return ResponseEntity.badRequest().body(Map.of("message", "mangoId is required"));

            Long mangoId = mangoIdObj instanceof Integer
                    ? ((Integer) mangoIdObj).longValue()
                    : Long.parseLong(mangoIdObj.toString());

            Mango mango = mangoRepository.findById(mangoId).orElse(null);
            if (mango == null)
                return ResponseEntity.badRequest().body(Map.of("message", "Mango not found"));

            Object ratingObj = body.get("rating");
            if (ratingObj == null)
                return ResponseEntity.badRequest().body(Map.of("message", "rating is required"));

            int rating = ratingObj instanceof Integer
                    ? (Integer) ratingObj
                    : Integer.parseInt(ratingObj.toString());

            if (rating < 1 || rating > 5)
                return ResponseEntity.badRequest().body(Map.of("message", "Rating must be between 1 and 5"));

            Review review = Review.builder()
                    .mango(mango)
                    .rating(rating)
                    .comment(body.getOrDefault("comment", "").toString())
                    .reviewerName(body.getOrDefault("reviewerName", "").toString())
                    .reviewerPhone(body.getOrDefault("reviewerPhone", "").toString())
                    .approved(true)   // auto-approve so ratings show immediately
                    .build();

            reviewRepository.save(review);

            // Recalculate average rating from ALL approved reviews
            Double avg = reviewRepository.findAvgRatingByMangoId(mangoId);
            Long count = reviewRepository.countByMangoId(mangoId);
            mango.setAvgRating(avg != null
                    ? new java.math.BigDecimal(String.format("%.2f", avg))
                    : java.math.BigDecimal.ZERO);
            mango.setReviewCount(count != null ? count.intValue() : 0);
            mangoRepository.save(mango);

            return ResponseEntity.ok(Map.of(
                    "message", "Review submitted successfully! ⭐",
                    "reviewId", review.getId(),
                    "avgRating", mango.getAvgRating() != null ? mango.getAvgRating() : java.math.BigDecimal.ZERO,
                    "reviewCount", mango.getReviewCount() != null ? mango.getReviewCount() : 0
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ── ADMIN/SELLER: Get pending reviews ────────────────────────────────────

    @GetMapping("/pending")
    public ResponseEntity<List<Review>> getPending() {
        return ResponseEntity.ok(reviewRepository.findByApprovedFalseOrderByCreatedAtDesc());
    }

    // ── ADMIN/SELLER: Approve a review ───────────────────────────────────────

    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        return reviewRepository.findById(id).map(review -> {
            review.setApproved(true);
            reviewRepository.save(review);

            // Recalculate avg rating for the mango
            Long mangoId = review.getMango().getId();
            Double avg = reviewRepository.findAvgRatingByMangoId(mangoId);
            Long count = reviewRepository.countByMangoId(mangoId);
            Mango mango = review.getMango();
            mango.setAvgRating(avg != null
                    ? new java.math.BigDecimal(String.format("%.2f", avg))
                    : java.math.BigDecimal.ZERO);
            mango.setReviewCount(count != null ? count.intValue() : 0);
            mangoRepository.save(mango);

            return ResponseEntity.ok(Map.of("message", "Review approved"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── ADMIN/SELLER: Delete a review ────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return reviewRepository.findById(id).map(review -> {
            reviewRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Review deleted"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
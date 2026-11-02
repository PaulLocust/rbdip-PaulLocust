package com.rbdip.bookstore.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rbdip.bookstore.purchase.PurchaseHistory;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewServiceTest {
    private final ReviewRepository reviews = mock(ReviewRepository.class);
    private final PurchaseHistory purchases = mock(PurchaseHistory.class);
    private final ReviewService service = new ReviewService(reviews, purchases);

    @Test
    void keepsReviewsAvailableToBuyersAndVisitors() {
        when(reviews.save(any())).thenAnswer(call -> call.getArgument(0));
        when(purchases.hasPurchased(1L, "Anna")).thenReturn(true);
        Review buyer = service.addReview(1L, "Anna", 5, "Useful");
        Review visitor = service.addReview(1L, null, 4, null);
        assertThat(buyer.getAuthorName()).isEqualTo("Anna");
        assertThat(buyer.getProductId()).isEqualTo(1);
        assertThat(buyer.getRating()).isEqualTo(5);
        assertThat(buyer.getComment()).isEqualTo("Useful");
        assertThat(visitor.getAuthorName()).isEqualTo("anonymous");
        assertThat(visitor.getComment()).isNull();
        verify(purchases).hasPurchased(1L, "Anna");
        verify(purchases).hasPurchased(1L, null);
    }

    @Test
    void listsOnlyReviewsForRequestedProduct() {
        List<Review> expected = List.of(new Review(2L, "Anna", 5, "Useful"));
        when(reviews.findByProductId(2L)).thenReturn(expected);
        assertThat(service.listReviews(2L)).isEqualTo(expected);
    }
}

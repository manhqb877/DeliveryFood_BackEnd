package com.fooddelivery.orders.document;


import com.fooddelivery.orders.enums.AuthorType;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Document(collection = "review_comments")
@CompoundIndexes({
        @CompoundIndex(name = "idx_review_created", def = "{ 'reviewId': 1, 'createdAt': 1 }"),
        @CompoundIndex(name = "idx_parent_created", def = "{ 'parentId': 1, 'createdAt': 1 }"),
        @CompoundIndex(name = "idx_review_deleted", def = "{ 'reviewId': 1, 'isDeleted': 1 }")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCommentDocument {

    @Id
    private ObjectId id; // Khóa chính MongoDB ObjectId

    @Field("review_id")
    @Indexed
    private Long reviewId; // Tham chiếu lỏng tới order_db.reviews.id (PostgreSQL)

    @Field("parent_id")
    private ObjectId parentId; // NULL = comment gốc (level 1), ngược lại chứa ID của comment cha

    @Field("ancestors")
    @Indexed
    private List<ObjectId> ancestors; // Mảng ObjectId chứa toàn bộ tổ tiên để query nhanh toàn cây

    @Field("depth")
    private Integer depth = 0; // Cấp độ: 0 = gốc, 1 = reply cấp 1, 2 = reply cấp 2,...

    @Field("author_type")
    private AuthorType authorType; // Tác giả (CUSTOMER, GUEST, SHOP_MANAGER, ADMIN)

    @Field("author_id")
    private Long authorId; // ID của tác giả (users.id)

    @Field("author_name")
    private String authorName; // Snapshot tên tác giả (tránh join)

    @Field("author_avatar")
    private String authorAvatar; // Snapshot ảnh đại diện tác giả

    @Field("content")
    private String content; // Nội dung bình luận

    @Field("image_urls")
    private List<String> imageUrls; // Mảng URL ảnh đính kèm

    @Field("likes_count")
    private Integer likes_count = 0; // Tổng số lượt thích bình luận

    @Field("replies_count")
    private Integer repliesCount = 0; // Số lượng replies trực tiếp cấp dưới

    @Field("is_deleted")
    private Boolean isDeleted = false; // Soft delete (giữ cây, ẩn nội dung)

    @Field("deleted_at")
    private Instant deletedAt; // Thời điểm xóa mềm

    @Field("created_at")
    private Instant createdAt = Instant.now();

    @Field("updated_at")
    private Instant updatedAt = Instant.now();
}
package com.fooddelivery.orders.document;



import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "comment_likes")
@CompoundIndexes({
        @CompoundIndex(name = "idx_comment_user_unique", def = "{ 'commentId': 1, 'userId': 1 }", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentLikeDocument {

    @Id
    private ObjectId id; // Khóa chính

    @Field("comment_id")
    private ObjectId commentId; // ID của bình luận được thích

    @Field("user_id")
    private Long userId; // ID người dùng (users.id) chống like trùng lặp

    @Field("created_at")
    private Instant createdAt = Instant.now();
}
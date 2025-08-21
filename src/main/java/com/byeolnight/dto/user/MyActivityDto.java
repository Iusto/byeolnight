package com.byeolnight.dto.user;

import com.byeolnight.dto.comment.CommentDto;
import com.byeolnight.dto.message.MessageDto;
import com.byeolnight.dto.post.PostDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyActivityDto {
    private List<PostDto.Response> myPosts;
    private List<CommentDto.Response> myComments;
    private MessageDto.ListResponse receivedMessages;
    private MessageDto.ListResponse sentMessages;
    private long totalPostCount;
    private long totalCommentCount;
    private long totalReceivedMessageCount;
    private long totalSentMessageCount;
}
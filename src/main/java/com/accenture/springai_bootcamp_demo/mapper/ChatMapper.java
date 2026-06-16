package com.accenture.springai_bootcamp_demo.mapper;

import com.accenture.springai_bootcamp_demo.dto.ChatDto;
import com.accenture.springai_bootcamp_demo.dto.ChatSummaryDto;
import com.accenture.springai_bootcamp_demo.entity.Chat;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps {@link Chat} aggregates to full and summary API representations.
 */
@Mapper(componentModel = "spring", uses = MessageMapper.class)
public interface ChatMapper {

    ChatDto toDto(Chat chat);

    @Mapping(target = "messageCount", expression = "java(chat.getChatMessages().size())")
    ChatSummaryDto toSummary(Chat chat);

    List<ChatSummaryDto> toSummaries(List<Chat> chats);
}

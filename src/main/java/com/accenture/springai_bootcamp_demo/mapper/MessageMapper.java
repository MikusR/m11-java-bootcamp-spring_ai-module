package com.accenture.springai_bootcamp_demo.mapper;

import com.accenture.springai_bootcamp_demo.dto.ChatMessageDto;
import com.accenture.springai_bootcamp_demo.entity.ChatMessage;

import java.util.List;
import org.mapstruct.Mapper;

/**
 * Maps persisted {@link ChatMessage} entities to their API representation.
 */
@Mapper(componentModel = "spring")
public interface MessageMapper {

    ChatMessageDto toDto(ChatMessage chatMessage);

    List<ChatMessageDto> toDtos(List<ChatMessage> chatMessages);
}

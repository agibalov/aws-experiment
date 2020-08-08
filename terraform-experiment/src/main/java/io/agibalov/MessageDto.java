package io.agibalov;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageDto {
    private String message;
}

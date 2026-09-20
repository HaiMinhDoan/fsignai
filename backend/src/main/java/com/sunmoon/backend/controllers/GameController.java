package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.GameCode;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.gamification.GameFinishRequest;
import com.sunmoon.backend.dto.request.gamification.GameStartRequest;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.gamification.GameFinishResponse;
import com.sunmoon.backend.dto.response.gamification.GameStartResponse;
import com.sunmoon.backend.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.UUID;

@Tag(name = "Học tập - Trò chơi", description = "Chơi 4 trò ôn tập và ghi sao")
@RestController
@RequestMapping("/api/v1/learn/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @Operation(summary = "Bắt đầu một ván",
            description = "Trả nội dung ván (cặp từ hoặc câu đố) rút ngẫu nhiên. Mỗi lần gọi là một ván mới.")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/{gameCode}/start")
    public ResponseEntity<ResponseData<GameStartResponse>> start(
            @PathVariable GameCode gameCode, @RequestBody(required = false) GameStartRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(gameService.start(me, gameCode, request), "GAME_STARTED");
    }

    @Operation(summary = "Chốt kết quả ván", description = "Mỗi ván chỉ chốt được một lần; cộng sao vào tài khoản")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/sessions/{sessionId}/finish")
    public ResponseEntity<ResponseData<GameFinishResponse>> finish(
            @PathVariable UUID sessionId, @Valid @RequestBody GameFinishRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(gameService.finish(me, sessionId, request), "GAME_FINISHED");
    }

    private <T> ResponseEntity<ResponseData<T>> ok(T data, String messageCode) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseData.<T>builder()
                .status(HttpStatus.OK.value())
                .messageCode(messageCode)
                .data(data)
                .lang(SecurityContextHolder.getLang())
                .path(SecurityContextHolder.getPath())
                .timestamp(new Date())
                .build());
    }
}

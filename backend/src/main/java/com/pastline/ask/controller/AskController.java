package com.pastline.ask.controller;

import com.pastline.ask.dto.AskRequest;
import com.pastline.ask.dto.AskResponse;
import com.pastline.ask.service.AskService;
import com.pastline.auth.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AskController {

    private final AskService askService;

    @PostMapping("/ask")
    public AskResponse ask(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody AskRequest request) {
        return askService.ask(user.id(), request.question()).response();
    }
}

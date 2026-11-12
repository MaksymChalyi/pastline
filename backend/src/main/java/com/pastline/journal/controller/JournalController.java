package com.pastline.journal.controller;

import com.pastline.auth.security.AuthenticatedUser;
import com.pastline.journal.dto.EntryRequest;
import com.pastline.journal.dto.EntryResponse;
import com.pastline.journal.dto.PageResponse;
import com.pastline.journal.service.JournalService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Власник завжди береться із сесії, ніколи з запиту. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/entries")
public class JournalController {

    private final JournalService journalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntryResponse create(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody EntryRequest request) {
        return EntryResponse.from(journalService.create(user.id(), request));
    }

    @GetMapping
    public PageResponse<EntryResponse> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(journalService.list(user.id(), from, to, page, size), EntryResponse::from);
    }

    @GetMapping("/{id}")
    public EntryResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return EntryResponse.from(journalService.get(user.id(), id));
    }

    @PutMapping("/{id}")
    public EntryResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody EntryRequest request) {
        return EntryResponse.from(journalService.update(user.id(), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        journalService.delete(user.id(), id);
    }
}

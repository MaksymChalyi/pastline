package com.pastline.importing.controller;

import com.pastline.auth.security.AuthenticatedUser;
import com.pastline.importing.dto.ImportReport;
import com.pastline.importing.service.ImportService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/import")
public class ImportController {

    private final ImportService importService;

    /** За замовчуванням dryRun=true: без явного dryRun=false нічого не записується. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportReport importEntries(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean dryRun)
            throws IOException {
        return importService.importFile(user.id(), file.getOriginalFilename(), file.getBytes(), dryRun);
    }
}

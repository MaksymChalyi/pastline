package com.pastline.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** Реєструє нового користувача і повертає його сесію після входу. */
public final class TestSessions {

    private TestSessions() {}

    public static MockHttpSession newUser(MockMvc mvc) throws Exception {
        String credentials =
                "{\"email\":\"user-%s@example.com\",\"password\":\"correct-horse-1\"}".formatted(UUID.randomUUID());
        mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isCreated());
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
        assertThat(session).isNotNull();
        return session;
    }
}

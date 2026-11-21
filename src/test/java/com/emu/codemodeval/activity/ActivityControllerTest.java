package com.emu.codemodeval.activity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emu.codemodeval.activity.model.ActivityKind;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ActivityControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ActivityStream stream;

    @Test
    void subscribersGetEachActivityAsAnEscapedHtmlEvent() throws Exception {
        MvcResult events = mvc.perform(get("/events")).andExpect(request().asyncStarted()).andReturn();

        stream.publish(UUID.fromString("12345678-0000-0000-0000-000000000000"), ActivityKind.command, "a/b: $ npm test 2>&1 <script>");

        String body = events.getResponse().getContentAsString();
        assertTrue(body.contains("event:activity"), body);
        assertTrue(body.contains("<li class=\"command\">"), body);
        assertTrue(body.contains("<span class=\"job\">12345678</span>"), body);
        assertTrue(body.contains("npm test 2&gt;&amp;1 &lt;script&gt;"), body);
        assertFalse(body.contains("<script>"), "agent output must be escaped");
    }

    @Test
    void subscribingSendsSomethingRightAwaySoTheBrowserSeesTheStreamOpen() throws Exception {
        MvcResult events = mvc.perform(get("/events")).andExpect(request().asyncStarted()).andReturn();

        assertTrue(events.getResponse().getContentAsString().startsWith(":connected"),
            events.getResponse().getContentAsString());
    }

    @Test
    void homePageConnectsToTheEventStream() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("sse-connect=\"/events\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"npm\">npm</option>")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/css/app.css\"")));
        mvc.perform(get("/css/app.css")).andExpect(status().isOk());
    }
}

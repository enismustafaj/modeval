package com.emu.codemodeval.activity.api;

import com.emu.codemodeval.activity.ActivityStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityStream stream;

    /** Live backend activity: one "activity" event per line, as an HTML fragment for htmx. */
    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        return stream.subscribe();
    }
}

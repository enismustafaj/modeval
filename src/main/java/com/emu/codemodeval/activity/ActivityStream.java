package com.emu.codemodeval.activity;

import com.emu.codemodeval.activity.model.Activity;
import com.emu.codemodeval.activity.model.ActivityKind;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Broadcasts backend activity to every open browser as server-sent events. Nothing is stored. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStream {

    static final String EVENT = "activity";

    private final ActivityHtml html;
    private final List<SseEmitter> subscribers = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout; dropped when the browser goes away
        subscribers.add(emitter);
        emitter.onCompletion(() -> subscribers.remove(emitter));
        emitter.onTimeout(() -> subscribers.remove(emitter));
        emitter.onError(e -> subscribers.remove(emitter));
        try {
            // nothing reaches the browser until something is sent, so without this the page's EventSource
            // stays "connecting" until the first activity. A comment is ignored by the browser.
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            subscribers.remove(emitter);
        }
        return emitter;
    }

    /**
     * Open streams never finish on their own, so graceful shutdown would wait its full timeout for them.
     * The context publishes this before the web server stops; the browser reconnects to the next instance.
     */
    @EventListener(ContextClosedEvent.class)
    public void closeAll() {
        subscribers.forEach(SseEmitter::complete);
        subscribers.clear();
    }

    public void publish(UUID jobId, String message) {
        publish(jobId, ActivityKind.info, message);
    }

    public void publish(UUID jobId, ActivityKind kind, String message) {
        log.info("[{}] {}", jobId, message);
        String line = html.render(new Activity(jobId, kind, message, OffsetDateTime.now()));
        for (SseEmitter emitter : subscribers) {
            try {
                emitter.send(SseEmitter.event().name(EVENT).data(line));
            } catch (IOException | IllegalStateException e) {
                subscribers.remove(emitter); // browser closed the tab
            }
        }
    }
}

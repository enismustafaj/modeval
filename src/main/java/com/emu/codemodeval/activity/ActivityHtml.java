package com.emu.codemodeval.activity;

import com.emu.codemodeval.activity.model.Activity;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/** Renders an activity as the HTML line htmx appends to the log. */
@Component
public class ActivityHtml {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public String render(Activity activity) {
        // messages carry agent commands and model output: always escaped
        String job = activity.jobId() == null ? "" : activity.jobId().toString().substring(0, 8);
        return "<li class=\"" + activity.kind() + "\"><time>" + TIME.format(activity.at()) + "</time>"
            + "<span class=\"job\">" + job + "</span>"
            + "<span class=\"msg\">" + HtmlUtils.htmlEscape(activity.message().replace('\n', ' ')) + "</span></li>";
    }
}

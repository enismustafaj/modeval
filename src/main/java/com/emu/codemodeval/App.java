package com.emu.codemodeval;

import io.javalin.Javalin;

public class App {
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Javalin.create(config -> config.useVirtualThreads = true)
            .get("/health", ctx -> ctx.result("ok"))
            .start(port);
    }
}

package com.example.gmaildummy;

import java.util.Locale;

final class Mail {
    private static long nextId = 1;

    final long id = nextId++;
    final String category, sender, subject, snippet, time, attachment;
    final int color;
    String email, to = "me", body;
    boolean unread, starred, important, verified;
    int threadCount;

    Mail(String category, String sender, String subject, String snippet, String time,
         int color, boolean unread, boolean starred, String attachment) {
        this.category = category; this.sender = sender; this.subject = subject; this.snippet = snippet;
        this.time = time; this.color = color; this.unread = unread; this.starred = starred;
        this.attachment = attachment;
        this.email = sender.toLowerCase(Locale.ROOT).replace(' ', '.') + "@gmail.com";
        this.body = snippet;
    }

    Mail from(String email) { this.email = email; return this; }
    Mail body(String body) { this.body = body; return this; }
    Mail to(String to) { this.to = to; return this; }
    Mail important() { this.important = true; return this; }
    Mail verified() { this.verified = true; return this; }
    Mail thread(int count) { this.threadCount = count; return this; }

    boolean isOutgoing() { return category.equals("Sent") || category.equals("Drafts"); }

    String initial() { return sender.substring(0, 1).toUpperCase(Locale.ROOT); }
}

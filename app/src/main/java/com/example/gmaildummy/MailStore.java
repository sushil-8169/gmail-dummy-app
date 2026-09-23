package com.example.gmaildummy;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** In-memory mailbox shared by the inbox, reading and compose screens. */
final class MailStore {
    static final String ME = "alex.johnson@gmail.com";
    static final int ME_COLOR = 0xFF7B1FA2;
    static final List<Mail> mails = new ArrayList<>();

    static { seed(); }

    private MailStore() { }

    static Mail find(long id) {
        for (Mail m : mails) if (m.id == id) return m;
        return null;
    }

    static String now() {
        return new SimpleDateFormat("h:mm a", Locale.US).format(new Date());
    }

    private static void seed() {
        mails.add(new Mail("Primary", "Priya Sharma", "Weekend plans", "Are we still on for brunch this Saturday? I found a lovely new place downtown.", "10:42 AM", 0xFF8E24AA, true, false, null)
                .body("Hi Alex,\n\nAre we still on for brunch this Saturday? I found a lovely new place downtown – they do the best pancakes and the coffee is great.\n\nI was thinking 11 AM so we can beat the rush. Let me know if that works, and feel free to bring Sam along!\n\nCheers,\nPriya"));
        mails.add(new Mail("Primary", "Design Team", "Q4 product launch deck ✨", "Hi all, the latest mockups are ready for review. Please leave your comments by Friday.", "9:18 AM", 0xFF00897B, true, false, "Q4_Launch_Deck.pdf")
                .from("design-team@phoenix.dev")
                .body("Hi all,\n\nThe latest mockups for the Q4 launch are ready for review. The attached deck covers the new onboarding flow, the refreshed home screen and the updated brand colours.\n\nPlease leave your comments directly in the deck by Friday so we can lock the designs before the sprint planning on Monday.\n\nThanks!\nDesign Team"));
        mails.add(new Mail("Primary", "Alex Johnson", "Re: Project timeline", "Thanks for the update. I’ll share the revised timeline this afternoon.", "8:05 AM", 0xFF039BE5, true, true, null)
                .from("alex.j@work.dev")
                .body("Thanks for the update.\n\nI’ll share the revised timeline this afternoon. The main change is that QA moves to the week of Oct 12, which gives us an extra buffer for the payments integration.\n\nLet me know if anything looks off.\n\nAlex"));
        mails.add(new Mail("Primary", "Rahul Verma", "Invoice for September", "Please find attached the invoice for September. Let me know if you have any questions.", "Sep 22", 0xFFF4511E, false, false, "Invoice_Sep_2026.pdf")
                .body("Hello,\n\nPlease find attached the invoice for September. Payment is due within 15 days of receipt.\n\nLet me know if you have any questions.\n\nRegards,\nRahul Verma"));
        mails.add(new Mail("Primary", "Neha Kapoor", "Birthday party pics 🎉", "Here are all the photos from Saturday! Thanks again for coming.", "Sep 22", 0xFFD81B60, false, true, null)
                .body("Hey!\n\nHere are all the photos from Saturday! Thanks again for coming – it wouldn’t have been the same without you.\n\nThe cake photo is my favourite 😄\n\nLove,\nNeha"));
        mails.add(new Mail("Primary", "Google", "Security alert", "A new sign-in on Pixel 9 was detected. If this was you, you don’t need to do anything.", "Sep 21", 0xFF1A73E8, false, false, null)
                .from("no-reply@accounts.google.com")
                .body("A new sign-in on Pixel 9\n\n" + ME + "\n\nWe noticed a new sign-in to your Google Account on a Pixel 9 device. If this was you, you don’t need to do anything. If not, we’ll help you secure your account.\n\nCheck activity at myaccount.google.com/notifications"));
        mails.add(new Mail("Primary", "Mom", "Dinner on Sunday?", "Your dad is making his famous biryani. Let me know if you can come!", "Sep 20", 0xFF43A047, false, false, null)
                .body("Hi beta,\n\nYour dad is making his famous biryani on Sunday. Let me know if you can come! Bring some dessert if you pass by the bakery.\n\nLove,\nMom"));
        mails.add(new Mail("Primary", "Karan Mehta", "Offsite agenda", "Sharing the draft agenda for next week’s team offsite. Feel free to add topics.", "Sep 19", 0xFF3949AB, false, false, null)
                .body("Hi team,\n\nSharing the draft agenda for next week’s offsite:\n\n• 10:00 – Kickoff and 2026 recap\n• 11:30 – Roadmap breakout sessions\n• 1:00 – Lunch\n• 2:30 – Team activity\n\nFeel free to add topics.\n\nKaran"));
        mails.add(new Mail("Primary", "Sarah Lee", "Coffee next week?", "It’s been a while! Would love to catch up if you’re free Tuesday or Wednesday.", "Sep 18", 0xFF6D4C41, false, false, null)
                .body("Hi Alex,\n\nIt’s been a while! Would love to catch up if you’re free Tuesday or Wednesday. There’s a new café near your office I’ve been wanting to try.\n\nSarah"));
        mails.add(new Mail("Promotions", "Spotify", "Your Daily Mix is ready", "A fresh playlist picked just for you is waiting.", "7:30 AM", 0xFF1DB954, true, false, null)
                .from("no-reply@spotify.com"));
        mails.add(new Mail("Promotions", "Medium Daily Digest", "Stories you might enjoy", "The latest ideas and perspectives from writers you follow.", "6:10 AM", 0xFF212121, true, false, null)
                .from("noreply@medium.com"));
        mails.add(new Mail("Social", "LinkedIn", "You appeared in 12 searches this week", "See who’s looking at your profile and grow your network.", "9:02 AM", 0xFF0A66C2, true, false, null)
                .from("notifications@linkedin.com"));
        mails.add(new Mail("Social", "Meetup", "New event: Android Devs Bangalore", "Join us for talks on Compose, performance and more.", "Sep 21", 0xFFE53935, false, false, null)
                .from("info@meetup.com"));
        mails.add(new Mail("Updates", "GitHub", "[gmail-dummy-app] Build succeeded", "Build APK workflow completed successfully on main.", "Sep 22", 0xFF24292F, false, false, null)
                .from("notifications@github.com"));
        mails.add(new Mail("Updates", "Amazon.in", "Your order has shipped", "Your package is on its way and will arrive by Thursday.", "Sep 20", 0xFFFF9900, false, false, null)
                .from("shipment-tracking@amazon.in"));
        mails.add(new Mail("Sent", "me", "Re: Weekend plans", "Sounds great! 11 works for me.", "Sep 19", ME_COLOR, false, false, null)
                .from(ME).to("Priya Sharma"));
    }
}

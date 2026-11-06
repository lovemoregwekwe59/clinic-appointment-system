package com.clinic;

import com.clinic.ui.ConsoleApp;

/**
 * Main application entry point for the Clinic Appointment System.
 */
public class Main {

    public static void main(String[] args) {
        ConsoleApp app = new ConsoleApp();

        // Check if automated demonstration flag was passed
        if (args.length > 0 && ("--demo".equalsIgnoreCase(args[0]) || "-d".equalsIgnoreCase(args[0]))) {
            app.runFiveMinuteDemo();
            return;
        }

        // Start the interactive console interface
        app.start();
    }
}

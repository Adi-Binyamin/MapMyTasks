Map My Tasks

Map My Tasks is a comprehensive Android application. Built with Kotlin in Android Studio, this application goes beyond standard to-do lists by integrating location-based services, real-time weather updates, partner collaboration, and productivity tracking.

Key Features:

Advanced Task Management: Create, edit, and track your daily tasks seamlessly.
Location-Aware Architecture: Proactive task tracking based on your geographic location and time.
Weather Integration: Real-time weather updates and proactive alerts to help you plan your schedule effectively.
Partner Collaboration: Connect with partners and assign shared tasks to manage work together.
App Orientation Chat: A built-in chat system designed to guide users and help them navigate the application.
Productivity Screen: A dedicated screen to track your task completion progress and stay motivated.
Ballon game: interactive mini-game screen designed for entertainment, allowing users to pop balloons for a quick and fun break from their daily tasks.
Weekly Summary Screen: Provides a comprehensive overview of your performance and completed tasks over the past week.
Smart Notifications: Receive actionable alerts for upcoming deadlines, shared task updates, and weather conditions.
Holiday Alerts: An integrated notification system that warns you if a chosen task date conflicts with a Jewish holiday.
Smart Recommendation Algorithm: The app analyzes your historical task behavior. If you attempt to schedule a task in a category or at a time of day when you frequently cancel tasks, the system triggers a proactive warning, suggesting you consider a different category and a better time slot to improve your completion rate.

Location Services and Mapping Architecture:

The defining feature of Map My Tasks is its ability to seamlessly tie your daily responsibilities to the physical world and your schedule. Below is an overview of how the location and mapping subsystem is engineered:

1. Fused Location Provider and Permissions
   The app leverages Google Play Services FusedLocationProviderClient to efficiently fetch the device's precise geographical coordinates while optimizing battery consumption. The app explicitly requests runtime permissions for precise and coarse location, as well as background location tracking to ensure tasks can be evaluated even when the app is minimized.

2. Smart Time, Location, and Weather Alerts
   To ensure users receive timely and relevant reminders without being overwhelmed with notifications, the alert system operates under strict conditions:

Regular Task Notifications: The background service periodically checks the user's current coordinates against pending tasks. A system notification reminding the user to complete a task is triggered only if the user is within a 1-kilometer radius of the designated location and the current time matches the scheduled time for that specific task.

Proactive Weather Warnings: To prevent weather-related disruptions, the app evaluates the forecast for a scheduled task's location. A dedicated weather warning notification is sent up to 24 hours before the event, allowing users to prepare or reschedule if necessary.

3. Interactive Mapping Interface
   The tasks screen utilizes Google Maps SDK for Android to provide a clear spatial visualization. Active tasks are rendered as interactive pins on the map. The camera automatically centers or adjusts zoom bounds to encapsulate the user's current location relative to nearby tasks. When creating or editing a task, the app uses the Geocoder API to asynchronously translate string addresses into Latitude and Longitude pairs.

Tech Stack:
Language: Kotlin
Environment: Android Studio
Core Components: Activities, Adapters, Background Workers, Location Services, and Broadcast Receivers.
APIs and SDKs: Google Maps SDK, Fused Location Provider API, OpenWeatherMap API.

Created By:
Ido Muallem
Adi Binyamin
# Cloneify

Cloneify is a desktop music streaming application, inspired by Spotify, built using JavaFX for the user interface and a Spring Boot backend. It integrates with the Deezer API to provide a vast library of music for discovery and playback. The application features robust user authentication, personal playlist management, and an innovative AI-powered playlist generator using Google's Gemini API.

## Features

-   **Music Discovery & Playback:** Browse curated playlists by genre (Rock, Pop, Jazz, etc.) and play 30-second previews for any track, sourced directly from the Deezer API.
-   **Track Search:** A powerful search function to find any song or artist available on Deezer.
-   **Personal Playlist Management:** Create, rename, delete, and manage your own custom playlists. Add songs from search, discovery, or AI suggestions to your library.
-   **AI Playlist Generation:** Describe a mood, genre, or theme, and the built-in AI assistant, powered by Google Gemini, will generate a unique playlist for you. This playlist can then be saved to your library.
-   **User System:** Secure user registration with email verification and login functionality.
-   **Profile Avatars:** Automatically fetches and displays user profile pictures from Gravatar based on the registered email address.
-   **Modern UI:** A sleek, draggable, and transparent user interface built with JavaFX and custom CSS styling for a contemporary dark theme.

## Tech Stack

-   **Backend:** Spring Boot, Spring Data JPA, Spring Mail
-   **Frontend:** JavaFX
-   **Database:** H2 (In-file)
-   **Build Tool:** Maven
-   **External APIs:**
    -   Deezer API: For all music data, including tracks, artists, and previews.
    -   Google Gemini API: Powers the AI playlist generation feature.
    -   Gravatar API: For user profile avatars.

## Getting Started

### Prerequisites

-   Java Development Kit (JDK) 21 or later
-   Apache Maven
-   An active internet connection

### Configuration

This project requires API keys for its email and AI features. You must set the following environment variables before running the application:

1.  **Gmail Credentials:** For sending email verification codes to new users.
    -   `MAIL_USER`: Your Gmail address.
    -   `MAIL_PASS`: Your Gmail App Password. You can [create an App Password here](https://support.google.com/accounts/answer/185833).

2.  **Google Gemini API Key:** For the AI Playlist generation feature.
    -   `GEMINI_API_KEY`: Your API key from Google AI Studio. You can [get one here](https://aistudio.google.com/app/apikey).

Set these variables in your operating system's environment or directly in your IDE's run configuration.

### Installation & Running

1.  Clone the repository to your local machine:
    ```sh
    git clone https://github.com/POGorilla/Cloneify.git
    cd Cloneify
    ```

2.  Run the application using the provided Maven wrapper. This will handle all dependencies and start the backend server and JavaFX client.

    -   On macOS/Linux:
        ```sh
        ./mvnw spring-boot:run
        ```

    -   On Windows:
        ```cmd
        mvnw.cmd spring-boot:run
        ```

The application will launch the login window. The backend REST API will be accessible at `http://localhost:8080`.

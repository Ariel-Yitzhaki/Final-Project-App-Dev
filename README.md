![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-Min%20SDK%2024-3DDC84?logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-32.7.0-FFCA28?logo=firebase&logoColor=black)
![Google Maps](https://img.shields.io/badge/Google%20Maps-18.2.0-4285F4?logo=googlemaps&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

# Travel 
<img width="200" alt="img_travel_logo" src="https://github.com/user-attachments/assets/4a2f7abc-b830-4597-8142-e10e745eac50" />
<br><br>

## Description

An Android app that turns your trips into visual route diaries. Take photos at stops along the way — each one is pinned to the map and connected by your travel path, building up in real time as you go. Friends can follow your trip as it happens, see your route grow, and like your photos.

Built with Kotlin, Firebase, and Google Maps SDK.
<br><br>

## Features

### Trip Management <img align="right" width="200" src="https://github.com/user-attachments/assets/b0e3e9ba-eb1c-47ae-bed8-cb88151d4cc7">  <img align="right" width="200" src="https://github.com/user-attachments/assets/59c9325b-1dad-4048-bf9b-631200eb66a8">  
- Create, name, activate, and reactivate trips with automatic lifecycle handling
- Bottom sheet trip selector for quick switching between trips
- Full trip deletion with cascading cleanup of photos, likes, and storage
<br clear="right">


<br><br>

### Geotagged Photos <img align="right" width="130" src="https://github.com/user-attachments/assets/86fba9b5-fa45-4aeb-bb70-a0f7ea939624"> <img align="right" width="130" alt="image" src="https://github.com/user-attachments/assets/b398eb46-1c75-49d8-818c-0c5025ed9206">

- Capture photos with the in-app camera
- Preview with reverse-geocoded address before uploading
- 200m minimum distance between shots to keep the route meaningful
- Photos are tagged with GPS coordinates and linked to the active trip


<br clear="right">
<br><br>

### Interactive Map <img align="right" width="200" src="https://github.com/user-attachments/assets/50ed65ea-da7e-460b-94c4-a0f1d283f848">

- Live location tracking on Google Maps
- Custom photo pin markers with tap-to-view popups
- Dashed travel path with direction arrows connecting your stops
- Full-screen trip map dialog for viewing any trip's complete route

<br clear="right">

<br><br>

### Social <img align="right" width="200" src="https://github.com/user-attachments/assets/d7b25bf9-bbff-4e43-9d8a-dfc3de0b2b8f">

- Add friends via username search with request/accept flow
- Home feed of friends' completed trips with like counts
- View friend profiles, explore their trips and routes
- Like photos across the app

<img width="130" alt="image" src="https://github.com/user-attachments/assets/ac6e4f38-a8f8-47ae-9b84-6ca6bb07ada5" >
<img width="130" alt="image" src="https://github.com/user-attachments/assets/446de0db-c357-479c-b42b-f717c9a7aec1" />
<img width="130" alt="image" src="https://github.com/user-attachments/assets/76cb8156-ce23-4eda-9405-481eebb1bbfb" />



<br clear="right">

<br><br>


## Tech Stack

| Layer | Technologies |
|-------|-------------|
| Language | Kotlin |
| Platform | Android (Min SDK 24, Target SDK 36) |
| Auth | Firebase Authentication (email/password) |
| Database | Cloud Firestore |
| Storage | Firebase Storage |
| Maps | Google Maps SDK, Maps Utils, Play Services Location |
| Images | Glide |
| UI | Material Components (Material3), ConstraintLayout, SwipeRefreshLayout |
<br>


## Architecture

```
com.example.travel/
├── activities/          # Login, Main, PhotoPreview
├── adapters/            # 8 RecyclerView adapters
├── data/                # Repository singletons (Auth, Trip, Photo, Friends, Like)
├── fragments/           # HomeFeed, Map, Profile, Friends, TripDetail, etc.
├── interfaces/          # Refresh, TripEndListener
├── managers/            # TripManager, CameraManager, TripCleanupManager
├── models/              # User, Trip, Photo, Like, Friends, FriendRequest
├── utils/               # Geocoding, MapRendering, Fragment, Click utilities
└── viewmodels/          # PhotoPreviewViewModel
```

- **Repositories** handle all Firebase access - no fragment or activity calls Firestore/Storage directly. Each is a singleton with in-memory caching.
- **Managers** extract complex workflows (trip lifecycle, camera flow, cascading deletes) out of activities.
- **Fragments** own screen logic - fetch data from repositories and update their own views.
- **Callbacks and interfaces** for communication - managers use lambdas, fragments use interfaces (`TripEndListener`, `Refresh`) to talk to their host activity.

### Firestore Data Model

```
users/{uid}
  ├── id, username, displayName, email, profilePictureUrl

trips/{tripId}
  ├── id, userId, name, startDate, endDate, active, photoCount

photos/{photoId}
  ├── id, userId, tripId, imageUrl, latitude, longitude, date, timestamp

likes/{photoId_userId}
  ├── id, photoId, userId

friends/{user1Id_user2Id}                 (alphabetically ordered IDs)
  ├── id, user1Id, user2Id

friendRequests/{senderId_receiverId}
  ├── id, senderId, receiverId, status    (pending/accepted/declined)
```
<br>


## Setup

### Prerequisites
- Android Studio (latest stable)
- A Firebase project with **Authentication** (Email/Password), **Cloud Firestore**, and **Firebase Storage** enabled
- A Google Cloud API key with **Maps SDK for Android** enabled

### Steps

**1. Clone the repo**

```bash
git clone https://github.com/Ariel-Yitzhaki/Final-Project-App-Dev.git
```

**2. Add Firebase config**

Download `google-services.json` from your Firebase project settings and place it in the `app/` directory.

**3. Add your Maps API key** to `local.properties`:

```properties
MAPS_API_KEY=your_api_key_here
```

**4. Build and run** on a physical device (camera and GPS required).

---


## Author

Ariel Yitzhaki


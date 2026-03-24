![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-Min%20SDK%2024-3DDC84?logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-32.7.0-FFCA28?logo=firebase&logoColor=black)
![Google Maps](https://img.shields.io/badge/Google%20Maps-18.2.0-4285F4?logo=googlemaps&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

# Travel

An Android app that turns your trips into visual route diaries. Take photos at stops along the way - each one is pinned to the map and connected by your travel path, building up in real time as you go. Friends can follow your trip as it happens, see your route grow, and like your photos.

Built with Kotlin, Firebase, and Google Maps SDK.


---

## Features

- **Trip Management** - Create, name, activate, and reactivate trips with automatic lifecycle handling and a bottom sheet selector
  
- **Geotagged Photos** - Capture photos with the in-app camera, preview with reverse-geocoded address, and upload with a 200m minimum distance between shots
  
- **Interactive Map** - Live location on Google Maps with custom photo pin markers, a dashed travel path with direction arrows, and tap-to-view photo popups
  
- **Social** - Add friends, explore their trips and routes in a home feed, like their photos, and view their profiles
  
- **Profile** - Custom profile picture, trip history with like counts, and full trip/photo deletion with cascading cleanup
  

---

## Tech Stack

### Language & Platform
| | |
|---|---|
| **Language** | Kotlin |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 36 |

### Backend - Firebase (serverless)
| Service | Usage |
|---|---|
| **Firebase Auth** | Email/password authentication |
| **Cloud Firestore** | Database - `users`, `trips`, `photos`, `likes`, `friends`, `friendRequests` collections |
| **Firebase Storage** | Photo images (`photos/{id}.jpg`), profile pictures (`profile_pictures/{uid}.jpg`) |
| **Firebase Analytics** | Usage tracking |

### Libraries
| Library | Version | Purpose |
|---|---|---|
| Firebase BOM | 32.7.0 | Auth, Firestore, Storage, Analytics |
| Google Maps SDK | 18.2.0 | Map display, markers, polylines |
| Google Maps Utils | 3.8.2 | `SphericalUtil` for midpoint/bearing calculations |
| Play Services Location | 21.0.1 | Device GPS location |
| Glide | 4.16.0 | Image loading, circle crop, bitmap targets for map markers |
| Material Components | 1.12.0 | Material3 theme, BottomSheetDialog, ShapeableImageView, MaterialButton |
| AndroidX Core KTX | 1.17.0 | Kotlin extensions for Android core APIs |
| AndroidX AppCompat | 1.6.1 | Backward-compatible Activity and Fragment support |
| ConstraintLayout | 2.2.1 | Flexible view positioning in layouts |
| Lifecycle Runtime KTX | 2.6.1 | `lifecycleScope` for coroutine-based async work |
| Lifecycle ViewModel KTX | 2.7.0 | `viewModelScope` for ViewModel coroutines |
| Activity KTX | 1.8.2 | `viewModels()` delegate, `registerForActivityResult` |
| SwipeRefreshLayout | 1.1.0 | Pull-to-refresh on feed, profile, and trip detail |


---

## Architecture

```
com.example.travel/
├── activities/          # 3 Activities (Login, Main, PhotoPreview)
├── adapters/            # 8 RecyclerView adapters
├── data/                # 5 Repository singletons (Auth, Trip, Photo, Friends, Like)
├── fragments/           # 8 Fragments (HomeFeed, Map, Profile, Friends, etc.)
├── interfaces/          # Refresh, TripEndListener
├── managers/            # TripManager, CameraManager, TripCleanupManager
├── models/              # 6 Data classes (User, Trip, Photo, Like, Friends, FriendRequest)
├── utils/               # GeocodingUtils, MapRenderingUtils, FragmentUtils, ClickUtils
└── viewmodels/          # PhotoPreviewViewModel
```

### Pattern
- **Repositories** handle all Firebase access - no fragment or activity calls Firestore/Storage directly. Each repository is a singleton with in-memory caching to avoid redundant network calls
  
- **Managers** extract complex workflows (trip lifecycle, camera flow, cascading deletes) out of activities to keep them thin
  
- **Fragments** own screen logic - they fetch data from repositories and update their own views
  
- **Callbacks and interfaces** for communication - managers use lambdas to notify activities, fragments use interfaces (`TripEndListener`, `Refresh`) to talk to their host activity

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

friends/{user1Id_user2Id}        (alphabetically ordered IDs)
  ├── id, user1Id, user2Id

friendRequests/{senderId_receiverId}
  ├── id, senderId, receiverId, status (pending/accepted/declined)
```

---

## Screens

| Screen | Description |
|---|---|
| **Login** | Email/password login with toggle to register mode (username + display name) |
| **Home Feed** | Friends' completed trips in a scrollable feed with like counts |
| **Map** | Google Map centered on user location with photo markers for active trip |
| **Profile** | User info, profile picture, trip list with end/delete actions |
| **Friends** | Pending requests + friends list with remove and profile navigation |
| **User Search** | Search by username, send friend requests, status indicators |
| **Trip Detail** | Vertical photo list with likes, addresses, delete option (owner only) |
| **Trip Map Dialog** | Full-screen map showing a specific trip's route and photos |
| **Photo Preview** | Camera output preview with location, upload/retake options |

---

## Setup

### Prerequisites
- Android Studio (latest stable)
  
- A Firebase project with **Authentication** (Email/Password), **Cloud Firestore**, and **Firebase Storage** enabled
  
- A Google Cloud API key with **Maps SDK for Android** enabled
  

### Steps

1. Clone the repository
```bash
   git clone https://github.com/Ariel-Yitzhaki/Final-Project-App-Dev.git
```

2. Download `google-services.json` from your Firebase project's settings and place it in the `app/` directory
   
   Then download `google-services.json` from Project Settings and place it in the `app/` directory

3. Add your Google Maps API key to `local.properties`:
```properties
   MAPS_API_KEY=your_api_key_here
```

4. Build and run on a physical device (camera and GPS required)
---

## Design

- **Theme:** Material3 NoActionBar with a white-dominant color scheme
- **Typography:** Mont font family (6 weights) - Heavy for screen titles and trip names, Bold for labels and buttons, SemiBold for metadata, Regular for secondary text
- **Navigation:** Bottom bar with 4 tabs (Home, Friends, Map, Profile) with Azure/black icon tinting for selected state, and a centered FAB overlapping the bar on the map tab
- **Colors:** White backgrounds, dark (`#18181B`) primary text and buttons, Azure (`#0066FF`) for accents and interactive elements, light gray (`#F4F4F5`) card surfaces, dark surface for the photo preview screen
- **Components:** Rounded MaterialCardView cards (16dp corners) for feed, trips, friends, and search results. Bottom sheet trip selector with grab handle.
   Circular profile pictures with 1dp black stroke. Custom dialogs with rounded backgrounds and side-by-side action buttons. Gradient hero overlay on the map screen. Swipe-to-refresh on feed, profile, and trip detail  

---

## Authors

Ariel Yitzhaki

---

## License

This project is licensed under the [MIT License](LICENSE).

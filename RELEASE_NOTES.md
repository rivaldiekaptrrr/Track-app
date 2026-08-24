# TrackIt v3.0.0 (Major Release)

## 🚀 Massive Core Architecture Update

Version 3.0.0 brings a complete overhaul to TrackIt's cloud synchronization engine. We have entirely replaced the official Firebase Android SDK's gRPC transport layer with a custom-built, highly resilient HTTP REST API implementation.

### Why this change was made:
During extensive debugging, we discovered that certain Android devices and cellular networks silently drop or block **gRPC** packets (the default protocol used by Firebase Firestore SDK), causing indefinite timeouts and preventing data from syncing. 

### What's New:
- **Custom FirestoreRestClient**: A hand-crafted OkHttp client that communicates directly with Google's Firestore REST APIs.
- **100% gRPC Bypass**: By using standard HTTP POST/PATCH/DELETE methods, synchronization is now immune to ISP-level gRPC throttling or blocking.
- **Fetch-on-Startup Sync**: Replaced the fragile `addSnapshotListener` with a robust, one-time bulk data fetch that occurs seamlessly every time the app opens.
- **Enhanced Debugging Removed**: The temporary in-app terminal overlay has been successfully removed, restoring the clean Dashboard UI.

This update ensures that no matter what mobile network you are on, your financial records and wedding plans will flawlessly sync to the cloud!

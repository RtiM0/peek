<p align="center">
  <img src="assets/peek-logo.webp" width="112" alt="Peek app icon" />
</p>

<h1 align="center">Peek for Android</h1>

<p align="center">
  A calm, native space for the links people share.<br />
  <strong>Kotlin</strong> · <strong>Jetpack Compose</strong> · <strong>Android</strong>
</p>

Paste a link, then read, watch, and browse the content without the noise of the source app.

## Screens

<p align="center">
  <img src="assets/screenshots/home.webp" alt="Peek home and recent links" width="31%" />
  <img src="assets/screenshots/post-viewer.webp" alt="Peek post viewer" width="31%" />
  <img src="assets/screenshots/post-viewer-media.webp" alt="Peek video post viewer" width="31%" />
</p>
<p align="center">
  <img src="assets/screenshots/video-flow.webp" alt="Peek full-screen video flow" width="31%" />
  <img src="assets/screenshots/loading.webp" alt="Peek loading state" width="31%" />
  <img src="assets/screenshots/unsupported-link.webp" alt="Peek unsupported link state" width="31%" />
</p>

## Current features

- Paste links from the clipboard or open Instagram links directly.
- Resolve public Instagram posts, reels, and carousels with photos and videos.
- Read comments, load more replies, refresh content, and revisit recent links.
- Open media in an edge-to-edge viewer with video playback and carousel navigation.
- Cache resolved content locally for a quicker return experience.

## Availability

| Link type | Availability | Resolver |
| --- | --- | --- |
| Public Instagram posts (`/p/`) | Available | Direct logged-out GraphQL resolver |
| Public Instagram reels (`/reel/`, `/reels/`) | Available | Direct GraphQL resolver, then hidden WebView fallback |
| Public Instagram carousels | Available | Instagram resolver chain above |
| Private posts, login-required content, challenges, consent flows | Unsupported | No resolver currently available |

Resolvers are selected through a shared prioritized resolver chain, so adding a new source does not require changing the viewer UI.

## Roadmap

- [ ] Add more link sources, including Reddit, X/Twitter, TikTok, and YouTube.
- [ ] Add more Instagram features, including Stories.
- [ ] Improve sharing, link history, and source-specific viewing experiences.

## Principle and architecture

Peek separates link resolution from presentation. Source-specific resolvers normalize content into shared domain models; repositories handle caching and pagination; ViewModels expose screen state; stateless Jetpack Compose UI renders it.

```mermaid
flowchart LR
    A[Shared link] --> B[Resolver chain]
    B --> C[Normalized domain model]
    C --> D[Repository + cache]
    D --> E[ViewModel state]
    E --> F[Jetpack Compose UI]
```

The app is a single-activity Kotlin/Jetpack Compose project. `domain` contains models, repository contracts, and use cases; `data` contains Instagram resolution, persistence, caching, and fixtures; `ui` contains navigation, ViewModels, mappers, and rendering.

## Contributing

Peek is early and intentionally small. Ideas, source integrations, design improvements, bug reports, and pull requests are all welcome—please include a short description and relevant tests when you can.

## Build and verify

Requires JDK 17 and Android SDK 36.

```shell
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:validateDebugScreenshotTest
./gradlew :app:connectedDebugAndroidTest
```

## Acknowledgements

- Peek’s Instagram fetching logic is based on [Kittygram](https://codeberg.org/irelephant/kittygram).
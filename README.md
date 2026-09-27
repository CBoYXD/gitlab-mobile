# GitLab Mobile

A native Android client for GitLab.com and self-hosted GitLab servers, built with Jetpack Compose. It aims for the depth of GitHub Mobile: read code with syntax highlighting, review merge requests and diffs, follow pipelines down to the job log, and write Markdown with a real editor.

<p>
  <img src="docs/screenshots/dark/home.webp" width="200" alt="Home with popular projects">
  <img src="docs/screenshots/dark/project.webp" width="200" alt="Project page with languages and sections">
  <img src="docs/screenshots/dark/file.webp" width="200" alt="Ruby file with syntax highlighting">
  <img src="docs/screenshots/dark/merge-request.webp" width="200" alt="Merge request overview">
</p>

## Features

**Projects and code**
- Project page with description, topics, license, stars and forks, and a language breakdown
- Rendered README (GitLab Flavored Markdown: tables, task lists, code blocks, images, badges, emoji)
- Code browser with branch and tag switching, breadcrumbs and folder navigation
- File viewer with syntax highlighting for 17 languages, Markdown preview and images
- Commits, branches, tags, releases (with notes and assets), contributors and project activity

**Merge requests and issues**
- Lists with filters, colored labels (including scoped labels) and pipeline status
- Merge request tabs: overview, discussion, commits, pipelines and highlighted diffs
- Approvals, reviewers, merge status in plain words, milestone and reactions
- Issue detail with assignees, labels, milestone, due date, weight, time tracking and task progress
- Markdown editor with a formatting toolbar and live preview for comments, descriptions and new issues
- Merge, close, reopen, comment and edit when signed in

**CI/CD**
- Pipelines across your projects and per project, with stages
- Pipeline detail with jobs grouped by stage; running pipelines refresh on their own
- Job detail with the log rendered in color (ANSI), collapsed section markers and a jump to the end
- Retry pipelines and jobs, cancel running pipelines

**Everything else**
- Inbox for GitLab to-dos, with filters and mark as done
- Global search for projects, issues, merge requests and people
- User profiles with their projects and activity; groups with subgroups and projects
- GitLab links inside Markdown open the matching screen in the app
- Several servers side by side; switch from your profile
- Light and dark themes, reduced-motion support, 48dp touch targets and screen-reader labels throughout

## Screenshots

| Light | Dark |
| --- | --- |
| <img src="docs/screenshots/light/project.webp" width="260" alt="Project page, light"> | <img src="docs/screenshots/dark/project.webp" width="260" alt="Project page, dark"> |
| <img src="docs/screenshots/light/readme.webp" width="260" alt="Rendered README, light"> | <img src="docs/screenshots/dark/readme.webp" width="260" alt="Rendered README, dark"> |
| <img src="docs/screenshots/light/code.webp" width="260" alt="Code browser, light"> | <img src="docs/screenshots/dark/code.webp" width="260" alt="Code browser, dark"> |
| <img src="docs/screenshots/light/merge-requests.webp" width="260" alt="Merge requests with labels, light"> | <img src="docs/screenshots/dark/merge-requests.webp" width="260" alt="Merge requests with labels, dark"> |
| <img src="docs/screenshots/light/changes.webp" width="260" alt="Merge request diff, light"> | <img src="docs/screenshots/dark/changes.webp" width="260" alt="Merge request diff, dark"> |
| <img src="docs/screenshots/light/pipeline.webp" width="260" alt="Pipeline detail, light"> | <img src="docs/screenshots/dark/pipeline.webp" width="260" alt="Pipeline detail, dark"> |
| <img src="docs/screenshots/light/release.webp" width="260" alt="Release notes and assets, light"> | <img src="docs/screenshots/dark/release.webp" width="260" alt="Release notes and assets, dark"> |

All screenshots show public projects on GitLab.com, browsed without signing in.

## Signing in

| Method | When to use it |
| --- | --- |
| **Access token** | Any server. Create a personal access token with the `api` scope under *Preferences → Access tokens* (`read_api` is enough to browse). |
| **OAuth** | Servers where you can register an application: non-confidential, `api` scope, redirect URI `com.aistudio.gitlab.mclient://oauth`. Works with SSO. Tokens refresh automatically. |
| **Password** | Servers that allow password sign-in. The app creates a personal access token for itself. Supports two-factor codes. |
| **No sign-in** | Browse public projects, code, merge requests and pipelines without an account. Nothing can be changed in this mode. |

Credentials are encrypted with a key held in Android Keystore and never leave the device except as the auth header to the server you added. Backups are disabled for the app.

## Architecture

- **UI:** Jetpack Compose with a small design system (`ui/theme`, `ui/components`): semantic color tokens measured for WCAG contrast in both themes, a named type scale, motion tokens that respect *Remove animations*.
- **State:** one `GitLabViewModel` owning a `StateFlow<GitLabUiState>`, split into features (`ProjectFeature`, `ReviewFeature`, `PipelineFeature`, `ExploreFeature`). Every section is a `Loadable` with its own loading, error and empty states.
- **Sessions:** all network work runs in a coroutine scope tied to the active server and is cancelled when you switch servers, so a slow response from one server never lands in another's screens.
- **Networking:** Retrofit and OkHttp on a single shared client. Lists are paged with hard limits (the app never walks a whole history), cancellation always propagates, and expired OAuth tokens are refreshed once under a mutex with a retry.
- **Markdown:** parsed with `org.jetbrains:markdown` (GFM) and rendered natively; highlighting uses `dev.snipme:highlights`, both off the main thread.

## Building

Requirements: JDK 17+, Android SDK 36.

```bash
./gradlew :app:assembleDebug          # build
./gradlew :app:testDebugUnitTest      # unit tests (MockWebServer, Robolectric)
./gradlew :app:installDebug           # install on a connected device or emulator
```

The unit tests cover pagination limits, cancellation, JSON null handling, token parsing, error messages, Markdown parsing and the editor's formatting actions.

## License

No license has been chosen yet, so default copyright applies. Add a `LICENSE` file before accepting contributions.

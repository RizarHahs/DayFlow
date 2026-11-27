# DAYFLOW — Project Guide

## 1. Project Identity

- **App Name:** DAYFLOW
- **Repository:** https://github.com/RizarHahs/DayFlow
- **Application ID:** `com.dayflow.app`
- **Current Released Version:** 0.6.0
- **Current Version Code:** 6
- **Minimum Android Version:** Android 8.0 (API 26)
- **Target SDK:** 35 (verify in Gradle before relying on this)
- **Language:** Kotlin

DAYFLOW is a personal productivity and daily routine management Android application designed to help users organize their day, build better habits, manage tasks, and maintain consistency.

---

## 2. Technology Stack

The project currently uses or is configured around:

- Kotlin
- Jetpack Compose
- MVVM Architecture
- Room Database
- Hilt Dependency Injection
- AlarmManager
- BroadcastReceivers
- Gradle

Always verify the actual implementation in the source code before making assumptions about the architecture or dependencies.

---

## 3. Core Features

DAYFLOW includes or is designed around the following features:

- Daily planning and roadmap.
- Habit tracking.
- Task management and subtasks.
- Ideas and notes.
- Alarms and timers.
- Scheduled WhatsApp messages.
- Insights and daily reviews.
- JSON data backup and restore.

### Scheduled WhatsApp Messages

DAYFLOW does not automatically send WhatsApp messages. The user prepares the message and manually sends it through WhatsApp.

Do not change this behavior without explicit approval.

---

## 4. Important Development Rules

Every AI assistant working on DAYFLOW must follow these rules.

1. Read `PROJECT_GUIDE.md`, `DEVELOPMENT_STATE.md`, and `CHANGELOG.md` before beginning work.
2. Inspect the relevant source files before modifying anything.
3. Treat the actual source code as authoritative if documentation is outdated.
4. DAYFLOW is an existing application. Never create a replacement project from scratch.
5. Do not rewrite the architecture unnecessarily.
6. Do not remove existing functionality without explicit approval.
7. Avoid unrelated refactoring while implementing a requested feature.
8. Preserve the existing application ID: `com.dayflow.app`.
9. Preserve the existing database and user data.
10. Database changes must be handled carefully with appropriate migrations.
11. Never perform destructive database changes that could erase user data without explicit approval.
12. Do not introduce unnecessary libraries or dependencies.
13. Do not change release signing configurations carelessly.
14. Never generate a replacement signing key for future releases.
15. Never request, expose, or commit private signing credentials.
16. Never commit `dayflow-release.jks`, `keystore.properties`, passwords, secrets, or private user data.
17. Keep the versionCode increasing for future releases.
18. Update versionName and versionCode intentionally when preparing a new release.
19. Do not claim a build, test, or verification succeeded unless it was actually performed.
20. Explain important changes and potential risks before implementing them.

---

## 5. GitHub and Collaboration Rules

GitHub is the shared source of truth for the project source code.

Repository:

https://github.com/RizarHahs/DayFlow

### Collaboration between AI accounts

Multiple AI accounts may work on DAYFLOW, but only one account should modify the project at a time.

Before starting:

1. Read the project documentation.
2. Inspect the latest repository state.
3. Verify the checkpoint against the actual code.
4. Understand the existing implementation.
5. Confirm the task before making changes.

Before handing over to another AI account:

1. Complete or document the current work.
2. Update `DEVELOPMENT_STATE.md`.
3. Update `CHANGELOG.md` when appropriate.
4. Commit changes.
5. Push changes to GitHub when possible.
6. Record the latest commit SHA in the checkpoint when available.
7. Clearly document unfinished work.

Never force-push or overwrite concurrent work.

Prefer separate branches and pull requests for larger changes when practical, but do not change the user's existing workflow without explaining it first.

---

## 6. Release Signing

DAYFLOW has a permanent release signing key configured privately by the project owner.

Future release APKs must use the same signing identity.

The signing key and configuration are stored outside the public source repository.

Never:

- Generate a new release key without explicit approval.
- Delete or replace the existing signing configuration.
- Commit signing keys or passwords.
- Ask the owner to paste signing passwords into an AI chat.
- Modify release signing configuration without explaining why.

The owner is responsible for keeping the signing key and backups secure.

---

## 7. Release Distribution

DAYFLOW is currently distributed through GitHub Releases.

Repository:

https://github.com/RizarHahs/DayFlow

Future releases should include:

- Updated source code on GitHub.
- An increased versionCode.
- An appropriate versionName.
- A signed APK using the permanent release key.
- Release notes.
- A GitHub Release with the APK attached.

The Google Play Store is not currently the distribution channel.

---

## 8. Planned Update System

An in-app update system using GitHub Releases is a planned feature, not something that should be assumed to be implemented.

The intended functionality includes:

- Checking the latest GitHub release.
- Comparing the installed version with the latest version.
- Displaying update notifications.
- Showing release notes.
- Downloading the APK.
- Launching Android's package installer.
- Allowing the user to approve the installation.

Verify the actual code before assuming any of these capabilities exist.

---

## 9. Development Workflow

For every task:

1. Understand the user's request.
2. Inspect the relevant source code.
3. Explain the implementation plan briefly.
4. Identify risks if applicable.
5. Implement only the necessary changes.
6. Verify the implementation.
7. Run builds or tests when possible.
8. Report actual results.
9. Update project documentation.
10. Prepare a clear handover if work is unfinished.

---

## 10. Communication Style

The project owner is a beginner in Android development.

Therefore:

- Explain things in simple language.
- Avoid unnecessary technical jargon.
- Give instructions step by step.
- Do not overwhelm the owner with multiple unrelated tasks.
- Do not assume the owner understands Gradle, Kotlin, Git, signing, or Android internals.
- Explain what important commands or actions accomplish.
- Wait for confirmation when a workflow requires the owner to perform a step.
- Do not silently make major architectural decisions.

The goal is to build DAYFLOW carefully, maintain its existing functionality, protect user data, and make the development process understandable to its owner.

---

**DAYFLOW — Make your days count.**

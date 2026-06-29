# Maintained MoneyWallet Fork

[![License: GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.html)

This repository is a maintained fork of [MoneyWallet](https://github.com/AndreAle94/moneywallet), a GPL-licensed Android expense manager.

The immediate goal is to make the FOSS/OpenStreetMap build work reliably on current Android versions, including fixing the startup crash reported by existing users and modernizing the build toolchain.

This fork is not yet a published replacement for the original app. A rebranded release, a new application id, migration notes, and an F-Droid submission are planned before public distribution.

> "Maintained MoneyWallet Fork" is a temporary working name. The final name and application id will be chosen before any release.

![Showcase](pictures/showcase.png)

## About this fork
MoneyWallet is an on-device expense manager: multiple wallets, categories, budgets, recurring transactions, multi-currency, reports, and local backup, with no account required. The upstream project has not had a release since 2021 and no longer starts on recent Android for many users. This fork picks up maintenance, beginning with compatibility, and aims to continue it as a privacy-respecting, offline-first expense tracker.

## Current status
Modernization is in progress on a development branch and has not been released. The most recent published upstream build does not launch on Android 14 and 15 for users who had auto-backup enabled. Making the app build and start again on current Android is the first priority. Until a release is published, this fork is not an installable replacement for the original app.

## What is being fixed first
- Modernizing the FOSS/OpenStreetMap Android build to a current toolchain and SDK level.
- The Android 12+ manifest and PendingIntent requirements needed to run on modern Android.
- The startup crash caused by a legacy auto-backup path (upstream [#177](https://github.com/AndreAle94/moneywallet/issues/177) and [#286](https://github.com/AndreAle94/moneywallet/issues/286)).
- Verifying the core flow (create a wallet, add income and expense, totals, relaunch persistence) on Android 14 and 15.

Progress is tracked in this repository's [issues](https://github.com/herrerad85/moneywallet/issues) and milestones.

## Build from source
The fully open-source variant uses OpenStreetMap and no proprietary services. Build it with the `floss` and `osm` flavors:

```
./gradlew assembleFlossOsmDebug
```

Requirements: a recent Android SDK and a JDK. The `proprietary` flavor (Google Drive, Dropbox) and the `gmap` flavor (Google Maps) need API keys in `gradle.properties` and are not the focus of this fork. The build-toolchain modernization is landing on the development branch, so the most reliable build comes from there once it is published.

Note on icons: the original precompiled app bundled an icon pack that is not redistributable; only the small free subset is in the source tree. Providing a distributable icon set is part of the rebranded-release work.

## Roadmap
- Android 14/15 compatibility: modernize the build, fix the startup crash, verify core flows.
- Rebranded release: choose a fork name and application id, present the README and license cleanly for a GPL fork, add build and migration docs, and submit to F-Droid.
- Backup, export, and sync: fix export and import under scoped storage, add WebDAV/Nextcloud sync (upstream [#67](https://github.com/AndreAle94/moneywallet/issues/67)), and a local-file, Syncthing-friendly option.

See the pinned [roadmap issue](https://github.com/herrerad85/moneywallet/issues/15) for current details.

## Upstream and license
This is a fork of [AndreAle94/moneywallet](https://github.com/AndreAle94/moneywallet). MoneyWallet is free software licensed under the GNU General Public License v3.0, and this fork remains under the same license. See [LICENSE.md](LICENSE.md).

Original work and credits: MoneyWallet was created by its upstream author and contributors. Logo by Adam Lapinski; first-start images from Freepik; internal icon set from RoundIcons.

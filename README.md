# Tallybook

[![License: GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.html)
[![F-Droid](https://img.shields.io/f-droid/v/io.github.herrerad85.tallybook.svg)](https://f-droid.org/packages/io.github.herrerad85.tallybook/)

Tallybook is a private, offline-first expense and budget tracker for Android: multiple wallets, categories, budgets, recurring transactions, multi-currency, reports, an optional PIN, pattern or fingerprint lock, and backup to a local folder or your own WebDAV server, with no account required.

## Install

Tallybook is on F-Droid: [f-droid.org/packages/io.github.herrerad85.tallybook](https://f-droid.org/packages/io.github.herrerad85.tallybook/)

Signed APKs are also attached to each [GitHub release](https://github.com/herrerad85/moneywallet/releases). The F-Droid build is reproducible and carries the developer signature, so the two are interchangeable.

**Tallybook is a maintained fork of [MoneyWallet](https://github.com/AndreAle94/moneywallet)** by AndreAle94, a GPL-licensed Android expense manager whose last release was in 2021. This fork modernizes the open-source build, fixes the startup crash that stopped the app launching on recent Android, and continues maintenance under a new name and application id.

> **Tallybook is a separate app, not an automatic update to MoneyWallet.** It uses a different application id (`io.github.herrerad85.tallybook`), so it installs side by side with the original and does not replace it or migrate its data automatically. See [MIGRATION.md](MIGRATION.md) for the verified manual migration path.

This fork is independent and is not endorsed by or affiliated with the original author.

![Showcase](pictures/showcase.png)

## Status
Tallybook is published on F-Droid and actively maintained. The current release is **1.2.0**. Builds are reproducible and carry the developer signature, so the F-Droid binary matches the one built from this repository.

Since the first release: backups to your own WebDAV server, such as Nextcloud, ownCloud or a NAS, with no third-party account (1.1.0); and a per-app language setting, a run of localization and date and time fixes, and a batch of smaller UI and backup fixes (1.2.0).

Earlier work, all shipped: Android 14/15 compatibility on a modernized FOSS/OpenStreetMap build, the Android 12+ manifest and PendingIntent updates, a fix for the startup crash caused by a legacy auto-backup path (reported upstream as [#177](https://github.com/AndreAle94/moneywallet/issues/177) and [#286](https://github.com/AndreAle94/moneywallet/issues/286)), the rebrand to a new name and application id, a verified backup and restore migration path (see [MIGRATION.md](MIGRATION.md)), export and import under scoped storage, a local-folder backup option that works with file sync tools, and Android 15 edge-to-edge UI polish.

Progress is tracked in this repository's [issues](https://github.com/herrerad85/moneywallet/issues).

## Build from source
The fully open-source variant uses OpenStreetMap and no proprietary services. Build the `floss` + `osm` flavors:

```
./gradlew assembleFlossOsmDebug
```

Requirements: a recent Android SDK and JDK 17 or newer. Release builds use JDK 21, which is what the F-Droid build server uses, so a release built on an older JDK will not reproduce. The `proprietary` flavor (Google Drive, Dropbox) and the `gmap` flavor (Google Maps) require API keys in `gradle.properties` and are not the focus of this fork.

Note on icons: the launcher and the intro illustrations are original artwork for this fork, released under the GPLv3. The category picker icons place glyphs from Phosphor Icons (MIT), Tabler Icons (MIT), Lucide (ISC), and Bootstrap Icons (MIT) on original GPLv3 disc backgrounds; a few remain original artwork. The interface also uses Material Design Icons, licensed under Apache-2.0.

## Roadmap
The original plan is complete: Android 14/15 compatibility, the rebrand, backup and export fixes, F-Droid submission and publication, and self-hosted WebDAV sync (upstream [#67](https://github.com/AndreAle94/moneywallet/issues/67)) have all shipped.

Current direction, open work and anything under consideration live in the pinned [roadmap issue](https://github.com/herrerad85/moneywallet/issues/15).

## Upstream and license
Tallybook is a fork of [AndreAle94/moneywallet](https://github.com/AndreAle94/moneywallet). MoneyWallet is free software licensed under the GNU General Public License v3.0, and Tallybook remains under the same license. See [LICENSE.md](LICENSE.md).

Original work and credits: MoneyWallet was created by its upstream author and contributors. The Tallybook app icon and the intro illustrations are original artwork for this fork, released under the same GPLv3 license. The category icons combine original GPLv3 disc backgrounds with glyphs from Phosphor Icons (MIT), Tabler Icons (MIT), Lucide (ISC), and Bootstrap Icons (MIT); a few category icons remain original artwork. Full copyright and license texts for these icon libraries are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). The interface also uses Material Design Icons, licensed under Apache-2.0.

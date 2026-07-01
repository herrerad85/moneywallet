# Tallybook

[![License: GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.html)

Tallybook is a private, offline-first expense and budget tracker for Android: multiple wallets, categories, budgets, recurring transactions, multi-currency, reports, and local backup, with no account required.

**Tallybook is a maintained fork of [MoneyWallet](https://github.com/AndreAle94/moneywallet)** by AndreAle94, a GPL-licensed Android expense manager whose last release was in 2021. This fork modernizes the open-source build, fixes the startup crash that stopped the app launching on recent Android, and continues maintenance under a new name and application id.

> **Tallybook is a separate app, not an automatic update to MoneyWallet.** It uses a different application id (`io.github.herrerad85.tallybook`), so it installs side by side with the original and does not replace it or migrate its data automatically. See [MIGRATION.md](MIGRATION.md) for the verified manual migration path.

This fork is independent and is not endorsed by or affiliated with the original author.

![Showcase](pictures/showcase.png)

## Status
The Android 14/15 compatibility work has landed: a modernized FOSS/OpenStreetMap build, the Android 12+ manifest and PendingIntent updates, and a fix for the startup crash caused by a legacy auto-backup path (reported upstream as [#177](https://github.com/AndreAle94/moneywallet/issues/177) and [#286](https://github.com/AndreAle94/moneywallet/issues/286)). It has been verified on an Android 15 emulator (launch, wallet creation, income and expense entry, totals, and relaunch persistence).

The rebrand to Tallybook is complete: a new name and application id, a verified backup and restore migration path (see [MIGRATION.md](MIGRATION.md)), export and import fixed under scoped storage, a local-folder backup option that works with file sync tools, and Android 15 edge-to-edge UI polish. The F-Droid listing metadata and screenshots are prepared in the repository. No public release has been published yet. Self-hosted WebDAV/Nextcloud sync and the F-Droid submission are still planned. Progress is tracked in this repository's [issues](https://github.com/herrerad85/moneywallet/issues) and milestones.

## Build from source
The fully open-source variant uses OpenStreetMap and no proprietary services. Build the `floss` + `osm` flavors:

```
./gradlew assembleFlossOsmDebug
```

Requirements: a recent Android SDK and JDK 17. The `proprietary` flavor (Google Drive, Dropbox) and the `gmap` flavor (Google Maps) require API keys in `gradle.properties` and are not the focus of this fork.

Note on icons: the launcher and the intro illustrations are original artwork for this fork, released under the GPLv3. The category picker icons place glyphs from Phosphor Icons (MIT), Tabler Icons (MIT), Lucide (ISC), and Bootstrap Icons (MIT) on original GPLv3 disc backgrounds; a few remain original artwork. The interface also uses Material Design Icons, licensed under Apache-2.0.

## Roadmap
- Android 14/15 compatibility: modernize the build, fix the startup crash, verify core flows. (Done.)
- Rebrand: new name and application id, build and migration docs. (Done.)
- Backup and export: fix export and import under scoped storage, and a local-file, Syncthing-friendly backup option. (Done.)
- Self-hosted sync: WebDAV/Nextcloud (upstream [#67](https://github.com/AndreAle94/moneywallet/issues/67)). (Planned.)
- F-Droid: listing metadata and screenshots. (Done.)
- F-Droid: submission to the f-droiddata build-recipe repository. (Planned.)

See the pinned [roadmap](https://github.com/herrerad85/moneywallet/issues/15) for current details.

## Upstream and license
Tallybook is a fork of [AndreAle94/moneywallet](https://github.com/AndreAle94/moneywallet). MoneyWallet is free software licensed under the GNU General Public License v3.0, and Tallybook remains under the same license. See [LICENSE.md](LICENSE.md).

Original work and credits: MoneyWallet was created by its upstream author and contributors. The Tallybook app icon and the intro illustrations are original artwork for this fork, released under the same GPLv3 license. The category icons combine original GPLv3 disc backgrounds with glyphs from Phosphor Icons (MIT), Tabler Icons (MIT), Lucide (ISC), and Bootstrap Icons (MIT); a few category icons remain original artwork. Full copyright and license texts for these icon libraries are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). The interface also uses Material Design Icons, licensed under Apache-2.0.

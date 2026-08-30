# Tallybook

[![License: GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.html)
[![F-Droid](https://img.shields.io/f-droid/v/io.github.herrerad85.tallybook.svg)](https://f-droid.org/packages/io.github.herrerad85.tallybook/)

Tallybook is a private, offline-first expense and budget tracker for Android: multiple wallets, categories, budgets, recurring transactions, multi-currency, reports, an optional PIN, pattern or fingerprint lock, and backup to a local folder or your own WebDAV server, with no account required.

New to the app, or wondering whether it already does something? See the [FAQ](docs/FAQ.md).

## Install

Tallybook is on F-Droid: [f-droid.org/packages/io.github.herrerad85.tallybook](https://f-droid.org/packages/io.github.herrerad85.tallybook/)

Signed APKs are also attached to each [GitHub release](https://github.com/herrerad85/moneywallet/releases). The F-Droid build is reproducible and carries the developer signature, so the two are interchangeable.

**Tallybook is a maintained fork of [MoneyWallet](https://github.com/AndreAle94/moneywallet)** by AndreAle94, a GPL-licensed Android expense manager whose last release was in 2021. This fork modernizes the open-source build, fixes the startup crash that stopped the app launching on recent Android, and continues maintenance under a new name and application id.

> **Tallybook is a separate app, not an automatic update to MoneyWallet.** It uses a different application id (`io.github.herrerad85.tallybook`), so it installs side by side with the original and does not replace it or migrate its data automatically. See [MIGRATION.md](MIGRATION.md) for the verified manual migration path.

This fork is independent and is not endorsed by or affiliated with the original author.

![Showcase](pictures/showcase.png)

## Status
Tallybook is published on F-Droid and actively maintained. The current version in this repository is **1.7.0**, so F-Droid may show an older one.

1.7.0 adds a home screen widget showing one wallet's balance, lets a budget cover more than one category, opens a category's Overview total into the subcategories it is made of, and adds 73 category icons. It also returns you to Transactions when you press Back in a section, where that used to close the app, and makes the status bar follow the theme color on Android 15 and later. Before that: 1.6.0 let a budget repeat, said what came in and what went out on a transactions list, and marked the days that have transactions in the calendar day strip; 1.5.0 fixed Android's own backup, which stored nothing, so a restore brought back an empty app; and 1.4.0 added duplicating a transaction and hiding a category's child categories, after WebDAV backup (1.1.0), a per app language setting (1.2.0), and a map you can point at any tile server (1.3.0).

Earlier work, all shipped: Android 14/15 compatibility on a modernized FOSS/OpenStreetMap build, the Android 12+ manifest and PendingIntent updates, a fix for the startup crash caused by a legacy auto-backup path (reported upstream as [#177](https://github.com/AndreAle94/moneywallet/issues/177) and [#286](https://github.com/AndreAle94/moneywallet/issues/286)), the rebrand to a new name and application id, a verified backup and restore migration path (see [MIGRATION.md](MIGRATION.md)), export and import under scoped storage, a local-folder backup option that works with file sync tools, and Android 15 edge-to-edge UI polish.

Progress is tracked in this repository's [issues](https://github.com/herrerad85/moneywallet/issues).

## The CSV import format
The header row carries the raw column keys, and a file written by hand needs the same ones. Five columns are required on every row: `wallet`, `currency`, `category`, `datetime` and `money`. Five more are optional: `description`, `event`, `people`, `place` and `note`.

`datetime` is `yyyy-MM-dd HH:mm:ss`, or `yyyy-MM-dd` for a row with no time of day. `currency` is the ISO code, and it has to be one the app already has. `money` is negative for an expense, and zero or above for income.

```
"wallet","currency","category","datetime","money","description"
"Everyday","USD","Groceries","2026-08-12 09:30:00","-12.34","market"
"Everyday","USD","Salary","2026-08-12","2000.00","august"
```

A row the importer will not read ends the import before anything from the file is saved, and the message names the line it stopped on. A row the CSV reader itself will not read, such as one with the wrong number of fields, ends it the same way but with the reader's own wording.

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

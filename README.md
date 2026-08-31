# 💰 Cash Ledger

<p align="center">
  <img src="pictures/showcase.png" alt="Cash Ledger Showcase" width="100%" />
</p>

<p align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0.html"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge" alt="License: GPLv3" /></a>
  <a href="https://f-droid.org/packages/io.github.solomonrajan.cashledger/"><img src="https://img.shields.io/f-droid/v/io.github.solomonrajan.cashledger.svg?style=for-the-badge&logo=fdroid" alt="F-Droid" /></a>
  <a href="https://github.com/herrerad85/moneywallet/releases"><img src="https://img.shields.io/github/v/release/herrerad85/moneywallet?style=for-the-badge&logo=github&color=success" alt="GitHub Release" /></a>
  <img src="https://img.shields.io/badge/Platform-Android%2014%2B%20%7C%2015-green?style=for-the-badge&logo=android" alt="Android" />
</p>

**Cash Ledger** is a private, offline-first expense and budget manager for Android. Take full control of your personal finances with multiple wallets, custom categories, smart budgets, recurring transactions, multi-currency support, detailed reports, and biometric security — with zero accounts, no tracking, and complete data ownership.

---

## 📑 Table of Contents

| Section | Description |
| :--- | :--- |
| 🚀 [**Installation**](#-installation) | Download via F-Droid or GitHub Releases |
| ✨ [**Key Features**](#-key-features) | Overview of app capabilities & tools |
| 🔄 [**Migration & Upstream**](#-migration-from-moneywallet) | Migrating data from MoneyWallet & fork history |
| 📊 [**CSV Import Specification**](#-csv-import-specification) | File format, schema table, and syntax examples |
| 🛠️ [**Build from Source**](#%EF%B8%8F-build-from-source) | Requirements, build flavor matrix, and commands |
| 🗺️ [**Roadmap & Status**](#%EF%B8%8F-roadmap--status) | Current release highlights and tracking |
| 📜 [**Licenses & Credits**](#-upstream-and-licenses) | GPLv3 licensing, third-party icons, and notices |
| ❓ [**Documentation & FAQ**](#-documentation--support) | Common questions, guides, and issue tracker |

---

## 🚀 Installation

| Source | Link | Details |
| :--- | :--- | :--- |
| 📦 **F-Droid** | [Get it on F-Droid](https://f-droid.org/packages/io.github.solomonrajan.cashledger/) | Official reproducible open-source build |
| 🐙 **GitHub Releases** | [Download Signed APK](https://github.com/herrerad85/moneywallet/releases) | Interchangeable developer-signed release APKs |
| 🛠️ **Source Code** | [Build Instructions](#%EF%B8%8F-build-from-source) | Compile the FLOSS + OpenStreetMap flavor locally |

---

## ✨ Key Features

- 👛 **Multiple Wallets**: Manage separate accounts, cash, credit cards, and bank balances.
- 🏷️ **Categorization & Icons**: Rich icon picker with subcategories and overview drill-downs.
- 🎯 **Budgets & Goals**: Set category and recurring budgets to prevent overspending.
- 🔁 **Recurring Transactions**: Schedule regular income, bills, and subscription payments.
- 💱 **Multi-Currency Support**: Track exchange rates and manage custom or crypto currencies.
- 📈 **Reports & Charts**: Visual breakdown of income vs. expenses across custom timeframes.
- 🔒 **Privacy & App Lock**: Screen lock via PIN, pattern, or fingerprint; 100% offline with no analytics.
- 💾 **Flexible Backup & Sync**: Automated WebDAV sync (Nextcloud, ownCloud, NAS) or local-folder backup.
- 📱 **Home Screen Widget**: View balance and quickly log transactions right from your launcher.
- 📤 **Data Portability**: Export to CSV, PDF, and XLS; seamless CSV transaction import.

---

## 🔄 Migration from MoneyWallet

> [!IMPORTANT]
> **Cash Ledger is a separate app, not an in-place update to MoneyWallet.**  
> It uses its own application ID (`io.github.solomonrajan.cashledger`), allowing it to install side-by-side with original MoneyWallet without overwriting existing data.

Follow the verified step-by-step guide in [MIGRATION.md](MIGRATION.md) to transfer your database seamlessly:

| Step | Action | Description |
| :---: | :--- | :--- |
| **1** | 💾 **Export Backup** | Open MoneyWallet > Settings > Create a local backup (`.mwbx` / `.mwbs`). |
| **2** | 📥 **Install App** | Install **Cash Ledger** alongside MoneyWallet. |
| **3** | 🔄 **Restore Data** | Open Cash Ledger > Settings > Restore from your saved backup file. |
| **4** | ✅ **Verify & Enjoy** | Confirm wallets and transactions appear, then resume managing your finances. |

> [!NOTE]
> This repository is a fork of [herrerad85/moneywallet](https://github.com/herrerad85/moneywallet), which is a maintained fork of [MoneyWallet](https://github.com/AndreAle94/moneywallet) by AndreAle94. This fork is independent and is not endorsed by or affiliated with the original author.

---

## 📊 CSV Import Specification

Import existing transaction records easily with standard CSV files.

### 📋 Column Schema

| Column | Required | Type / Format | Description & Notes |
| :--- | :---: | :--- | :--- |
| `wallet` | ✅ Yes | String | Name of the wallet (e.g. `Everyday`, `Savings`). Created automatically if missing. |
| `currency` | ✅ Yes | ISO Code (3 chars) | Standard currency code (e.g. `USD`, `EUR`). Must be enabled in the app. |
| `category` | ✅ Yes | String | Category name (e.g. `Groceries`, `Salary`). Created automatically if missing. |
| `datetime` | ✅ Yes | `yyyy-MM-dd HH:mm:ss` or `yyyy-MM-dd` | Timestamp of the transaction. |
| `money` | ✅ Yes | Decimal Number | Amount: **negative** for expenses (e.g. `-12.34`), **positive/zero** for income (e.g. `2000.00`). |
| `description`| ⚪ Optional | String | Summary or label for the transaction. |
| `event` | ⚪ Optional | String | Associated event tag (e.g. `Vacation 2026`). |
| `people` | ⚪ Optional | String | Associated person or payee/payer name. |
| `place` | ⚪ Optional | String | Location or venue name. |
| `note` | ⚪ Optional | String | Additional detailed notes. |

### 📝 Example CSV

```csv
"wallet","currency","category","datetime","money","description"
"Everyday","USD","Groceries","2026-08-12 09:30:00","-12.34","Supermarket"
"Everyday","USD","Salary","2026-08-12","2000.00","August Payroll"
```

A row the importer cannot read stops the process before saving, clearly naming the problem line.

---

## 🛠️ Build from Source

### ⚙️ Prerequisites
- **Android SDK** (API Level 35+)
- **JDK 17+** (Release builds use **JDK 21** for F-Droid reproducibility)

### 📦 Build Flavor Matrix

| Flavor Combination | Components | Services Included | Build Command |
| :--- | :--- | :--- | :--- |
| **`floss` + `osm`** *(Recommended)* | Open Source + OpenStreetMap | 100% Free & Open Source (No Google APIs) | `./gradlew assembleFlossOsmDebug` |
| **`proprietary` + `gmap`** | Google Drive / Dropbox + Google Maps | Requires API keys in `gradle.properties` | `./gradlew assembleProprietaryGmapDebug` |

---

## 🗺️ Roadmap & Status

- 📌 **Current Version**: **`1.7.0`**
- 🌟 **Recent Updates**:
  - Home screen widget for live wallet balance display.
  - Multi-category budgets & subcategory Overview breakdowns.
  - 73 new category icons and modernized edge-to-edge UI.
  - Android 14/15 system bar theme colors and scoped storage stability.
  - WebDAV self-hosted cloud backup and restore.
- 🎯 **Roadmap**: Open features and proposals are tracked in [Roadmap Issue #15](https://github.com/herrerad85/moneywallet/issues/15).

---

## 📜 Upstream and Licenses

| Component | License | Author / Source |
| :--- | :--- | :--- |
| **Cash Ledger Codebase** | [GPL-3.0](LICENSE.md) | Fork of [herrerad85/moneywallet](https://github.com/herrerad85/moneywallet) & [AndreAle94/moneywallet](https://github.com/AndreAle94/moneywallet) |
| **App Icon & Illustrations** | [GPL-3.0](LICENSE.md) | Original artwork created for this fork |
| **Material Design Icons** | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) | [Material Design Icons](https://materialdesignicons.com) |
| **Phosphor Icons** | [MIT](https://github.com/phosphor-icons/core) | [Phosphor Icons](https://phosphoricons.com) |
| **Tabler Icons** | [MIT](https://github.com/tabler/tabler-icons) | [Tabler Icons](https://tabler.io/icons) |
| **Lucide Icons** | [ISC](https://github.com/lucide-icons/lucide) | [Lucide](https://lucide.dev) |
| **Bootstrap Icons** | [MIT](https://github.com/twbs/icons) | [Bootstrap Icons](https://icons.getbootstrap.com) |

> Complete license texts and attributions can be found in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and [LICENSE.md](LICENSE.md).

---

## ❓ Documentation & Support

| Resource | Description |
| :--- | :--- |
| 📖 [**User FAQ**](docs/FAQ.md) | Answers to common questions, navigation guides, and tips |
| 🔄 [**Migration Guide**](MIGRATION.md) | Step-by-step instructions to transfer data from MoneyWallet |
| 🛡️ [**Privacy Policy**](PRIVACY.md) | Full details on offline storage, network permissions, and data safety |
| 🐛 [**Issue Tracker**](https://github.com/herrerad85/moneywallet/issues) | Report bugs or submit feature requests |

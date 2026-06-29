# Migrating from MoneyWallet to Tallybook

Tallybook is a **separate app** from the original MoneyWallet. It uses a different application id (`io.github.herrerad85.tallybook`), which means:

- Both apps can be installed at the same time, side by side.
- Installing Tallybook does **not** update or replace MoneyWallet, and it does **not** read MoneyWallet's data automatically.
- Tallybook starts empty. To bring your data across, migrate it manually using the steps below.

## Intended migration path: Backup and Restore

Tallybook is expected to restore MoneyWallet backups because it preserves the legacy data format, but this still needs end-to-end verification before a public release. The intended flow brings across your wallets, transactions, categories, budgets, and the rest:

1. In the original **MoneyWallet**: open Settings and create a local **Backup**. If you set a backup password, remember it, you will need it to restore.
2. Find the backup file in the backup folder the app used, and keep it somewhere safe (for example copy it to your computer, or to another folder on the device).
3. Install **Tallybook** (it installs alongside MoneyWallet, it does not overwrite it).
4. In **Tallybook**: open Settings and **Restore** from that backup file, entering the password if one was set.
5. Check that your wallets and recent transactions appear, then carry on in Tallybook.

Keep MoneyWallet installed and your backup file safe until you are confident everything came across. This process does not delete anything from MoneyWallet.

## Notes and current limitations

- **End-to-end restore from the original MoneyWallet into Tallybook is still a required pre-release verification step.** It has not yet been validated end to end, so treat the steps above as the intended path rather than a confirmed one.
- **Backup/Restore is the intended path** for a complete copy of your data. CSV, Excel, and PDF export are meant for use in other tools, not as a full round-trip migration.
- **File export/import under modern Android scoped storage still needs follow-up work** and may not behave correctly in every case yet (tracked in the project issues). If a backup file is hard to locate or restore, please open an issue and include your Android version.
- Coming from a very old (pre-4.0) MoneyWallet? The app also includes a legacy importer for the old database; prefer Backup/Restore where possible and use the legacy import only as a fallback.

## Why Tallybook is a separate app

The original application id belongs to the upstream MoneyWallet project and cannot be reused for a distributed fork, so Tallybook ships under its own id. This keeps the two apps independent and lets you run them side by side while you migrate.

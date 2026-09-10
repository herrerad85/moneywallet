# The CSV import format

The header row carries the raw column keys, and a file written by hand needs the same ones. Five columns are required on every row: `wallet`, `currency`, `category`, `datetime` and `money`. Five more are optional: `description`, `event`, `people`, `place` and `note`.

`datetime` is `yyyy-MM-dd HH:mm:ss`, or `yyyy-MM-dd` for a row with no time of day. `currency` is the ISO code, and it has to be one the app already has. `money` is negative for an expense, and zero or above for income.

```
"wallet","currency","category","datetime","money","description"
"Everyday","USD","Groceries","2026-08-12 09:30:00","-12.34","market"
"Everyday","USD","Salary","2026-08-12","2000.00","august"
```

A row the importer will not read ends the import before anything from the file is saved, and the message names the line it stopped on. A row the CSV reader itself will not read, such as one with the wrong number of fields, ends it the same way but with the reader's own wording.

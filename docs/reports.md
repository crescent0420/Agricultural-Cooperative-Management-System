# ACMS management reports

The brief asks for at least two of four reports; ACMS implements **all four**. Each one is a `GET` under `/api/reports`,
answers in the usual envelope, and has the same shape:

```json
{ "success": true, "message": "OK", "timestamp": "...",
  "data": {
    "report": "Outstanding contributions per season",
    "basis": "one or two sentences: what is counted and how",
    "scope": { "districtId": 15, "seasonId": 42, "fieldOfficerId": null },
    "generatedAt": "2026-10-29T12:00:00Z",
    "rows":   [ ... one line per cooperative / offering / season-and-district ... ],
    "totals": { ... the sum of the rows ... } } }
```

## Why the reports always agree with each other

The Union's complaint was that *"the reports prepared by different districts do not agree with one another"*.
ACMS removes the causes:

| Cause of disagreement | What ACMS does |
|---|---|
| Each district counts in its own way | **One definition per report**: one SQL query (`ReportQueries`) and one `basis` text, used for every district. A district report is the Union report with a district filter, never a separate calculation. |
| Figures typed in or copied from spreadsheets | Calculated **from the source records at the moment of the request**. Nothing is stored, cached or entered by hand. |
| A total taken at a different moment than its rows | All queries of one report run in **one `REPEATABLE READ` read-only transaction**: they read the same snapshot even while payments or enrolments are being recorded. The `totals` are **added up from the rows themselves**. |
| Different rounding | One rounding helper (`ReportMath`), always `BigDecimal`. Money collected rounds **down**, with the same rule as a farmer's own balance (`ReportMathTest` checks that they agree). |

## Who may see which report

| Report | Field Officer | District Agronomist | Union Registrar | Finance Officer | System Admin | Farmer |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| Farmers per cooperative | own district | own district | ✔ | | ✔ | |
| Enrolments and capacity | **own offerings** | own district | ✔ | | ✔ | |
| Outstanding contributions | | | ✔ | ✔ | | |
| Grade distribution | **own offerings** | own district | ✔ | | ✔ | |

* The function is opened by the permission (`REPORT_*`, see `permission-matrix.md`); the **records** are limited in
  `ReportService`: district staff are always pinned to their own district. Asking for another district
  (`?districtId=`) is a **403**, never an empty report. A Field Officer only sees the offerings assigned to them.
* Money reports are for the Finance Officer and the Registrar, the same people who may list contributions.
  Agricultural reports are not shown to the Finance Officer, who must not deal with field or quality data.
* An unknown `districtId` or `seasonId` is a **404**.

## 1. Farmers per cooperative

`GET /api/reports/farmers-per-cooperative?districtId=` (optional)

**Data:** `cooperatives` → `districts`, and `farmers` (their current cooperative and status).

**Calculation:** for each cooperative, the number of farmers in each status (`ACTIVE`, `SUSPENDED`, `WITHDRAWN`,
`RETIRED`) and the total. A `LEFT JOIN` keeps cooperatives that have no farmers (they show 0). A district's farmers are
those of its cooperatives, as in the brief. Sorted by district, then cooperative.

**Totals:** the number of cooperatives and the sum of each column.

## 2. Enrolments and capacity per programme offering

`GET /api/reports/offering-capacity?seasonId=&districtId=` (both optional)

**Data:** `programme_offerings` → `crop_programmes` (district) → `seasons`, the assigned Field Officer, and `enrolments`.

**Calculation, per offering:**
* `enrolled` = enrolments with status `ENROLLED`. Cancelled enrolments free their place and are counted apart
  (`cancelled`).
* `placesLeft` = `maxParticipants − enrolled`; `full` = no place left.
* `capacityUsedPercent` = `enrolled × 100 / maxParticipants`, rounded half-up to 2 decimals (2 of 3 = 66.67).
* `hectaresEnrolled` = sum of the hectares of the `ENROLLED` enrolments.

The places are counted from the enrolments themselves, not read from the `enrolled_count` counter.

**Totals:** the number of offerings and of full offerings, the sum of each column, and the overall capacity used
(`Σ enrolled × 100 / Σ maxParticipants`, not an average of percentages).

## 3. Outstanding contributions per season

`GET /api/reports/outstanding-contributions?seasonId=&districtId=` (both optional)

**Data:** `contribution_obligations` (the current amount owed, after adjustments) → `seasons`, the farmer's cooperative
and district, and `payments`.

**Calculation, per season and district:**
* `amountDue` = sum of the amounts owed.
* `confirmedPaid` = sum of the **CONFIRMED** payments only. `pendingPayments` are shown, but they do **not** count as
  paid until they are confirmed (the same rule as for enrolment and permits). Failed payments count nowhere.
* `outstanding` = `amountDue − confirmedPaid`. This is never negative, because an amount can never be lowered below
  what was already confirmed (Step 6).
* `collectedPercent` = `confirmedPaid × 100 / amountDue`, rounded **down** to 2 decimals, so a report never shows more
  collected than was paid (1 400 of 3 000 = 46.66, not 46.67).
* `fullyPaid` / `partlyPaid` / `unpaid` = the contributions with confirmed ≥ due / between 0 and due / nothing confirmed.

Sorted by season (newest first), then district.

**Totals:** the sum of each column, and the overall collected percentage, calculated from the summed amounts.

## 4. Grade distribution per programme offering

`GET /api/reports/grade-distribution?seasonId=&districtId=` (both optional)

**Data:** `programme_offerings`, their `enrolments`, the `inspection_results` and the current `grading_bands`.

**Calculation, per offering:**
* Only **PUBLISHED** results count: drafts and results awaiting the agronomist's approval are not official.
  A corrected result counts with its corrected grade.
* `grades` = for every grade of the current scale (best first, 0 when nobody got it), the number of published
  results with that grade and `percent` = `count × 100 / published`, rounded half-up to 2 decimals. The shares may
  therefore add up to 99.99 or 100.01. A grade that only exists in an older scale (a published grade keeps the scale
  in force when it was published) is listed after the current grades.
* `averageFinalScore` = `totalFinalScore / published`, rounded half-up to 2 decimals (absent when nothing is published).
* `notYetPublished` = active enrolments without a published result.

**Totals:** the grade counts of all rows together, and the average over **all published results**. Every result
weighs the same: it is not the average of the offering averages.

## Why reports are not paginated

A report is one document: its totals must cover every row of the same snapshot. The rows are few (one per
cooperative, offering, or season and district), unlike the lists of farmers or payments, which are paginated. The
filters `seasonId` and `districtId` keep a report small when needed.

## Tests

* `ReportTest` (integration) builds its own cooperatives, seasons, offerings, payments and results and checks every
  number above, the totals, and the district and own-offering limits (403/404).
* `ReportMathTest` (unit) checks the rounding rules, and that money collected rounds exactly like a farmer's balance.
* `PermissionMatrixTest.reportPermissions` checks who holds which report permission.
* Postman Part I shows the same rules against the running application.

# ACMS endpoint table

Generated from the controllers (`@GetMapping`... ), the URL rules in `SecurityConfig` and the `PermissionMatrix`: the roles column is exactly what the application enforces. 93 endpoints. Interactive documentation of every request and response body: Swagger UI at `/swagger-ui.html`.

**Every answer** uses the envelope `{success, message, data | error, timestamp}` (see the README).

**Errors that apply everywhere:** `401` without a valid token (all endpoints except login, register and verify-email); `403` when the role lacks the permission; `400` when the body or a parameter is invalid; `404` when an id in the URL does not exist. **Record-level 403** (★): the role may use the endpoint, but only for its own district, its own offerings, or (farmer) its own records; another record gives 403.

Lists marked *(paged)* accept `page`, `size`, `sortBy`, `direction` and answer a page.

## Authentication and accounts

| Method | URI | Purpose | Roles | Success | Errors (besides 401/403) |
|---|---|---|---|---|---|
| GET | `/api/audit-logs` | Security audit log (logins, refusals, changes) *(paged)* | Admin | 200 | — |
| POST | `/api/auth/login` | Log in; returns the access token | anyone (no login) | 200 | 400, 401 wrong credentials, 422 email not verified |
| POST | `/api/auth/register` | Self-registration; the very first user becomes System Admin | anyone (no login) | 201 | 400, 409 duplicate username/email |
| GET | `/api/auth/verify-email` | Confirms an email address; called once by the web app's `/verify-email` page, which the link in the email opens | anyone (no login) | 200 | 400 invalid/expired token |
| GET | `/api/auth/me` | Who am I: account, role, district, permissions | any logged-in user | 200 | — |
| GET | `/api/permissions` | The permission matrix (role → permissions) | all roles | 200 | — |
| POST | `/api/users` | Create a staff or farmer account; emails a one-time password | Admin | 201 | 400 district/farmer does not fit the role, 409 duplicate username/email or farmer already has an account |
| GET | `/api/users` | List user accounts *(paged)* | Admin | 200 | — |
| GET | `/api/users/{id}` | One user account | Admin | 200 | 404 |
| PATCH | `/api/users/{id}/role` | Change a role (and its district or farmer) | Admin | 200 | 404, 400 district/farmer does not fit the role, 409 own role / farmer already has an account |
| PATCH | `/api/users/{id}/enabled` | Enable or disable an account | Admin | 200 | 400, 404, 409 own account |
| POST | `/api/users/{id}/reset-password` | Email a new one-time password | Admin | 200 | 404, 422 no email address / account disabled |
| POST | `/api/auth/change-password` | Replace the (one-time) password; the current token stops working, so log in again | any logged-in user | 204 | 400 wrong current password / weak or unchanged password |

## Reference data

| Method | URI | Purpose | Roles | Success | Errors (besides 401/403) |
|---|---|---|---|---|---|
| POST | `/api/cooperatives` | Create a cooperative in a district | Registrar | 201 | 400, 409 duplicate name in the district |
| GET | `/api/cooperatives` | List cooperatives *(paged)* | all roles ★ | 200 | — |
| GET | `/api/cooperatives/{id}` | One cooperative | all roles ★ | 200 | 404 |
| PUT | `/api/cooperatives/{id}` | Rename a cooperative | Registrar | 200 | 400, 404, 409 duplicate name |
| POST | `/api/programmes` | Create a crop programme in a district | Registrar | 201 | 400, 409 duplicate name |
| GET | `/api/programmes` | List crop programmes (district staff: own district) *(paged)* | all roles ★ | 200 | — |
| GET | `/api/programmes/{id}` | One crop programme with its prerequisites | all roles ★ | 200 | 404 |
| PUT | `/api/programmes/{id}` | Rename a crop programme | Registrar | 200 | 400, 404, 409 duplicate name |
| POST | `/api/programmes/{id}/prerequisites` | Add a prerequisite training | Registrar | 201 | 400, 404, 409 already a prerequisite / farmers actively enrolled |
| DELETE | `/api/programmes/{id}/prerequisites/{trainingId}` | Remove a prerequisite training | Registrar | 200 | 404, 409 farmers actively enrolled |
| GET | `/api/districts` | The 30 districts (by province) *(paged)* | all roles | 200 | — |
| GET | `/api/districts/{id}` | One district with its province | all roles | 200 | 404 |
| PUT | `/api/districts/{id}` | Correct a district name | Registrar | 200 | 400, 404, 409 duplicate name |
| GET | `/api/provinces` | The 5 provinces *(paged)* | all roles | 200 | — |
| POST | `/api/trainings` | Create a training | Registrar | 201 | 400, 409 duplicate name |
| GET | `/api/trainings` | List trainings *(paged)* | all roles | 200 | — |
| GET | `/api/trainings/{id}` | One training | all roles | 200 | 404 |
| PUT | `/api/trainings/{id}` | Rename a training | Registrar | 200 | 400, 404, 409 duplicate name |

## Farmers

| Method | URI | Purpose | Roles | Success | Errors (besides 401/403) |
|---|---|---|---|---|---|
| POST | `/api/farmers` | Register a farmer; gives the membership number | Registrar | 201 | 400, 409 duplicate national id |
| GET | `/api/farmers` | Search farmers by name, membership number, cooperative, status; paginated and sortable *(paged)* | Field Officer, Agronomist, Registrar, Finance, Admin ★ | 200 | 400 |
| GET | `/api/farmers/{id}` | One farmer (district staff: own district; farmer: themself) | Farmer, Field Officer, Agronomist, Registrar ★ | 200 | 404 |
| PUT | `/api/farmers/{id}` | Correct name and phone | Registrar | 200 | 400, 404 |
| PATCH | `/api/farmers/{id}/status` | Change status (Active, Suspended, Withdrawn, Retired) | Registrar | 200 | 400, 404, 422 transition not allowed (e.g. Retired → Active) |
| POST | `/api/farmers/{farmerId}/trainings` | Record a completed training | Registrar | 201 | 400, 404, 409 already recorded |
| GET | `/api/farmers/{farmerId}/trainings` | A farmer's completed trainings *(paged)* | Farmer, Field Officer, Agronomist, Registrar ★ | 200 | 404 |
| GET | `/api/farmers/{farmerId}/missing-prerequisites` | Trainings still missing for a programme | Farmer, Field Officer, Agronomist, Registrar ★ | 200 | 404 |
| GET | `/api/me/profile` | The farmer's own profile | Farmer | 200 | — |

## Seasons and offerings

| Method | URI | Purpose | Roles | Success | Errors (besides 401/403) |
|---|---|---|---|---|---|
| POST | `/api/offerings` | Offer a programme in a season: places, land per participant, Field Officer | Registrar | 201 | 400, 409 already offered in that season, 422 not a Field Officer of the district / season ended / land above the season limit |
| GET | `/api/offerings` | List offerings (district staff and farmers: own district) *(paged)* | Farmer, Field Officer, Agronomist, Registrar, Admin ★ | 200 | — |
| GET | `/api/offerings/assigned-to-me` | The calling Field Officer's offerings *(paged)* | Field Officer | 200 | — |
| GET | `/api/offerings/{id}` | One offering with places left | Farmer, Field Officer, Agronomist, Registrar, Admin ★ | 200 | 404 |
| PUT | `/api/offerings/{id}` | Change places or Field Officer | Registrar | 200 | 400, 404, 409 fewer places than enrolled / land change after enrolment, 422 officer or land rules |
| GET | `/api/field-officers` | Enabled Field Officers to assign to an offering, by username; `?districtId=` (optional) limits to one district; id, username and district only | Registrar | 200 | 400 districtId not a number, 404 unknown district |
| POST | `/api/seasons` | Create a season (dates, land limit, distribution day) | Registrar | 201 | 400 dates, 409 duplicate name |
| GET | `/api/seasons` | List seasons *(paged; optional `name`, `enrolmentOpen`)* | all roles | 200 | — |
| GET | `/api/seasons/{id}` | One season with its window status | all roles | 200 | 404 |
| PUT | `/api/seasons/{id}` | Change a season before any window opened | Registrar | 200 | 404, 400 dates, 409 a window was already opened / duplicate name |
| POST | `/api/seasons/{id}/windows/open` | Open the enrolment window | Registrar | 201 | 404, 409 already open, 422 season ended |
| POST | `/api/seasons/{id}/windows/close` | Close the enrolment window | Registrar | 200 | 404, 409 not open |
| GET | `/api/seasons/{id}/windows` | History of the windows of a season *(paged)* | all roles | 200 | 404 |

## Enrolment

| Method | URI | Purpose | Roles | Success | Errors (besides 401/403) |
|---|---|---|---|---|---|
| POST | `/api/offerings/{offeringId}/enrolments` | Farmer enrols (window open, active, prerequisites, land limit, ≥30% paid, place left) | Farmer ★ | 201 | 404, 409 already enrolled / offering full, 422 window closed / not Active / training missing / land limit / under 30% paid |
| GET | `/api/offerings/{offeringId}/enrolments` | Farmers enrolled in an offering *(paged)* | Field Officer, Agronomist, Registrar, Admin ★ | 200 | 404 |
| GET | `/api/me/enrolments` | The farmer's own enrolments *(paged)* | Farmer | 200 | — |
| GET | `/api/enrolments/{id}` | One enrolment | Farmer, Field Officer, Agronomist, Registrar, Admin ★ | 200 | 404 |
| POST | `/api/enrolments/{id}/cancel` | Cancel an enrolment (frees the place) | Farmer, Registrar ★ | 200 | 404, 400 reason required (Registrar), 409 already cancelled, 422 window closed / season ended |

## Contributions and payments

| Method | URI | Purpose | Roles | Success | Errors (besides 401/403) |
|---|---|---|---|---|---|
| POST | `/api/contributions` | Record a farmer's input contribution for a season | Finance | 201 | 400, 409 already exists for that farmer and season |
| GET | `/api/contributions` | List contributions with balances *(paged)* | Registrar, Finance | 200 | — |
| GET | `/api/contributions/{id}` | One contribution with its balance | Farmer, Registrar, Finance ★ | 200 | 404 |
| POST | `/api/contributions/{id}/adjustments` | Change the amount owed (reason recorded) | Finance | 200 | 404, 400 same amount, 422 below what is already paid |
| GET | `/api/contributions/{id}/adjustments` | History of amount changes *(paged)* | Registrar, Finance | 200 | 404 |
| GET | `/api/contributions/{id}/payments` | Payments of a contribution *(paged)* | Farmer, Registrar, Finance ★ | 200 | 404 |
| GET | `/api/me/contributions` | The farmer's own contributions and balances *(paged)* | Farmer | 200 | — |
| POST | `/api/payments` | Record a payment request (idempotent on transactionReference) | Finance | 201; 200 repeat | 400, 409 same reference with different details, 422 more than still owed |
| POST | `/api/payments/{id}/confirm` | Confirm a payment (now it counts) | Finance | 200 | 400, 404, 409 failed / other provider reference, 422 would pay more than owed |
| POST | `/api/payments/{id}/fail` | Mark a payment failed (reason required) | Finance | 200 | 400, 404, 409 not pending any more |
| GET | `/api/payments/{id}` | One payment | Farmer, Registrar, Finance ★ | 200 | 404 |

## Inspections and results

| Method | URI | Purpose | Roles | Success | Errors (besides 401/403) |
|---|---|---|---|---|---|
| GET | `/api/grading-scale` | The grading scale (score → grade) | all roles | 200 | — |
| PUT | `/api/grading-scale` | Replace the grading scale | Registrar | 200 | 400 no band at 0 / duplicate grade or minimum |
| POST | `/api/offerings/{offeringId}/inspections` | Create an inspection (max score, weight) | Field Officer ★ | 201 | 400, 404, 409 duplicate name / results already submitted, 422 weights above 100% |
| GET | `/api/offerings/{offeringId}/inspections` | Inspections of an offering | Field Officer, Agronomist, Registrar, Admin ★ | 200 | 404 |
| PUT | `/api/inspections/{id}` | Change an inspection | Field Officer ★ | 200 | 400, 404, 409 duplicate name / scores entered / results submitted, 422 weights above 100% |
| DELETE | `/api/inspections/{id}` | Delete an inspection without scores | Field Officer ★ | 204 | 404, 409 scores entered / results submitted |
| PUT | `/api/enrolments/{enrolmentId}/scores` | Enter or change draft scores | Field Officer ★ | 200 | 404, 400 inspection listed twice, 409 result not a draft, 422 score out of range / enrolment cancelled |
| POST | `/api/results/{id}/submit` | Submit for approval (computes final score and grade) | Field Officer ★ | 200 | 404, 409 not a draft, 422 no inspections / weights not 100% / a score missing |
| POST | `/api/results/{id}/approve` | District Agronomist publishes the result | Agronomist ★ | 200 | 404, 409 not submitted |
| POST | `/api/results/{id}/return` | Return for rework (reason required) | Agronomist ★ | 200 | 400, 404, 409 not submitted |
| POST | `/api/results/{id}/corrections` | Authorised correction of a published score (original kept) | Agronomist ★ | 200 | 404, 400 same score, 409 not published, 422 score out of range |
| GET | `/api/results/{id}` | One result with its scores | Farmer, Field Officer, Agronomist, Registrar, Admin ★ | 200 | 404 |
| GET | `/api/results/{id}/corrections` | Correction history of a result | Farmer, Field Officer, Agronomist, Registrar, Admin ★ | 200 | 404 |
| GET | `/api/offerings/{offeringId}/results` | Results of an offering *(paged)* | Field Officer, Agronomist, Registrar, Admin ★ | 200 | 404 |
| GET | `/api/me/results` | The farmer's own published results *(paged)* | Farmer | 200 | — |
| GET | `/api/farmers/{farmerId}/results` | A farmer's published results (farmer: only their own) *(paged)* | Farmer, Field Officer, Agronomist, Registrar, Admin ★ | 200 | 404 |

## Collection permits

| Method | URI | Purpose | Roles | Success | Errors (besides 401/403) |
|---|---|---|---|---|---|
| POST | `/api/enrolments/{enrolmentId}/permit` | Farmer generates the collection permit (every attempt recorded) | Farmer ★ | 201; 200 repeat | 404, 422 not 100% paid / not active / enrolment cancelled / too late |
| GET | `/api/me/permits` | The farmer's own permits *(paged)* | Farmer | 200 | — |
| GET | `/api/permits/verify/{permitNumber}` | Storekeeper checks a permit number against the current data | Field Officer, Agronomist, Registrar, Finance, Admin | 200 | 404 |
| GET | `/api/permits/attempts` | Every permit attempt: issued, repeated, refused (with reason) *(paged)* | Registrar, Admin | 200 | — |

## Management reports

| Method | URI | Purpose | Roles | Success | Errors (besides 401/403) |
|---|---|---|---|---|---|
| GET | `/api/reports/farmers-per-cooperative` | Report: farmers per cooperative and status | Field Officer, Agronomist, Registrar, Admin ★ | 200 | — |
| GET | `/api/reports/offering-capacity` | Report: enrolments and capacity per offering | Field Officer, Agronomist, Registrar, Admin ★ | 200 | — |
| GET | `/api/reports/outstanding-contributions` | Report: outstanding contributions per season and district | Registrar, Finance ★ | 200 | — |
| GET | `/api/reports/grade-distribution` | Report: grade distribution per offering | Field Officer, Agronomist, Registrar, Admin ★ | 200 | — |

## Permission behind each endpoint

For reference, the permission each URL rule requires (see `docs/permission-matrix.md` for the roles holding it):

| Method | URI | Permission |
|---|---|---|
| GET | `/api/audit-logs` | `AUDIT_LOG_READ` |
| POST | `/api/auth/login` | `permitAll` |
| POST | `/api/auth/register` | `permitAll` |
| GET | `/api/auth/verify-email` | `permitAll` |
| GET | `/api/auth/me` | `authenticated` |
| POST | `/api/contributions` | `CONTRIBUTION_MANAGE` |
| GET | `/api/contributions` | `CONTRIBUTION_LIST` |
| GET | `/api/contributions/{id}` | `CONTRIBUTION_READ` |
| POST | `/api/contributions/{id}/adjustments` | `CONTRIBUTION_MANAGE` |
| GET | `/api/contributions/{id}/adjustments` | `CONTRIBUTION_LIST` |
| GET | `/api/contributions/{id}/payments` | `CONTRIBUTION_READ` |
| GET | `/api/me/contributions` | `CONTRIBUTION_READ_OWN` |
| POST | `/api/cooperatives` | `REFERENCE_MANAGE` |
| GET | `/api/cooperatives` | `REFERENCE_READ` |
| GET | `/api/cooperatives/{id}` | `REFERENCE_READ` |
| PUT | `/api/cooperatives/{id}` | `REFERENCE_MANAGE` |
| POST | `/api/programmes` | `REFERENCE_MANAGE` |
| GET | `/api/programmes` | `REFERENCE_READ` |
| GET | `/api/programmes/{id}` | `REFERENCE_READ` |
| PUT | `/api/programmes/{id}` | `REFERENCE_MANAGE` |
| POST | `/api/programmes/{id}/prerequisites` | `REFERENCE_MANAGE` |
| DELETE | `/api/programmes/{id}/prerequisites/{trainingId}` | `REFERENCE_MANAGE` |
| GET | `/api/districts` | `REFERENCE_READ` |
| GET | `/api/districts/{id}` | `REFERENCE_READ` |
| PUT | `/api/districts/{id}` | `REFERENCE_MANAGE` |
| POST | `/api/offerings/{offeringId}/enrolments` | `ENROLMENT_CREATE` |
| GET | `/api/offerings/{offeringId}/enrolments` | `ENROLMENT_LIST_FOR_OFFERING` |
| GET | `/api/me/enrolments` | `ENROLMENT_READ_OWN` |
| GET | `/api/enrolments/{id}` | `ENROLMENT_READ` |
| POST | `/api/enrolments/{id}/cancel` | `ENROLMENT_CANCEL` |
| POST | `/api/farmers` | `FARMER_MANAGE` |
| GET | `/api/farmers` | `FARMER_LIST` |
| GET | `/api/farmers/{id}` | `FARMER_READ` |
| PUT | `/api/farmers/{id}` | `FARMER_MANAGE` |
| PATCH | `/api/farmers/{id}/status` | `FARMER_STATUS_CHANGE` |
| POST | `/api/farmers/{farmerId}/trainings` | `TRAINING_RECORD_MANAGE` |
| GET | `/api/farmers/{farmerId}/trainings` | `TRAINING_RECORD_READ` |
| GET | `/api/farmers/{farmerId}/missing-prerequisites` | `TRAINING_RECORD_READ` |
| GET | `/api/grading-scale` | `GRADING_SCALE_READ` |
| PUT | `/api/grading-scale` | `GRADING_SCALE_MANAGE` |
| POST | `/api/offerings/{offeringId}/inspections` | `INSPECTION_MANAGE` |
| GET | `/api/offerings/{offeringId}/inspections` | `INSPECTION_READ` |
| PUT | `/api/inspections/{id}` | `INSPECTION_MANAGE` |
| DELETE | `/api/inspections/{id}` | `INSPECTION_MANAGE` |
| GET | `/api/me/profile` | `PROFILE_READ_OWN` |
| POST | `/api/payments` | `PAYMENT_MANAGE` |
| POST | `/api/payments/{id}/confirm` | `PAYMENT_MANAGE` |
| POST | `/api/payments/{id}/fail` | `PAYMENT_MANAGE` |
| GET | `/api/payments/{id}` | `CONTRIBUTION_READ` |
| GET | `/api/permissions` | `PERMISSION_MATRIX_READ` |
| POST | `/api/enrolments/{enrolmentId}/permit` | `PERMIT_GENERATE` |
| GET | `/api/me/permits` | `PERMIT_READ_OWN` |
| GET | `/api/permits/verify/{permitNumber}` | `PERMIT_VERIFY` |
| GET | `/api/permits/attempts` | `PERMIT_ATTEMPT_READ` |
| POST | `/api/offerings` | `OFFERING_MANAGE` |
| GET | `/api/offerings` | `OFFERING_READ` |
| GET | `/api/offerings/assigned-to-me` | `OFFERING_READ_ASSIGNED` |
| GET | `/api/offerings/{id}` | `OFFERING_READ` |
| PUT | `/api/offerings/{id}` | `OFFERING_MANAGE` |
| GET | `/api/field-officers` | `OFFERING_MANAGE` |
| GET | `/api/provinces` | `REFERENCE_READ` |
| GET | `/api/reports/farmers-per-cooperative` | `REPORT_FARMERS_PER_COOPERATIVE` |
| GET | `/api/reports/offering-capacity` | `REPORT_OFFERING_CAPACITY` |
| GET | `/api/reports/outstanding-contributions` | `REPORT_OUTSTANDING_CONTRIBUTIONS` |
| GET | `/api/reports/grade-distribution` | `REPORT_GRADE_DISTRIBUTION` |
| PUT | `/api/enrolments/{enrolmentId}/scores` | `RESULT_ENTER` |
| POST | `/api/results/{id}/submit` | `RESULT_ENTER` |
| POST | `/api/results/{id}/approve` | `RESULT_APPROVE` |
| POST | `/api/results/{id}/return` | `RESULT_APPROVE` |
| POST | `/api/results/{id}/corrections` | `RESULT_CORRECT` |
| GET | `/api/results/{id}` | `RESULT_READ` |
| GET | `/api/results/{id}/corrections` | `RESULT_READ` |
| GET | `/api/offerings/{offeringId}/results` | `INSPECTION_READ` |
| GET | `/api/me/results` | `RESULT_READ_OWN` |
| GET | `/api/farmers/{farmerId}/results` | `RESULT_READ` |
| POST | `/api/seasons` | `SEASON_MANAGE` |
| GET | `/api/seasons` | `SEASON_READ` |
| GET | `/api/seasons/{id}` | `SEASON_READ` |
| PUT | `/api/seasons/{id}` | `SEASON_MANAGE` |
| POST | `/api/seasons/{id}/windows/open` | `SEASON_MANAGE` |
| POST | `/api/seasons/{id}/windows/close` | `SEASON_MANAGE` |
| GET | `/api/seasons/{id}/windows` | `SEASON_READ` |
| POST | `/api/trainings` | `REFERENCE_MANAGE` |
| GET | `/api/trainings` | `REFERENCE_READ` |
| GET | `/api/trainings/{id}` | `REFERENCE_READ` |
| PUT | `/api/trainings/{id}` | `REFERENCE_MANAGE` |
| POST | `/api/users` | `USER_MANAGE` |
| GET | `/api/users` | `USER_MANAGE` |
| GET | `/api/users/{id}` | `USER_MANAGE` |
| PATCH | `/api/users/{id}/role` | `USER_MANAGE` |
| PATCH | `/api/users/{id}/enabled` | `USER_MANAGE` |
| POST | `/api/users/{id}/reset-password` | `USER_MANAGE` |
| POST | `/api/auth/change-password` | `authenticated` |

# ACMS permission matrix

Generated from `src/main/java/com/acms/security/PermissionMatrix.java` (also served live at `GET /api/permissions`).

A ✔ means the role holds the permission. Holding a permission opens the **function**; the services then check the
**record** (a farmer only their own data, Field Officers and Agronomists only their own district or offerings).

| Area | Permission | What it allows | Farmer | Field Officer | District Agronomist | Union Registrar | Finance Officer | System Admin |
|---|---|---|:-:|:-:|:-:|:-:|:-:|:-:|
| Reference data | `REFERENCE_READ` | Read provinces, districts, cooperatives, trainings and crop programmes | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| Reference data | `REFERENCE_MANAGE` | Create and change cooperatives, trainings, programmes and prerequisites; edit districts |  |  |  | ✔ |  |  |
| Seasons | `SEASON_READ` | Read seasons and enrolment windows | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| Seasons | `SEASON_MANAGE` | Create and change seasons; open and close enrolment windows |  |  |  | ✔ |  |  |
| Programme offerings | `OFFERING_READ` | Read programme offerings (own district only for farmers, officers and agronomists) | ✔ | ✔ | ✔ | ✔ |  | ✔ |
| Programme offerings | `OFFERING_READ_ASSIGNED` | Read the offerings assigned to me |  | ✔ |  |  |  |  |
| Programme offerings | `OFFERING_MANAGE` | Create and change programme offerings |  |  |  | ✔ |  |  |
| Enrolment | `ENROLMENT_CREATE` | Enrol myself in an offering | ✔ |  |  |  |  |  |
| Enrolment | `ENROLMENT_READ_OWN` | Read my own enrolments | ✔ |  |  |  |  |  |
| Enrolment | `ENROLMENT_READ` | Read one enrolment (own, or within my offerings or district) | ✔ | ✔ | ✔ | ✔ |  | ✔ |
| Enrolment | `ENROLMENT_LIST_FOR_OFFERING` | List the farmers enrolled in an offering (own offering or district) |  | ✔ | ✔ | ✔ |  | ✔ |
| Enrolment | `ENROLMENT_CANCEL` | Cancel an enrolment (farmers: their own, while the window is open) | ✔ |  |  | ✔ |  |  |
| Contributions and payments | `CONTRIBUTION_READ_OWN` | Read my own contributions and balances | ✔ |  |  |  |  |  |
| Contributions and payments | `CONTRIBUTION_READ` | Read one contribution, its payments, or one payment (farmers: their own) | ✔ |  |  | ✔ | ✔ |  |
| Contributions and payments | `CONTRIBUTION_LIST` | List all contributions and their adjustment history |  |  |  | ✔ | ✔ |  |
| Contributions and payments | `CONTRIBUTION_MANAGE` | Record contributions and adjust the amount owed |  |  |  |  | ✔ |  |
| Contributions and payments | `PAYMENT_MANAGE` | Record, confirm and fail payments |  |  |  |  | ✔ |  |
| Farmers | `FARMER_LIST` | Search farmers (own district only for officers and agronomists) |  | ✔ | ✔ | ✔ | ✔ | ✔ |
| Farmers | `FARMER_READ` | Read one farmer (farmers: only themselves) | ✔ | ✔ | ✔ | ✔ |  |  |
| Farmers | `FARMER_MANAGE` | Register farmers and change their details |  |  |  | ✔ |  |  |
| Farmers | `FARMER_STATUS_CHANGE` | Change a farmer's status (Active, Suspended, Withdrawn, Retired) |  |  |  | ✔ |  |  |
| Farmers | `PROFILE_READ_OWN` | Read my own farmer profile | ✔ |  |  |  |  |  |
| Training records | `TRAINING_RECORD_READ` | Read a farmer's completed trainings and missing prerequisites | ✔ | ✔ | ✔ | ✔ |  |  |
| Training records | `TRAINING_RECORD_MANAGE` | Record a completed training for a farmer |  |  |  | ✔ |  |  |
| Administration | `USER_MANAGE` | Create accounts, change roles, enable/disable, reset passwords |  |  |  |  |  | ✔ |
| Administration | `AUDIT_LOG_READ` | Read the audit log |  |  |  |  |  | ✔ |
| Administration | `PERMISSION_MATRIX_READ` | Read this permission matrix | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |

A user who must still replace the emailed one-time password holds **no** permission: only `GET /api/auth/me`
and `POST /api/auth/change-password` work until the password is changed.

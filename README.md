# Leaves Management System (Backend)

## Technologies Used
- Java (21+)
- Spring Boot (4.0+)
- Spring Data JPA
- PostgreSQL (for development and production)
- H2 Database (for testing)

## Features

### Get All Leaves — `GET /api/leaves`

Allows an authenticated user to retrieve leave records. Based on the `scope` parameter, a user can either view their **own leaves** or (if they are a Manager) view **all employees' leaves**.

---

#### Request Headers

| Header    | Required | Description                         |
|-----------|----------|-------------------------------------|
| `user_id` | Yes      | UUID of the user making the request |

#### Query Parameters

| Parameter | Required | Default | Accepted Values                   |
|-----------|----------|---------|-----------------------------------|
| `scope`   | No       | `self`  | `self`, `organisation`            |
| `status`  | No       | —       | `upcoming`,`ongoing`, `completed` |

---

#### How Scope Works

| Scope  | Who can use it       | What it returns                        |
|--------|----------------------|----------------------------------------|
| `self` | Any user             | Only the requesting user's own leaves  |
| `team` | Manager only         | Leaves of all employees in the system  |

---

#### How Status Works

| Status      | What it returns                          |
|-------------|------------------------------------------|
| _(not set)_ | All leaves regardless of date            |
| `upcoming`  | Only leaves with a date **after** today  |
| `ongoing`   | Only leaves with a date  today           |
| `completed` | Only leaves with a date **before** today |

---

#### Example Requests

**Employee fetching their own upcoming leaves**
```
GET /api/leaves?scope=self&status=upcoming
Header: user_id: <your-uuid>
```

**Manager fetching all team leaves**
```
GET /api/leaves?scope=organisation
Header: user_id: <manager-uuid>
```

---
#### Response
```json
{
  "success": true,
  "message": "Leaves retrieved successfully",
  "data": [
    {
      "id": "uuid",
      "date": "2026-03-30",
      "employeeName": "Priyansh",
      "type": "Annual Leave",
      "duration": "FULL_DAY",
      "startTime": "09:00:00",
      "applyOn": "2026-03-01T10:00:00",
      "reason": "Taking annual leave"
    }
  ]
}
```
### Apply Leave — `POST /api/leaves`

Allows an user to apply for one or more leaves in a single request. Each date in the request is processed individually. Weekends, already-applied dates, and dates outside the allowed range are automatically skipped or rejected.
 
---

#### Request Headers

| Header       | Required | Description                         |
|--------------|----------|-------------------------------------|
| `user_id`    | Yes      | UUID of the user making the request |
| `Content-Type` | Yes    | `application/json`                  |
 
---

#### Request Body

| Field            | Type               | Required | Description                                              |
|------------------|--------------------|----------|----------------------------------------------------------|
| `leaveCategoryId`| UUID               | Yes      | ID of the leave category (e.g. Annual Leave, Sick Leave) |
| `dates`          | Array of dates     | Yes      | One or more dates in `YYYY-MM-DD` format                 |
| `duration`       | String (enum)      | Yes      | `FULL_DAY` or `HALF_DAY`                                 |
| `startTime`      | String (LocalTime) | Yes      | Start time in `HH:mm` format                          |
| `description`    | String             | Yes      | Reason for the leave (max 1000 characters)               |
 
---

#### Date Validation Rules

| Rule                        | Is Allowed ?                                                        |
|-----------------------------|---------------------------------------------------------------------|
| Past dates (previous months)| Rejected — only past dates within the **current month** are allowed |
| Past dates (current month)  | Allowed                                                             |
| Today                       | Allowed                                                             |
| Future dates (current year) | Allowed                                                             |
| Future dates (next year+)   | Rejected — only dates within the **current year** are allowed       |
| Weekends (Saturday/Sunday)  | Skipped automatically                                               |
| Already applied dates       | Skipped silently — only new dates are saved                         |

> If **all** provided dates are invalid (out of range, weekends, or already applied), the request fails with an appropriate error.
 
---

#### Example Request

**Employee applying for leave on multiple dates**
```
POST /api/leaves
Header: user_id: <your-uuid>
Content-Type: application/json
```

```json
{
  "leaveCategoryId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "dates": ["2026-04-10", "2026-04-11", "2026-04-12"],
  "duration": "FULL_DAY",
  "startTime": "09:00:00",
  "description": "Dummy Description"
}
```
 
---

#### Response

**201 Created** — Leave(s) applied successfully.

```json
{
  "success": true,
  "message": "Leaves applied successfully",
  "data": [
    {
      "id": "uuid",
      "date": "2026-04-10",
      "leaveCategoryName": "Annual Leave",
      "duration": "FULL_DAY",
      "startTime": "09:00:00",
      "description": "Dummy Description"
    },
    {
      "id": "uuid",
      "date": "2026-04-13",
      "leaveCategoryName": "Annual Leave",
      "duration": "FULL_DAY",
      "startTime": "09:00:00",
      "description": "Dummy Description"
    }
  ]
}
```

 Note: The response contains one entry per successfully saved date. Skipped dates (weekends or duplicates) will not appear in the response.
 
---

#### Error Responses

| HTTP Status | Scenario                                                              |
|-------------|-----------------------------------------------------------------------|
| `400`       | All dates fall outside the allowed range (previous months / next year+) |
| `400`       | All valid dates fall on weekends                                      |
| `400`       | Request body is missing required fields or has validation errors      |
| `404`       | Provided `user_id` does not exist                                     |
| `404`       | Provided `leaveCategoryId` does not exist                             |
| `409`       | All valid working dates have already been applied for                 |

**Example error response:**

```json
{
  "statusCode": "400",
  "code": "Bad Request",
  "message": "Dates must be within the current month for past dates, or within the current year for future dates."
}
```
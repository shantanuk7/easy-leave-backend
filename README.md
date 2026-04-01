# Leaves Management System (Backend)

## Technologies Used
- Java (21+)
- Spring Boot (4.0+)
- Spring Data JPA
- PostgreSQL (for development and production)
- H2 Database (for testing)

## Features

### Authentication System

This project uses a hybrid OAuth2 + JWT-based authentication system to securely authenticate users and protect backend APIs.

#### The authentication flow consists of:
- OAuth2 Login (Google)
- User Persistence in Database
- JWT Token Generation
- JWT-based Authentication for API requests

#### Authentication Flow
**1. Login via Google**
- User hits: `/oauth2/authorization/google`
- Redirected to Google login
- Google returns user details (name, email)

**2. User Handling**
- Backend checks:
   - If user exists → fetch from DB
   - If new user → create user with default role (EMPLOYEE)
- Only allowed email domain users can login

**3. JWT Generation**
- After successful login:
   - JWT token is generated with:
        - id 
        - email 
        - role
   - Token is stored in HTTP-only cookie

**4. Redirect to Frontend**
- User is redirected to: `REDIRECT_FRONTEND_URL`

**5. API Authentication**
- Every request:
  - JWT is read from cookies
  - Token is validated
  - User is fetched from DB
  - User is set in Spring SecurityContext

#### Configuration
**Environment Variables:** Follow `.env.example` for required variables.

#### Protected Routes
- All routes require authentication
- Except
```bash
/api/auth/**
/oauth2/**
```

---

### Get All Leaves — `GET /api/leaves`

Allows an authenticated user to retrieve leave records. Based on the `scope` parameter, a user can either view their **own leaves** or (if they are a Manager) view **all employees' leaves**.

---

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
```

**Manager fetching all team leaves**
```
GET /api/leaves?scope=organisation
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

### Get Leave Categories — `GET /api/leave-categories`

Allows a user to retrieve all available leave categories (for example, Annual Leave, Sick Leave) to use while applying for leave.

---

#### Request Headers

No custom headers are required for this API.

#### Query Parameters

This API does not accept any query parameters.

---

#### Example Request

```
GET /api/leave-categories
```

---

#### Response

**200 OK** — Leave categories retrieved successfully.

```json
{
  "success": true,
  "message": "Leave Categories retrieved successfully",
  "data": [
    {
      "id": "uuid",
      "name": "Annual Leave"
    },
    {
      "id": "uuid",
      "name": "Bereavement Leave"
    }
  ]
}
```

Note: If no leave categories exist, the API returns `200 OK` with an empty array in `data`.

---
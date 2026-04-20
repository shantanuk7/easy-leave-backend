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

### Get Authenticated User — `GET /api/auth/me`

Allows to retrieve details of the currently authenticated user. 
It enables the frontend to fetch user information (such as name, email, and role) after authentication.

#### Endpoint
Get Current User:
```bash
GET /api/auth/me
```

#### Response
- Success (200 OK)
```json
{
    "success": true,
    "message": "User retrieved successfully",
    "data": {
        "id": "uuid",
        "name": "Raj",
        "email": "raj@technogise.com",
        "role": "EMPLOYEE"
    }
}
```

- Error (404 Not Found)
```json
{
    "statusCode": "404",
    "code": "NOT_FOUND",
    "message": "User not found"
}
```

---

### Get All Leaves — `GET /api/leaves`

Allows an authenticated user to retrieve leave records. Users can view their own leaves, while Managers can view leaves for the entire organization or specific employees.

---

#### Query Parameters

| Parameter | Required | Default | Accepted Values | Description |
| :--- | :--- | :--- | :--- | :--- |
| `scope` | No | `self` | `self`, `organisation` | Determines the breadth of the search. |
| `status` | No | — | `upcoming`, `ongoing`, `completed` | Filters leaves based on the current date. |
| `empId` | No | — | UUID | Filter by a specific employee (Requires `organisation` scope). |
| `year` | No | Current | Integer (e.g., 2026) | Filter by year (Used in conjunction with `empId`). |

---

#### How Scope and Authorization Work

| Scope | Who can use it | Behavior |
| :--- | :--- | :--- |
| `self` | Any User | Returns only the requesting user's own leaves. |
| `organisation` | Manager Only | Returns leaves for all employees or a specific employee if `empId` is provided. |

**Note:** If `empId` is provided, the `scope` must be set to `organisation`. Providing an `empId` with `scope=self` will result in a `400 Bad Request`.

---

#### How Status Works


If the `status` parameter is used, the system filters the results based on the current date:* **None provided:** Returns all leaves regardless of date.* **upcoming:** Returns leaves with a date after today.* **ongoing:** Returns leaves with today's date.* **completed:** Returns leaves with a date before today.



---
### Date Validation Rules



When applying for or updating leaves, the following rules apply:1.  **Past Dates:** Must be within the current calendar month.2.  **Future Dates:** Must be within the current calendar year.3.  **Weekends:** Leaves cannot be applied for or moved to Saturdays or Sundays.



---
### Example Requests**Employee fetching their own upcoming leaves**

GET /api/leaves?scope=self&status=upcoming



**Manager fetching all leaves for the organization**

GET /api/leaves?scope=organisation



**Manager fetching leaves for a specific employee in a specific year**

GET /api/leaves?scope=organisation&empId=550e8400-e29b-41d4-a716-446655440000&year=2026

---
### Response Format
```json
{
  "success": true,
  "message": "Leaves retrieved successfully",
  "data": [
    {
      "id": "uuid",
      "date": "2026-04-20",
      "employeeName": "Priyansh",
      "type": "Annual Leave",
      "duration": "FULL_DAY",
      "startTime": "09:00:00",
      "updatedAt": "2026-04-01T10:00:00",
      "description": "Taking annual leave"
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
## Get All Users — `GET /api/users`

Allows a **Manager** or **Admin** to fetch a paginated list of all users in the system.  
The users are sorted alphabetically by name by default. Employees cannot access this endpoint.

---

### Query Parameters

| Parameter | Type   | Default  | Description                                 |
|-----------|--------|----------|---------------------------------------------|
| page      | int    | 0        | Page number (0-indexed)                     |
| size      | int    | 50       | Number of users per page                     |

---
### Example Request
#### GET /api/users?page=0&size=20
#### Authorization: Bearer <JWT_TOKEN>
---
#### Actual Query
```
SELECT id, email, name, role
FROM users
ORDER BY name ASC
LIMIT 20 OFFSET 0;
```
#### Response

**200 OK**

```json
{
  "success": true,
  "message": "Users retrieved successfully",
  "data": [
    {
      "id": "27ba9c6d-72eb-4231-a662-bfe752130fc8",
      "email": "employee@gmail.com",
      "name": "EMPLOYEE",
      "role": "EMPLOYEE"
    },
    {
      "id": "01598c0a-74e8-427f-9490-857da36b86f1",
      "email": "manager@gmail.com",
      "name": "MANAGER",
      "role": "MANAGER"
    }
  ]
}
```
 
---

 

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

### Update Scheduled Leave Endpoint

```bash
PATCH /api/leaves/{id}
```

---

#### Request Body

| Field           | Type      | Required | Description                          |
| --------------- | --------- | -------- | ------------------------------------ |
| leaveCategoryId | UUID      | Yes      | Must be a valid leave category ID    |
| date            | LocalDate | Yes      | Format: `YYYY-MM-DD`                 |
| duration        | String    | Yes      | `FULL_DAY` or `HALF_DAY`             |
| startTime       | LocalTime | Yes      | Format: `HH:mm:ss`                   |
| description     | String    | Yes      | Max 1000 characters, cannot be blank |

---

#### Example Request

```json
{
  "leaveCategoryId": "4a53c482-2998-436b-b663-2d6671667c0a",
  "date": "2026-04-22",
  "duration": "HALF_DAY",
  "startTime": "10:00:00",
  "description": "Updated reason for leave"
}
```

---

#### Validation Rules

| Rule                  | Description                                          | Error           |
| --------------------- | ---------------------------------------------------- | --------------- |
| Ownership             | Only the owner of the leave can update it            | 403 Forbidden   |
| Existing Date Range   | Existing leave must be within allowed editable range | 400 Bad Request |
| New Date Range        | New date must be within allowed range                | 400 Bad Request |
| Weekend Restriction   | Cannot update leave to Saturday or Sunday            | 400 Bad Request |
| Conflict Check        | No duplicate leave allowed on same date              | 409 Conflict    |
| Leave Category Exists | Category must exist                                  | 404 Not Found   |

---

#### Response

**200 OK**

```json
{
  "success": true,
  "message": "Leave updated successfully",
  "data": {
    "id": "8496840a-c0b2-4e03-bc54-0a9d47e00a2c",
    "date": "2026-04-22",
    "leaveCategoryName": "ANNUAL",
    "duration": "HALF_DAY",
    "startTime": "10:00:00",
    "description": "Updated reason for leave"
  }
}
```

---

#### Error Responses

| HTTP Status | Scenario                            |
| ----------- | ----------------------------------- |
| 400         | Invalid date / weekend / validation |
| 403         | User not owner                      |
| 404         | Leave or category not found         |
| 409         | Leave already exists on that date   |

---
### Employee Leave Metrics - `GET /api/dashboard/manager`
Provides manager-specific dashboard metrics including total employees, employees on leave today, and employees on leave tomorrow.

**Endpoint:** `GET /api/dashboard/manager`  
**Authorization:** Requires `MANAGER` role

**Response:**
```json
{
  "success": true,
  "message": "Manager Dashboard data retrieved successfully",
  "data": {
    "totalEmployees": 50,
    "totalEmployeesOnLeaveToday": 5,
    "totalEmployeesOnLeaveTomorrow": 3
  }
}
```
---
## Logout — `POST /api/auth/logout`
Clears the JWT authentication cookie, effectively logging the user out of the system.

---

### Request Body
No request body required.

---

### Example Request
#### POST /api/auth/logout

### Response
{
"success": true,
"message": "Logout successful",
"data": null
}

### Notes
- Clears the `token` cookie by setting it with an empty value and `maxAge=0`
- Cookie attributes (`secure`, `path`, `sameSite`) match those set during login in `OAuth2LoginSuccessHandler`


## Update User Role — `PATCH /api/users/role`

Allows an **ADMIN** to update the role of a user in the system.

---

### Request Body

| Field       | Type | Required | Description                          |
|------------|------|----------|--------------------------------------|
| employeeId | UUID | Yes      | ID of the user whose role is updated |
| role       | Enum | Yes      | New role (`EMPLOYEE`, `MANAGER`, `ADMIN`) |

---

### Example Request
#### PATCH /api/users/role

```
{
  "employeeId": "27ba9c6d-72eb-4231-a662-bfe752130fc8",
  "role": "MANAGER"
}
```
### Response
```
{
  "success": true,
  "message": "Role updated successfully",
  "data": null
}
```
---

### Add holiday - `POST /api/holidays`
This API allows `ADMIN` users to create holidays in the system with proper validations and error handling.

**Endpoint:** `POST /api/holidays`

**Authorization:** Only users with `ADMIN` role can access this API

**Request Body:**
```json
{
    "name": "Diwali",
    "type": "FIXED",
    "date": "2026-11-08"
}
```

**Response:**
Success Response (201)
```json
{
  "success": true,
  "message": "Holiday created successfully",
  "data": {
    "id": "uuid",
    "name": "Diwali",
    "type": "FIXED",
    "date": "2026-11-08"
  }
}
```

---

### Add holiday - `POST /api/holidays`
This API allows `ADMIN` users to create holidays in the system with proper validations and error handling.

**Endpoint:** `POST /api/holidays`

**Authorization:** Only users with `ADMIN` role can access this API

**Request Body:**
```json
{
    "name": "Diwali",
    "type": "FIXED",
    "date": "2026-11-08"
}
```

**Response:**
Success Response (201)
```json
{
  "success": true,
  "message": "Holiday created successfully",
  "data": {
    "id": "uuid",
    "name": "Diwali",
    "type": "FIXED",
    "date": "2026-11-08"
  }
}
```
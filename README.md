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

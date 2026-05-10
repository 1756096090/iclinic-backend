# 📋 APPOINTMENT API - DOCUMENTACIÓN COMPLETA PARA FRONTEND

## 🌐 Base URL
```
http://localhost:8080/api/v1/appointments
```

---

## 📌 ENDPOINTS

### 1. **Obtener Slots Disponibles**
Retorna los horarios disponibles para una sucursal en una fecha específica.

**Endpoint:**
```
GET /api/v1/appointments/available-slots
```

**Parámetros Query:**
| Parámetro | Tipo | Requerido | Descripción | Ejemplo |
|-----------|------|-----------|-------------|---------|
| `branchId` | Long | ✅ Sí | ID de la sucursal | `1` |
| `doctorId` | Long | ✅ Sí | ID del doctor | `2` |
| `date` | String | ✅ Sí | Fecha (yyyy-MM-dd) | `2026-05-15` |

**Request Example:**
```bash
GET /api/v1/appointments/available-slots?branchId=1&doctorId=2&date=2026-05-15
```

**Response 200 OK:**
```json
[
  {
    "start": "2026-05-15T09:00:00",
    "end": "2026-05-15T09:30:00"
  },
  {
    "start": "2026-05-15T09:30:00",
    "end": "2026-05-15T10:00:00"
  },
  {
    "start": "2026-05-15T10:00:00",
    "end": "2026-05-15T10:30:00"
  }
]
```

**Errores:**
| Status | Código | Descripción |
|--------|--------|-------------|
| 404 | NOT_FOUND | Sucursal no existe |
| 400 | BAD_REQUEST | Fecha inválida o parámetros faltantes |

---

### 2. **Crear Nueva Cita**
Crea una nueva cita médica con validaciones de disponibilidad.

**Endpoint:**
```
POST /api/v1/appointments
```

**Body Request:**
```json
{
  "companyId": 1,
  "branchId": 1,
  "contactId": 1,
  "doctorId": 2,
  "scheduledStart": "2026-05-15T10:00:00",
  "scheduledEnd": "2026-05-15T10:30:00",
  "notes": "Revisión dental general"
}
```

**Parámetros Body:**
| Campo | Tipo | Requerido | Restricción | Ejemplo |
|-------|------|-----------|------------|---------|
| `companyId` | Long | ✅ Sí | Empresa existente | `1` |
| `branchId` | Long | ✅ Sí | Sucursal existente | `1` |
| `contactId` | Long | ✅ Sí | Contacto existente | `1` |
| `doctorId` | Long | ✅ Sí | Doctor asignado | `2` |
| `scheduledStart` | DateTime | ✅ Sí | Fecha futura | `2026-05-15T10:00:00` |
| `scheduledEnd` | DateTime | ✅ Sí | > scheduledStart | `2026-05-15T10:30:00` |
| `notes` | String | ❌ No | Max 500 chars | `"Revisión..."` |

**Response 201 Created:**
```json
{
  "id": 1,
  "companyId": 1,
  "branchId": 1,
  "contactId": 1,
  "scheduledStart": "2026-05-15T10:00:00",
  "scheduledEnd": "2026-05-15T10:30:00",
  "status": "SCHEDULED",
  "notes": "Revisión dental general",
  "createdAt": "2026-04-27T14:30:00Z",
  "updatedAt": "2026-04-27T14:30:00Z"
}
```

**Errores:**
| Status | Descripción |
|--------|-------------|
| 201 | Cita creada exitosamente |
| 400 | Datos inválidos, fechas inválidas, o conflicto de horario |
| 404 | Empresa, sucursal o contacto no encontrado |

---

### 3. **Reagendar Cita**
Cambia la fecha y hora de una cita existente.

**Endpoint:**
```
PUT /api/v1/appointments/{id}/reschedule
```

**Parámetros Path:**
| Parámetro | Tipo | Requerido | Descripción | Ejemplo |
|-----------|------|-----------|-------------|---------|
| `id` | Long | ✅ Sí | ID de la cita | `1` |

**Body Request:**
```json
{
  "scheduledStart": "2026-05-16T11:00:00",
  "scheduledEnd": "2026-05-16T11:30:00",
  "notes": "Cambio de horario - paciente solicitó mañana"
}
```

**Parámetros Body:**
| Campo | Tipo | Requerido | Restricción | Ejemplo |
|-------|------|-----------|------------|---------|
| `scheduledStart` | DateTime | ✅ Sí | Fecha futura | `2026-05-16T11:00:00` |
| `scheduledEnd` | DateTime | ✅ Sí | > scheduledStart | `2026-05-16T11:30:00` |
| `notes` | String | ❌ No | Max 500 chars | `"Cambio de horario..."` |

**Response 200 OK:**
```json
{
  "id": 1,
  "companyId": 1,
  "branchId": 1,
  "contactId": 1,
  "scheduledStart": "2026-05-16T11:00:00",
  "scheduledEnd": "2026-05-16T11:30:00",
  "status": "SCHEDULED",
  "notes": "Cambio de horario - paciente solicitó mañana",
  "createdAt": "2026-04-27T14:30:00Z",
  "updatedAt": "2026-04-27T15:45:00Z"
}
```

**Errores:**
| Status | Descripción |
|--------|-------------|
| 200 | Cita reagendada exitosamente |
| 400 | No se puede reagendar (cancelada, completada), o conflicto de horario |
| 404 | Cita no encontrada |

---

### 4. **Cancelar Cita**
Cancela una cita médica existente.

**Endpoint:**
```
DELETE /api/v1/appointments/{id}/cancel
```

**Parámetros Path:**
| Parámetro | Tipo | Requerido | Descripción | Ejemplo |
|-----------|------|-----------|-------------|---------|
| `id` | Long | ✅ Sí | ID de la cita | `1` |

**Body Request:**
```json
{
  "reason": "Paciente no pudo llegar"
}
```

**Parámetros Body:**
| Campo | Tipo | Requerido | Restricción | Ejemplo |
|-------|------|-----------|------------|---------|
| `reason` | String | ❌ No | Max 500 chars | `"Paciente no pudo llegar"` |

**Response 200 OK:**
```json
{
  "id": 1,
  "companyId": 1,
  "branchId": 1,
  "contactId": 1,
  "scheduledStart": "2026-05-15T10:00:00",
  "scheduledEnd": "2026-05-15T10:30:00",
  "status": "CANCELLED",
  "notes": "Revisión dental general\nCancelación: Paciente no pudo llegar",
  "createdAt": "2026-04-27T14:30:00Z",
  "updatedAt": "2026-04-27T16:00:00Z"
}
```

**Errores:**
| Status | Descripción |
|--------|-------------|
| 200 | Cita cancelada exitosamente |
| 400 | Cita ya está cancelada |
| 404 | Cita no encontrada |

---

### 5. **Obtener Citas por Sucursal**
Retorna todas las citas de una sucursal (ordenadas por fecha ascendente).

**Endpoint:**
```
GET /api/v1/appointments/branch/{branchId}
```

**Parámetros Path:**
| Parámetro | Tipo | Requerido | Descripción | Ejemplo |
|-----------|------|-----------|-------------|---------|
| `branchId` | Long | ✅ Sí | ID de la sucursal | `1` |

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "companyId": 1,
    "branchId": 1,
    "contactId": 1,
    "scheduledStart": "2026-05-15T10:00:00",
    "scheduledEnd": "2026-05-15T10:30:00",
    "status": "SCHEDULED",
    "notes": "Revisión dental",
    "createdAt": "2026-04-27T14:30:00Z",
    "updatedAt": "2026-04-27T14:30:00Z"
  },
  {
    "id": 2,
    "companyId": 1,
    "branchId": 1,
    "contactId": 2,
    "scheduledStart": "2026-05-15T10:30:00",
    "scheduledEnd": "2026-05-15T11:00:00",
    "status": "CONFIRMED",
    "notes": "Limpieza dental",
    "createdAt": "2026-04-27T14:45:00Z",
    "updatedAt": "2026-04-27T14:45:00Z"
  }
]
```

**Errores:**
| Status | Descripción |
|--------|-------------|
| 200 | Citas obtenidas exitosamente |
| 404 | Sucursal no encontrada |

---

### 6. **Obtener Citas por Contacto**
Retorna todas las citas de un contacto (ordenadas por fecha descendente).

**Endpoint:**
```
GET /api/v1/appointments/contact/{contactId}
```

**Parámetros Path:**
| Parámetro | Tipo | Requerido | Descripción | Ejemplo |
|-----------|------|-----------|-------------|---------|
| `contactId` | Long | ✅ Sí | ID del contacto | `1` |

**Response 200 OK:**
```json
[
  {
    "id": 2,
    "companyId": 1,
    "branchId": 1,
    "contactId": 1,
    "scheduledStart": "2026-05-16T15:00:00",
    "scheduledEnd": "2026-05-16T15:30:00",
    "status": "SCHEDULED",
    "notes": "Ortodoncia",
    "createdAt": "2026-04-27T15:00:00Z",
    "updatedAt": "2026-04-27T15:00:00Z"
  },
  {
    "id": 1,
    "companyId": 1,
    "branchId": 1,
    "contactId": 1,
    "scheduledStart": "2026-05-15T10:00:00",
    "scheduledEnd": "2026-05-15T10:30:00",
    "status": "COMPLETED",
    "notes": "Revisión dental",
    "createdAt": "2026-04-27T14:30:00Z",
    "updatedAt": "2026-04-27T14:30:00Z"
  }
]
```

**Errores:**
| Status | Descripción |
|--------|-------------|
| 200 | Citas obtenidas exitosamente |
| 404 | Contacto no encontrado |

---

### 7. **Obtener Cita por ID**
Retorna los detalles de una cita específica.

**Endpoint:**
```
GET /api/v1/appointments/{id}
```

**Parámetros Path:**
| Parámetro | Tipo | Requerido | Descripción | Ejemplo |
|-----------|------|-----------|-------------|---------|
| `id` | Long | ✅ Sí | ID de la cita | `1` |

**Response 200 OK:**
```json
{
  "id": 1,
  "companyId": 1,
  "branchId": 1,
  "contactId": 1,
  "scheduledStart": "2026-05-15T10:00:00",
  "scheduledEnd": "2026-05-15T10:30:00",
  "status": "COMPLETED",
  "notes": "Revisión dental general",
  "createdAt": "2026-04-27T14:30:00Z",
  "updatedAt": "2026-04-27T14:30:00Z"
}
```

**Errores:**
| Status | Descripción |
|--------|-------------|
| 200 | Cita obtenida exitosamente |
| 404 | Cita no encontrada |

---

## 📊 Estados de Cita (Enum AppointmentStatus)

| Estado | Descripción |
|--------|-------------|
| `SCHEDULED` | Cita programada (estado inicial) |
| `CONFIRMED` | Cita confirmada por el paciente o dentista |
| `CANCELLED` | Cita cancelada |
| `COMPLETED` | Cita completada |
| `RESCHEDULED` | Cita reprogramada (reservado para futuro) |

---

## ⏰ Formatos de Fecha y Hora

**DateTime Format:** `yyyy-MM-dd'T'HH:mm:ss`
```
2026-05-15T10:30:00
```

**Date Format:** `yyyy-MM-dd`
```
2026-05-15
```

**Instant Format:** `yyyy-MM-dd'T'HH:mm:ss'Z'` (ISO 8601)
```
2026-04-27T14:30:00Z
```

---

## 🔐 Seguridad

- Todos los endpoints requieren **Spring Security**
- La empresa debe pertenece al usuario autenticado
- El contacto debe pertenecer a la empresa

---

## 📱 Ejemplos cURL

### Obtener slots disponibles:
```bash
curl -X GET "http://localhost:8080/api/v1/appointments/available-slots?branchId=1&doctorId=2&date=2026-05-15"
```

### Crear cita:
```bash
curl -X POST "http://localhost:8080/api/v1/appointments" \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": 1,
    "branchId": 1,
    "contactId": 1,
    "doctorId": 2,
    "scheduledStart": "2026-05-15T10:00:00",
    "scheduledEnd": "2026-05-15T10:30:00",
    "notes": "Revisión dental"
  }'
```

### Reagendar cita:
```bash
curl -X PUT "http://localhost:8080/api/v1/appointments/1/reschedule" \
  -H "Content-Type: application/json" \
  -d '{
    "scheduledStart": "2026-05-16T11:00:00",
    "scheduledEnd": "2026-05-16T11:30:00",
    "notes": "Cambio de horario"
  }'
```

### Cancelar cita:
```bash
curl -X DELETE "http://localhost:8080/api/v1/appointments/1/cancel" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Paciente no pudo llegar"
  }'
```

### Obtener citas por sucursal:
```bash
curl -X GET "http://localhost:8080/api/v1/appointments/branch/1"
```

### Obtener citas por contacto:
```bash
curl -X GET "http://localhost:8080/api/v1/appointments/contact/1"
```

### Obtener cita por ID:
```bash
curl -X GET "http://localhost:8080/api/v1/appointments/1"
```

---

## ✅ Validaciones

- `scheduledStart` y `scheduledEnd` son **obligatorios**
- `scheduledEnd` debe ser **mayor** que `scheduledStart`
- Las fechas deben ser en el **futuro** (@Future)
- La cita debe estar dentro del horario de la sucursal
- No puede haber conflicto con otras citas (SCHEDULED o CONFIRMED)
- No puede haber bloques de sucursal en ese horario
- La sucursal debe pertenecer a la empresa
- El contacto debe pertenecer a la empresa

---

**API Version:** 1.0  
**Last Updated:** 2026-04-27


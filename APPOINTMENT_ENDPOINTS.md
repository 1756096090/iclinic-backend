# 📅 Endpoints de Citas - iClinic Backend

## Información General

- **Base URL**: `http://localhost:8080/api/v1/appointments`
- **Content-Type**: `application/json`
- **Documentación Swagger**: `http://localhost:8080/swagger-ui.html`

---

## 📋 Endpoints Disponibles

### 1. Obtener Slots Disponibles

**Endpoint**: `GET /available-slots`

**Descripción**: Retorna los horarios disponibles para una sucursal en una fecha específica.

**Parámetros Query**:
- `branchId` (Long, required): ID de la sucursal
- `date` (String, required): Fecha en formato `yyyy-MM-dd`

**Ejemplo de Request**:
```
GET /api/v1/appointments/available-slots?branchId=1&date=2024-12-15
```

**Response 200 OK**:
```json
[
  {
    "start": "2024-12-15T09:00:00",
    "end": "2024-12-15T09:30:00"
  },
  {
    "start": "2024-12-15T09:30:00",
    "end": "2024-12-15T10:00:00"
  }
]
```

**Response Codes**:
- `200` - Slots obtenidos exitosamente
- `404` - Sucursal no encontrada

---

### 2. Crear Nueva Cita

**Endpoint**: `POST /`

**Descripción**: Crea una nueva cita médica con validaciones de disponibilidad, pertenencia a empresa y sin conflictos de horario.

**Request Body**:
```json
{
  "companyId": 1,
  "branchId": 1,
  "contactId": 5,
  "scheduledStart": "2024-12-15T09:00:00",
  "scheduledEnd": "2024-12-15T09:30:00",
  "notes": "Revisión general"
}
```

**Validaciones**:
- `companyId` (Long, required): ID de la empresa
- `branchId` (Long, required): ID de la sucursal (debe pertenecer a la empresa)
- `contactId` (Long, required): ID del contacto CRM (debe pertenecer a la empresa)
- `scheduledStart` (DateTime, required): Fecha/hora de inicio (debe ser en el futuro)
- `scheduledEnd` (DateTime, required): Fecha/hora de fin (debe ser mayor al inicio)
- `notes` (String, optional): Notas adicionales

**Response 201 Created**:
```json
{
  "id": 1,
  "companyId": 1,
  "branchId": 1,
  "contactId": 5,
  "scheduledStart": "2024-12-15T09:00:00",
  "scheduledEnd": "2024-12-15T09:30:00",
  "status": "SCHEDULED",
  "notes": "Revisión general",
  "createdAt": "2024-12-01T10:15:30Z",
  "updatedAt": "2024-12-01T10:15:30Z"
}
```

**Response Codes**:
- `201` - Cita creada exitosamente
- `400` - Datos inválidos, conflicto de horario, horario fuera de configuración o bloqueo activo
- `404` - Empresa, sucursal o contacto no encontrado

---

### 3. Reagendar Cita

**Endpoint**: `PUT /{id}/reschedule`

**Descripción**: Cambia la fecha y hora de una cita existente.

**Path Parameters**:
- `id` (Long, required): ID de la cita a reagendar

**Request Body**:
```json
{
  "scheduledStart": "2024-12-16T10:00:00",
  "scheduledEnd": "2024-12-16T10:30:00",
  "notes": "Cita reagendada - cambio de horario"
}
```

**Validaciones**:
- `scheduledStart` (DateTime, required): Nueva fecha/hora de inicio
- `scheduledEnd` (DateTime, required): Nueva fecha/hora de fin
- `notes` (String, optional): Notas adicionales
- La cita no debe estar cancelada o completada
- Las mismas validaciones que al crear cita

**Response 200 OK**:
```json
{
  "id": 1,
  "companyId": 1,
  "branchId": 1,
  "contactId": 5,
  "scheduledStart": "2024-12-16T10:00:00",
  "scheduledEnd": "2024-12-16T10:30:00",
  "status": "SCHEDULED",
  "notes": "Cita reagendada - cambio de horario",
  "createdAt": "2024-12-01T10:15:30Z",
  "updatedAt": "2024-12-01T11:20:15Z"
}
```

**Response Codes**:
- `200` - Cita reagendada exitosamente
- `400` - Datos inválidos, cita cancelada/completada o conflicto de horario
- `404` - Cita no encontrada

---

### 4. Cancelar Cita

**Endpoint**: `DELETE /{id}/cancel`

**Descripción**: Cancela una cita médica existente con motivo opcional.

**Path Parameters**:
- `id` (Long, required): ID de la cita a cancelar

**Request Body**:
```json
{
  "reason": "Paciente solicitó cambio"
}
```

**Validaciones**:
- `reason` (String, optional): Motivo de la cancelación
- La cita no debe estar ya cancelada

**Response 200 OK**:
```json
{
  "id": 1,
  "companyId": 1,
  "branchId": 1,
  "contactId": 5,
  "scheduledStart": "2024-12-16T10:00:00",
  "scheduledEnd": "2024-12-16T10:30:00",
  "status": "CANCELLED",
  "notes": "Cita reagendada - cambio de horarioMotivo de cancelación: Paciente solicitó cambio",
  "createdAt": "2024-12-01T10:15:30Z",
  "updatedAt": "2024-12-01T11:25:00Z"
}
```

**Response Codes**:
- `200` - Cita cancelada exitosamente
- `400` - Cita ya está cancelada
- `404` - Cita no encontrada

---

### 5. Obtener Citas por Sucursal

**Endpoint**: `GET /branch/{branchId}`

**Descripción**: Retorna todas las citas de una sucursal ordenadas por fecha ascendente.

**Path Parameters**:
- `branchId` (Long, required): ID de la sucursal

**Ejemplo de Request**:
```
GET /api/v1/appointments/branch/1
```

**Response 200 OK**:
```json
[
  {
    "id": 1,
    "companyId": 1,
    "branchId": 1,
    "contactId": 5,
    "scheduledStart": "2024-12-15T09:00:00",
    "scheduledEnd": "2024-12-15T09:30:00",
    "status": "SCHEDULED",
    "notes": "Revisión general",
    "createdAt": "2024-12-01T10:15:30Z",
    "updatedAt": "2024-12-01T10:15:30Z"
  }
]
```

**Response Codes**:
- `200` - Citas obtenidas exitosamente
- `404` - Sucursal no encontrada

---

### 6. Obtener Citas por Contacto

**Endpoint**: `GET /contact/{contactId}`

**Descripción**: Retorna todas las citas de un contacto ordenadas por fecha descendente.

**Path Parameters**:
- `contactId` (Long, required): ID del contacto

**Ejemplo de Request**:
```
GET /api/v1/appointments/contact/5
```

**Response 200 OK**:
```json
[
  {
    "id": 2,
    "companyId": 1,
    "branchId": 1,
    "contactId": 5,
    "scheduledStart": "2024-12-16T14:00:00",
    "scheduledEnd": "2024-12-16T14:30:00",
    "status": "SCHEDULED",
    "notes": "Seguimiento",
    "createdAt": "2024-12-02T09:45:20Z",
    "updatedAt": "2024-12-02T09:45:20Z"
  },
  {
    "id": 1,
    "companyId": 1,
    "branchId": 1,
    "contactId": 5,
    "scheduledStart": "2024-12-15T09:00:00",
    "scheduledEnd": "2024-12-15T09:30:00",
    "status": "SCHEDULED",
    "notes": "Revisión general",
    "createdAt": "2024-12-01T10:15:30Z",
    "updatedAt": "2024-12-01T10:15:30Z"
  }
]
```

**Response Codes**:
- `200` - Citas obtenidas exitosamente
- `404` - Contacto no encontrado

---

### 7. Obtener Cita por ID

**Endpoint**: `GET /{id}`

**Descripción**: Retorna los detalles de una cita específica.

**Path Parameters**:
- `id` (Long, required): ID de la cita

**Ejemplo de Request**:
```
GET /api/v1/appointments/1
```

**Response 200 OK**:
```json
{
  "id": 1,
  "companyId": 1,
  "branchId": 1,
  "contactId": 5,
  "scheduledStart": "2024-12-15T09:00:00",
  "scheduledEnd": "2024-12-15T09:30:00",
  "status": "SCHEDULED",
  "notes": "Revisión general",
  "createdAt": "2024-12-01T10:15:30Z",
  "updatedAt": "2024-12-01T10:15:30Z"
}
```

**Response Codes**:
- `200` - Cita obtenida exitosamente
- `404` - Cita no encontrada

---

## 📊 Modelos de Datos

### AppointmentResponseDto

```json
{
  "id": "Long",
  "companyId": "Long",
  "branchId": "Long",
  "contactId": "Long",
  "scheduledStart": "LocalDateTime (ISO 8601)",
  "scheduledEnd": "LocalDateTime (ISO 8601)",
  "status": "Enum: SCHEDULED, CONFIRMED, RESCHEDULED, COMPLETED, CANCELLED",
  "notes": "String (nullable)",
  "createdAt": "Instant (ISO 8601)",
  "updatedAt": "Instant (ISO 8601)"
}
```

### AvailableSlotDto

```json
{
  "start": "LocalDateTime (ISO 8601)",
  "end": "LocalDateTime (ISO 8601)"
}
```

### CreateAppointmentRequestDto

```json
{
  "companyId": "Long (required)",
  "branchId": "Long (required)",
  "contactId": "Long (required)",
  "scheduledStart": "LocalDateTime (required, must be future)",
  "scheduledEnd": "LocalDateTime (required, must be future and after start)",
  "notes": "String (optional)"
}
```

### RescheduleAppointmentRequestDto

```json
{
  "scheduledStart": "LocalDateTime (required, must be future)",
  "scheduledEnd": "LocalDateTime (required, must be future and after start)",
  "notes": "String (optional)"
}
```

### CancelAppointmentRequestDto

```json
{
  "reason": "String (optional)"
}
```

---

## 🔍 Códigos de Error

| Código | Descripción |
|--------|-------------|
| `200` | OK - Operación exitosa |
| `201` | Created - Recurso creado exitosamente |
| `400` | Bad Request - Datos inválidos o validación fallida |
| `404` | Not Found - Recurso no encontrado |
| `500` | Internal Server Error - Error del servidor |

---

## 📝 Ejemplos Completos con cURL

### Crear Cita
```bash
curl -X POST http://localhost:8080/api/v1/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": 1,
    "branchId": 1,
    "contactId": 5,
    "scheduledStart": "2024-12-15T09:00:00",
    "scheduledEnd": "2024-12-15T09:30:00",
    "notes": "Revisión general"
  }'
```

### Obtener Slots Disponibles
```bash
curl -X GET "http://localhost:8080/api/v1/appointments/available-slots?branchId=1&date=2024-12-15"
```

### Reagendar Cita
```bash
curl -X PUT http://localhost:8080/api/v1/appointments/1/reschedule \
  -H "Content-Type: application/json" \
  -d '{
    "scheduledStart": "2024-12-16T10:00:00",
    "scheduledEnd": "2024-12-16T10:30:00",
    "notes": "Nuevo horario"
  }'
```

### Cancelar Cita
```bash
curl -X DELETE http://localhost:8080/api/v1/appointments/1/cancel \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Paciente solicitó cambio"
  }'
```

---

## ⚠️ Validaciones Importantes

1. **Sucursal debe pertenecer a la empresa**
2. **Contacto debe pertenecer a la empresa**
3. **Fecha/hora de fin debe ser mayor al inicio**
4. **Las citas deben estar dentro del horario configurado de la sucursal**
5. **No puede haber conflicto con otras citas (mismo horario)**
6. **No puede haber cita durante bloqueos configurados**
7. **El horario debe respetar la duración del slot configurado**

---

## 📞 Configuración Requerida

Para que el sistema funcione correctamente, se requiere:

1. **Empresa creada** en la base de datos
2. **Sucursal creada** y asignada a la empresa
3. **BranchSchedule configurado** (horarios de trabajo)
4. **Contacto CRM creado** y asignado a la empresa
5. **Slots configurados** (duración en minutos)

---

## 🔗 Rutas Relacionadas

- Empresas: `/api/v1/companies`
- Sucursales: `/api/v1/branches`
- Contactos CRM: `/api/v1/crm/contacts`
- Canales: `/api/v1/crm/channels`

---

**Última actualización**: Diciembre 2024
**Versión API**: v1
**Estado**: ✅ Funcional


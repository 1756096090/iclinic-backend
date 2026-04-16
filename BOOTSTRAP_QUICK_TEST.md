# 🚀 Bootstrap Data - Empresa & Sucursal de Prueba

**Los datos ya están precargados automáticamente cuando haces `./gradlew bootRun`**

---

## ✅ Datos Disponibles al Iniciar

### **Empresa 1 - Ecuador**
```
ID: 1
Nombre: Clínica Dental Premium Ecuador
Tipo: ECUADORIAN
RUC: 1718888888992
```

### **Empresa 2 - Colombia**
```
ID: 2
Nombre: Clínica Dental Bogotá
Tipo: COLOMBIAN
NIT: 9009034567
```

### **Sucursales de Empresa 1 (Ecuador)**
```
ID: 1 - Sucursal Centro Quito
Dirección: Av. Patria N31-169, Quito

ID: 2 - Sucursal Mariscal Quito
Dirección: Mariscal Sucre y 6 de Diciembre, Quito
```

### **Sucursal de Empresa 2 (Colombia)**
```
ID: 3 - Sucursal Centro Bogotá
Dirección: Carrera 7 #32-16, Bogotá
```

---

## 🔍 Acceder a los Datos (Ejemplos cURL)

### **1. Listar todas las Empresas**
```bash
curl -X GET "http://localhost:8080/api/v1/companies" \
  -H "Content-Type: application/json"
```

**Respuesta esperada:**
```json
[
  {
    "id": 1,
    "name": "Clínica Dental Premium Ecuador",
    "companyType": "ECUADORIAN",
    "taxId": "1718888888992"
  },
  {
    "id": 2,
    "name": "Clínica Dental Bogotá",
    "companyType": "COLOMBIAN",
    "taxId": "9009034567"
  }
]
```

### **2. Obtener Empresa por ID**
```bash
curl -X GET "http://localhost:8080/api/v1/companies/1" \
  -H "Content-Type: application/json"
```

### **3. Listar todas las Sucursales**
```bash
curl -X GET "http://localhost:8080/api/v1/branches" \
  -H "Content-Type: application/json"
```

### **4. Obtener Sucursales por Empresa**
```bash
curl -X GET "http://localhost:8080/api/v1/branches/company/1" \
  -H "Content-Type: application/json"
```

**Respuesta esperada:**
```json
[
  {
    "id": 1,
    "name": "Sucursal Centro Quito",
    "address": "Av. Patria N31-169, Quito",
    "branchType": "DENTAL_CLINIC",
    "companyId": 1
  },
  {
    "id": 2,
    "name": "Sucursal Mariscal Quito",
    "address": "Mariscal Sucre y 6 de Diciembre, Quito",
    "branchType": "DENTAL_CLINIC",
    "companyId": 1
  }
]
```

### **5. Obtener Sucursal por ID**
```bash
curl -X GET "http://localhost:8080/api/v1/branches/1" \
  -H "Content-Type: application/json"
```

---

## ➕ Crear Nueva Empresa (POST)

Si quieres crear una empresa adicional de prueba:

```bash
curl -X POST "http://localhost:8080/api/v1/companies" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mi Clínica Dental",
    "taxId": "1234567890123",
    "companyType": "ECUADORIAN"
  }'
```

**Respuesta esperada:**
```json
{
  "id": 3,
  "name": "Mi Clínica Dental",
  "companyType": "ECUADORIAN",
  "taxId": "1234567890123"
}
```

---

## ➕ Crear Nueva Sucursal (POST)

Para la empresa 1, crear sucursal:

```bash
curl -X POST "http://localhost:8080/api/v1/branches/1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sucursal Nueva El Recreo",
    "address": "Centro Comercial El Recreo, Quito",
    "branchType": "DENTAL_CLINIC"
  }'
```

**Respuesta esperada:**
```json
{
  "id": 4,
  "name": "Sucursal Nueva El Recreo",
  "address": "Centro Comercial El Recreo, Quito",
  "branchType": "DENTAL_CLINIC",
  "companyId": 1
}
```

---

## 🎯 Workflow de Prueba Completo

### **Paso 1: Iniciar Backend**
```bash
./gradlew bootRun
```

Verás en logs:
```
2026-03-30 00:00:00.000  INFO ... - Hibernate: create table companies ...
2026-03-30 00:00:00.123  INFO ... - Hibernate: insert into companies values (1, 'Clínica Dental Premium Ecuador', ...)
```

### **Paso 2: Verificar en Swagger**
```
http://localhost:8080/swagger-ui/index.html
```

### **Paso 3: Listar Empresas (GET)**
```bash
curl http://localhost:8080/api/v1/companies
```

### **Paso 4: Listar Sucursales de Empresa 1 (GET)**
```bash
curl http://localhost:8080/api/v1/branches/company/1
```

### **Paso 5: (Opcional) Crear Nueva Sucursal (POST)**
```bash
curl -X POST "http://localhost:8080/api/v1/branches/1" \
  -H "Content-Type: application/json" \
  -d '{"name": "Sucursal Test", "address": "Test St", "branchType": "DENTAL_CLINIC"}'
```

### **Paso 6: Conectar desde Frontend**
```javascript
// Angular/React
fetch('http://localhost:8080/api/v1/companies')
  .then(r => r.json())
  .then(data => console.log(data));

// Resultado esperado
[
  { id: 1, name: "Clínica Dental Premium Ecuador", companyType: "ECUADORIAN", taxId: "1718888888992" },
  { id: 2, name: "Clínica Dental Bogotá", companyType: "COLOMBIAN", taxId: "9009034567" }
]
```

---

## 🗄️ Ver Datos en H2 Console

```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:iclinicdb
Usuario: SA
Password: (vacío)
```

**Queries útiles:**
```sql
SELECT * FROM companies;
SELECT * FROM branches;
SELECT * FROM users;
SELECT * FROM crm_contacts;
SELECT * FROM channel_connections;
```

---

## 📱 Datos de Usuario

También hay **4 usuarios precargados** para probar:

```
ID: 1  - Dr. Juan García (ADMIN) - Ecuador
ID: 2  - Dra. María Rodríguez (DENTIST) - Ecuador
ID: 3  - Carlos Mendoza (RECEPTIONIST) - Ecuador
ID: 4  - Dr. Pedro Martínez (ADMIN) - Colombia
```

**Acceso:**
```bash
curl http://localhost:8080/api/v1/users
curl http://localhost:8080/api/v1/users/1
curl http://localhost:8080/api/v1/users/company/1    # Usuarios de Empresa 1
```

---

## 🔧 Resetear Datos

El proyecto usa `spring.jpa.hibernate.ddl-auto=create-drop` significa:
- **Cada vez que reinicies** → Tablas se crean nuevas
- **import.sql se ejecuta** → Datos precargados se cargan automáticamente
- **No hay necesidad de scripts manuales**

Para forzar reset:
```bash
./gradlew bootRun   # Reinicia = nuevo reset automático
```

---

## ✅ Checklist de Prueba

- [ ] Backend iniciado (`./gradlew bootRun`)
- [ ] Swagger accesible (http://localhost:8080/swagger-ui/)
- [ ] H2 console accesible (http://localhost:8080/h2-console)
- [ ] GET /api/v1/companies devuelve 2 empresas
- [ ] GET /api/v1/branches/company/1 devuelve 2 sucursales
- [ ] GET /api/v1/users devuelve 4 usuarios
- [ ] Frontend conecta sin CORS ERROR ✅
- [ ] El dado precargado de import.sql está visible en BD

---

**¡Listo! Ahora tienes empresa y sucursal de prueba disponibles automáticamente.** 🎉

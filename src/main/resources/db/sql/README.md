# SQL Test Data Scripts

This folder contains SQL scripts for loading test data into the iClinic database. All scripts follow the alphabetical/numeric ordering to ensure proper foreign key dependencies.

## File Structure

```
db/sql/
├── 01-companies.sql              → 2 Companies (Ecuador, Colombia)
├── 02-branches.sql               → 3 Branches (Quito, Bogotá)
├── 03-users.sql                  → 4 Staff Users (Admin, Dentist, Receptionist)
├── 04-channel_connections.sql    → 2 WhatsApp channel connections (Meta)
├── 05-crm_conversations.sql      → 4 Conversations (linked to contacts & channels)
├── 06-crm_messages.sql           → 5 Messages (linked to conversations)
├── 07-crm_contacts.sql           → 4 CRM Contacts
└── README.md                     → This file
```

## Execution Order & Dependencies

Scripts are executed in **numeric order** (01 → 07). Each depends on data from previous files:

| Order | File | Records | Dependencies |
|-------|------|---------|--------------|
| 1 | `01-companies.sql` | 2 | None (base entity) |
| 2 | `02-branches.sql` | 3 | Requires companies |
| 3 | `03-users.sql` | 4 | Requires companies, branches |
| 4 | `04-channel_connections.sql` | 2 | Requires companies, branches |
| 5 | `05-crm_conversations.sql` | 4 | Requires contacts, channels, users |
| 6 | `06-crm_messages.sql` | 5 | Requires conversations |
| 7 | `07-crm_contacts.sql` | 4 | Requires companies, branches |

## Test Data Structure

### Companies
- **Clínica Dental Premium Ecuador** (ID: 1)
  - RUC: 1718888888992
  - Type: ECUADORIAN
  
- **Clínica Dental Bogotá** (ID: 2)
  - NIT: 9009034567
  - Type: COLOMBIAN

### Branches
- **Sucursal Centro Quito** (Branch 1) → Company 1
- **Sucursal Mariscal Quito** (Branch 2) → Company 1
- **Sucursal Centro Bogotá** (Branch 3) → Company 2

### Staff Users
- **Dr. Juan García López** (ID: 1) → Admin, Branch 1
- **Dra. María Rodríguez Pérez** (ID: 2) → Dentist, Branch 1
- **Carlos Mendoza Silva** (ID: 3) → Receptionist, Branch 1
- **Dr. Pedro Martínez López** (ID: 4) → Admin, Branch 3

### Channel Connections
- WhatsApp connection for Company 1 (Ecuador)
- WhatsApp connection for Company 2 (Colombia)

### CRM Integration
- 4 Contacts from Ecuador and Colombia
- 4 Active conversations with contacts
- 5 Messages (mix of inbound/outbound)

## Key Features

✅ Multi-company & multi-branch support
✅ SINGLE_TABLE inheritance for Companies and Users (proper discriminator columns)
✅ Proper foreign key relationships
✅ H2 sequence resets for continuous ID generation
✅ Timestamps with CURRENT_TIMESTAMP
✅ Bilingual data (Spanish names and content)
✅ Real document types (CEDULA_EC, CEDULA_CO)
✅ WhatsApp/Meta integration data

## How It Works

Spring Boot automatically loads these scripts on application startup when:
- The schema is created by JPA (`spring.jpa.hibernate.ddl-auto=create-drop`)
- AND `spring.sql.init.mode=always` is enabled
- The scripts are placed in `classpath:db/sql/*.sql`

## Column Reference

### companies table
- `id` (PK)
- `name` (VARCHAR)
- `company_type` (ENUM: ECUADORIAN, COLOMBIAN)
- `type` (VARCHAR) - Discriminator column
- `ruc` (VARCHAR, nullable) - Ecuador tax ID
- `nit` (VARCHAR, nullable) - Colombia tax ID

### branches table
- `id` (PK)
- `name` (VARCHAR)
- `address` (VARCHAR)
- `branch_type` (ENUM: CLINIC, HOSPITAL)
- `type` (VARCHAR) - Discriminator column
- `company_id` (FK)
- `has_laboratory` (BOOLEAN, nullable) - Discriminator data for specific branch types

### users table
- `id` (PK)
- `first_name` (VARCHAR)
- `last_name` (VARCHAR)
- `email` (VARCHAR, UNIQUE)
- `password` (VARCHAR)
- `phone` (VARCHAR, UNIQUE, nullable)
- `role` (ENUM: ADMIN, DENTIST, RECEPTIONIST, etc.)
- `document_type` (ENUM: CEDULA_EC, CEDULA_CO, etc.)
- `active` (BOOLEAN)
- `user_type` (VARCHAR) - Discriminator column
- `company_id` (FK)
- `branch_id` (FK)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

### crm_channel_connections table
- `id` (PK)
- `company_id` (FK)
- `branch_id` (FK, nullable)
- `channel_type` (ENUM: WHATSAPP, TELEGRAM, etc.)
- `provider` (ENUM: META, TWILIO, etc.)
- `external_account_id` (VARCHAR)
- `external_phone_number_id` (VARCHAR, nullable)
- `access_token_encrypted` (TEXT)
- `webhook_verify_token` (VARCHAR)
- `status` (ENUM: VERIFIED, PENDING, DISCONNECTED)
- `created_at` (TIMESTAMP)

### crm_contacts table
- `id` (PK)
- `company_id` (FK) - UNIQUE with phone
- `branch_id` (FK, nullable)
- `full_name` (VARCHAR)
- `phone` (VARCHAR) - UNIQUE with company_id
- `email` (VARCHAR, nullable)
- `source_channel` (ENUM: WHATSAPP, TELEGRAM, FACEBOOK, EMAIL)
- `active` (BOOLEAN)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

### crm_conversations table
- `id` (PK)
- `contact_id` (FK)
- `channel_connection_id` (FK)
- `assigned_user_id` (FK, nullable)
- `status` (ENUM: OPEN, CLOSED, ARCHIVED)
- `last_message_at` (TIMESTAMP)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

### crm_messages table
- `id` (PK)
- `conversation_id` (FK)
- `direction` (ENUM: INBOUND, OUTBOUND)
- `channel_type` (ENUM: WHATSAPP, TELEGRAM, EMAIL)
- `message_type` (ENUM: TEXT, IMAGE, VIDEO, DOCUMENT)
- `status` (ENUM: RECEIVED, DELIVERED, READ, FAILED)
- `external_message_id` (VARCHAR, nullable)
- `content` (TEXT, nullable)
- `media_url` (TEXT, nullable)
- `sent_by_user_id` (FK, nullable)
- `created_at` (TIMESTAMP)

## ChannelType Enum Values

Supported in crm_contacts.source_channel:
- `WHATSAPP`
- `TELEGRAM`
- `FACEBOOK`
- `EMAIL`
- `SMS`

## Adding New SQL Files

To add more test data:

1. Create file with next number: `08-new-entity.sql`
2. Ensure dependencies exist in prior files
3. Follow this template:

```sql
-- =====================================================
-- N. ENTITY NAME
-- =====================================================
INSERT INTO table_name (id, field1, field2, field3, fk_id, created_at) 
VALUES (1, 'value1', 'value2', 'value3', 1, CURRENT_TIMESTAMP);

-- Reset H2 sequences
ALTER SEQUENCE table_seq RESTART WITH 2;
```

4. Document dependencies in this README
5. Test with H2 database locally before committing

File will be automatically executed on next startup! ✅

## Testing Data Load

After startup, verify data was loaded:

```bash
# Check companies
curl http://localhost:8080/api/v1/companies

# Check branches
curl http://localhost:8080/api/v1/branches

# Check users
curl http://localhost:8080/api/v1/users

# Or use H2 Console
http://localhost:8080/h2-console
SELECT * FROM companies;
SELECT * FROM branches;
SELECT * FROM users;
```

## Notes

- Scripts run **every startup** (due to `create-drop` resets database)
- Use **CURRENT_TIMESTAMP** for auto timestamps
- Set **IDs explicitly** to ensure consistency across restarts
- Use **NULL** for nullable fields
- Maintain **referential integrity** (order matters: companies → branches → users)
- Script execution order **must match dependency order**

---

**Last Updated:** March 30, 2026

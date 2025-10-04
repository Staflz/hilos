# 🏦 API de Transacciones Bancarias Concurrentes - Documentación Completa

Esta es una API REST desarrollada con Spring Boot que demuestra el manejo de concurrencia en transacciones bancarias, implementando patrones de sincronización para evitar condiciones de carrera.

## 📋 Tabla de Contenidos

1. [Características Principales](#características-principales)
2. [Arquitectura del Proyecto](#arquitectura-del-proyecto)
3. [Entidades y Modelos](#entidades-y-modelos)
4. [Endpoints de la API](#endpoints-de-la-api)
5. [Manejo de Concurrencia](#manejo-de-concurrencia)
6. [Configuración y Ejecución](#configuración-y-ejecución)
7. [Guía de Pruebas](#guía-de-pruebas)
8. [Ejemplos de Uso](#ejemplos-de-uso)
9. [Monitoreo y Logs](#monitoreo-y-logs)
10. [Solución de Problemas](#solución-de-problemas)
11. [Tecnologías Utilizadas](#tecnologías-utilizadas)

---

## 🚀 Características Principales

- **Arquitectura MVC**: Separación clara entre Modelo, Vista y Controlador
- **Concurrencia**: Transferencias procesadas en hilos separados con `@Async`
- **Sincronización**: Uso de bloqueos pesimistas para evitar condiciones de carrera
- **Validación**: Validación de datos de entrada con Bean Validation
- **Base de Datos**: PostgreSQL con JPA/Hibernate
- **Testing**: Pruebas automatizadas y manuales

---

## 🏗️ Arquitectura del Proyecto

### Estructura de Directorios
```
src/main/java/com/plataformas/hilos/
├── HilosApplication.java          ← PUNTO DE ENTRADA
├── entity/                        ← MODELO (Base de Datos)
│   ├── Account.java              ← Tabla "accounts"
│   ├── Transaction.java          ← Tabla "transactions"  
│   └── TransactionStatus.java    ← Enum para estados
├── repository/                     ← CAPA DE DATOS
│   ├── AccountRepository.java    ← Operaciones con Account
│   └── TransactionRepository.java ← Operaciones con Transaction
├── service/                       ← LÓGICA DE NEGOCIO
│   ├── AccountService.java            ← Reglas de negocio para cuentas
│   ├── TransactionService.java        ← Orquestación/asíncrono (@Async)
│   └── TransferExecutorService.java   ← Lógica transaccional de transferencias
├── controller/                    ← CAPA DE PRESENTACIÓN (REST API)
│   ├── AccountController.java     ← Endpoints para cuentas
│   ├── TransactionController.java ← Endpoints para transacciones
│   └── ConcurrencyDemoController.java ← Demo de concurrencia
├── dto/                          ← OBJETOS DE TRANSFERENCIA
│   ├── CreateAccountRequest.java  ← Datos que llegan del cliente
│   ├── AccountResponse.java      ← Datos que se envían al cliente
│   └── ...
├── config/                         ← CONFIGURACIÓN
│   └── AsyncConfig.java            ← Configuración de hilos
└── exception/                      ← MANEJO DE ERRORES
    └── InsufficientFundsException.java ← Error de saldo insuficiente
```

### Flujo de una Petición HTTP
```
Cliente HTTP → Controller → Service → Repository → Base de Datos
     ↓              ↓         ↓          ↓
   REST API    Lógica de   Acceso a   PostgreSQL
              Negocio     Datos
```

---

## 📊 Entidades y Modelos

### Account (Cuenta)
- `id`: Identificador único
- `owner`: Propietario de la cuenta
- `balance`: Saldo actual
- `createdAt`: Fecha de creación

### Transaction (Transacción)
- `id`: Identificador único
- `fromAccount`: Cuenta origen
- `toAccount`: Cuenta destino
- `amount`: Monto de la transferencia
- `status`: Estado (PENDING, COMPLETED, FAILED)
- `createdAt`: Fecha de creación
- `updatedAt`: Fecha de última actualización

### Estados de Transacción
- **PENDING**: Transacción iniciada, en proceso
- **COMPLETED**: Transacción completada exitosamente
- **FAILED**: Transacción fallida (saldo insuficiente, error, etc.)

---

## 🌐 Endpoints de la API

### Cuentas

#### POST /accounts
Crear una nueva cuenta con saldo inicial.

**Request Body:**
```json
{
  "owner": "Juan Pérez",
  "initialBalance": 1000.00
}
```

**Response:**
```json
{
  "success": true,
  "message": "Cuenta creada exitosamente",
  "data": {
    "id": 1,
    "owner": "Juan Pérez",
    "balance": 1000.00,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

#### GET /accounts/{id}
Consultar saldo de una cuenta específica.

#### GET /accounts
Listar todas las cuentas.

### Transacciones

#### POST /transactions/transfer
Realizar transferencia entre dos cuentas (procesamiento asíncrono).

**Request Body:**
```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 100.00
}
```

#### POST /transactions/transfer-sync
Realizar transferencia entre dos cuentas (procesamiento síncrono).

#### GET /transactions
Listar todas las transacciones realizadas.

#### GET /transactions/status/{status}
Listar transacciones por estado (PENDING, COMPLETED, FAILED).

### Demostración de Concurrencia

#### POST /demo/concurrent-transfers
Demostrar el problema de concurrencia con múltiples transferencias simultáneas.

**Parámetros:**
- `fromAccountId`: ID de la cuenta origen
- `toAccountId`: ID de la cuenta destino
- `amount`: Monto a transferir
- `numberOfTransfers`: Número de transferencias simultáneas (default: 5)

#### GET /demo/race-condition
Información sobre condiciones de carrera.

---

## ⚡ Manejo de Concurrencia

### Sincronización Implementada

1. **Bloqueos Pesimistas**: Uso de `@Lock(LockModeType.PESSIMISTIC_WRITE)` en el repositorio
2. **Transacciones**: `TransferExecutorService.executeTransfer` con `@Transactional(noRollbackFor=InsufficientFundsException.class)` para persistir FAILED sin rollback
3. **Procesamiento Asíncrono**: `TransactionService.processTransfer` con `@Async("transferExecutor")` que delega en el ejecutor transaccional

### Ejemplo de Problema de Concurrencia

**Sin sincronización, dos hilos podrían:**
1. Leer el mismo saldo (ej: $1000)
2. Restar el mismo monto (ej: $500)
3. Guardar el saldo final ($500)
4. **Resultado**: Saldo incorrecto si ambos hilos operan simultáneamente

**Con sincronización:**
1. Un hilo bloquea la cuenta
2. Realiza la operación completa
3. Libera el bloqueo
4. El siguiente hilo puede proceder
5. **Resultado**: Consistencia garantizada

### Configuración de Hilos (AsyncConfig)

Archivo: `src/main/java/com/plataformas/hilos/config/AsyncConfig.java`

Reglas prácticas de ajuste según concurrencia esperada:

- **corePoolSize**: ~2 a 4 × núcleos CPU (p.ej. 16-64)
- **maxPoolSize**: 4× a 8× de core para bursts (p.ej. 128-512)
- **queueCapacity**: mayor o igual al pico de solicitudes si quieres absorber picos; o menor si prefieres backpressure (con `CallerRunsPolicy`)

Ejemplos:

- Pruebas pequeñas (≈1k solicitudes): core=16, max=128, queue=2000
- Pruebas medianas (≈5k): core=32, max=256, queue=10000
- Pruebas grandes (≈10k): core=32-64, max=256-512, queue=20000

Nota: Ajusta el pool de conexiones de base de datos (Hikari) en `application.properties` si subes la concurrencia (30-100 conexiones suele ser razonable). El resto debe esperar en cola.

---

## ⚙️ Configuración y Ejecución

### Configuración de Base de Datos

La aplicación está configurada para usar PostgreSQL. Asegúrate de tener:

1. PostgreSQL ejecutándose en `localhost:5432`
2. Base de datos `hilos` creada
3. Usuario `postgres` con contraseña configurada

### Esquema de Base de Datos

```sql
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    owner VARCHAR(100) NOT NULL,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    from_account_id BIGINT REFERENCES accounts(id) ON DELETE CASCADE,
    to_account_id BIGINT REFERENCES accounts(id) ON DELETE CASCADE,
    amount NUMERIC(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Ejecución

1. **Iniciar la aplicación:**
   ```bash
   ./gradlew bootRun
   ```

2. **Verificar que esté funcionando:**
   ```bash
   curl http://localhost:8080/accounts
   ```

3. **La API estará disponible en:** `http://localhost:8080`

---

## 🧪 Guía de Pruebas

### Método 1: Script Automatizado (Recomendado)

```powershell
.\test-simple.ps1
```

#### Parámetros del script y cómo cambiarlos

- Archivo: `test-simple.ps1`
- Variables clave:
  - `$numRequests`: número de transferencias concurrentes (por defecto 10000)
  - `$amountPerTx`: monto por transacción (por defecto 1.00)
- El script espera activamente hasta que `COMPLETED + FAILED == $numRequests` o hasta timeout configurado (60s).

Para probar con otros valores, edita estas variables al inicio del bloque “Preparando prueba de concurrencia”.

### Método 2: Comandos Manuales

#### 1. Verificar que la Aplicación Funcione
```bash
curl http://localhost:8080/accounts
```

**Respuesta esperada:**
```json
{
  "success": true,
  "message": "Operación exitosa",
  "data": [
    {
      "id": 1,
      "owner": "Alice",
      "balance": 1000.00,
      "createdAt": "2025-09-28T15:11:16.006061"
    },
    {
      "id": 2,
      "owner": "Bob",
      "balance": 500.00,
      "createdAt": "2025-09-28T15:11:16.006061"
    }
  ]
}
```

#### 2. Crear una Nueva Cuenta
```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "owner": "Juan Pérez",
    "initialBalance": 2000.00
  }'
```

#### 3. Realizar Transferencia Síncrona
```bash
curl -X POST http://localhost:8080/transactions/transfer-sync \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 100.00
  }'
```

#### 4. Realizar Transferencia Asíncrona
```bash
curl -X POST http://localhost:8080/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 50.00
  }'
```

#### 5. Probar Concurrencia
```bash
curl -X POST "http://localhost:8080/demo/concurrent-transfers?fromAccountId=1&toAccountId=2&amount=10.00&numberOfTransfers=5"
```

#### 6. Consultar Transacciones
```bash
curl -X GET http://localhost:8080/transactions
```

### Método 3: PowerShell (Windows)

```powershell
# Consultar cuentas
Invoke-RestMethod -Uri "http://localhost:8080/accounts" -Method GET

# Crear cuenta
Invoke-RestMethod -Uri "http://localhost:8080/accounts" -Method POST -ContentType "application/json" -Body '{"owner":"Test","initialBalance":1000.00}'

# Transferencia
Invoke-RestMethod -Uri "http://localhost:8080/transactions/transfer-sync" -Method POST -ContentType "application/json" -Body '{"fromAccountId":1,"toAccountId":2,"amount":100.00}'
```

---

## 💡 Ejemplos de Uso

### Casos de Prueba para Demostrar Concurrencia

#### Caso 1: Transferencias Normales
1. Crear dos cuentas con saldo inicial
2. Realizar transferencias individuales
3. Verificar que los saldos sean correctos

#### Caso 2: Transferencias Concurrentes
1. Crear cuenta origen con saldo de $1000
2. Crear cuenta destino con saldo de $0
3. Ejecutar 20 transferencias simultáneas de $10 cada una
4. Verificar que:
   - No se produzcan saldos negativos
   - El saldo total se mantenga constante ($1000)
   - Todas las transacciones se procesen correctamente

#### Caso 3: Condiciones de Carrera
1. Crear cuenta con saldo de $100
2. Ejecutar 15 transferencias simultáneas de $10 cada una
3. Sin sincronización: podría resultar en saldo negativo
4. Con sincronización: todas las transferencias se procesan correctamente

### Casos de Prueba Avanzados

#### Probar Saldo Insuficiente
```bash
curl -X POST http://localhost:8080/transactions/transfer-sync \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 999999.00
  }'
```

**Respuesta esperada:**
```json
{
  "success": false,
  "message": "Error: Saldo insuficiente en la cuenta origen",
  "data": null
}
```

#### Probar Validaciones
```bash
# Cuenta inexistente
curl -X POST http://localhost:8080/transactions/transfer-sync \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": 999,
    "toAccountId": 2,
    "amount": 100.00
  }'

# Monto negativo
curl -X POST http://localhost:8080/transactions/transfer-sync \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": -100.00
  }'
```
Para postman:

Envía 1 sola petición POST a POST /demo/concurrent-transfers con query params:
fromAccountId, toAccountId, amount, numberOfTransfers=10000
Ese endpoint ya dispara la concurrencia desde el backend, así que Postman no necesita generar 10k clientes paralelos.
Luego verifica con:
GET /transactions/status/COMPLETED
GET /transactions/status/FAILED
GET /accounts/{id} para saldos


---

## 📈 Monitoreo y Logs

### Verificar Logs de Concurrencia

Los logs mostrarán:
- Inicio de transferencias asíncronas
- Bloqueos de cuentas
- Procesamiento de transferencias
- Completado de operaciones

### Ejemplo de Logs

```
2025-09-28 21:18:03 INFO --- [Transfer-1] c.p.hilos.service.TransactionService : Iniciando transferencia asíncrona: 1 -> 2 ($50.00)
2025-09-28 21:18:03 INFO --- [Transfer-2] c.p.hilos.service.TransactionService : Iniciando transferencia asíncrona: 1 -> 2 ($50.00)
2025-09-28 21:18:03 INFO --- [pool-2-thread-1] c.p.hilos.service.TransactionService : Ejecutando transferencia: 1 -> 2 ($50.00)
2025-09-28 21:18:03 INFO --- [pool-2-thread-1] c.p.hilos.service.TransactionService : Transferencia completada exitosamente. Transacción ID: 1
```

### Verificar Bloqueos de Base de Datos

Los logs mostrarán consultas SQL con `FOR UPDATE`:
```sql
SELECT a1_0.id, a1_0.balance, a1_0.created_at, a1_0.owner 
FROM accounts a1_0 
WHERE a1_0.id=? FOR UPDATE
```

### Checklist de Funcionamiento

- [ ] ✅ Aplicación inicia sin errores
- [ ] ✅ Endpoints de cuentas funcionan
- [ ] ✅ Transferencias síncronas funcionan
- [ ] ✅ Transferencias asíncronas funcionan
- [ ] ✅ Concurrencia se maneja correctamente
- [ ] ✅ Validaciones funcionan
- [ ] ✅ Manejo de errores funciona
- [ ] ✅ Logs muestran sincronización

### Métricas de Concurrencia

Después de ejecutar transferencias concurrentes, verifica:
- **No hay saldos negativos**
- **El saldo total se mantiene constante**
- **Todas las transacciones se procesan**
- **Los logs muestran bloqueos correctos**

---

## 🔧 Solución de Problemas

### Si la aplicación no inicia:
1. Verificar que PostgreSQL esté corriendo
2. Verificar credenciales en `application.properties`
3. Verificar que el puerto 8080 esté libre

### Si hay errores de conexión:
1. Verificar que la base de datos `hilos` existe
2. Verificar usuario y contraseña de PostgreSQL
3. Revisar logs de la aplicación

### Si las transferencias fallan:
1. Verificar que las cuentas existen
2. Verificar que hay saldo suficiente
3. Revisar logs de concurrencia

### Comandos Útiles

```bash
# Limpiar y reconstruir
./gradlew clean build

# Ejecutar pruebas
./gradlew test

# Verificar conexión a PostgreSQL
psql -h localhost -U postgres -d hilos
```

---

## 🛠️ Tecnologías Utilizadas

- **Spring Boot 3.5.6**
- **Spring Data JPA**
- **Spring Web**
- **Spring Validation**
- **PostgreSQL**
- **Lombok**
- **Java 21**

### Dependencias Principales

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    runtimeOnly 'org.postgresql:postgresql'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'com.h2database:h2'
}
```

---

## 🎓 Lo Que Has Aprendido

1. **Spring Boot Auto-Configuration**: Cómo Spring Boot configura todo automáticamente
2. **Arquitectura MVC**: Separación clara de responsabilidades
3. **Concurrencia en Java**: Hilos, sincronización, bloqueos
4. **REST APIs**: Endpoints, validación, manejo de errores
5. **Base de Datos**: JPA, transacciones, consistencia
6. **Testing**: Cómo probar APIs de forma sistemática

## 🚀 Próximos Pasos para Aprender Más

1. **Explorar los Logs**: Observa cómo se manejan los hilos concurrentes
2. **Modificar Código**: Prueba cambiar validaciones o lógica de negocio
3. **Agregar Endpoints**: Crea nuevos endpoints para más funcionalidad
4. **Optimizar**: Mejora el rendimiento o agrega más validaciones
5. **Documentar**: Usa Swagger/OpenAPI para documentar la API

---

**🎉 ¡Felicidades! Tu API de Transacciones Bancarias Concurrentes está funcionando perfectamente y has aprendido conceptos importantes de Spring Boot, concurrencia y arquitectura de software.**

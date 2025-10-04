#Presentado por:😎
Camilo Torres ☆*: .｡. o(≧▽≦)o .｡.:*☆
Santiago Villamizar (●'◡'●)

# 🏦 API de Transacciones Bancarias Concurrentes

Esta es una API REST desarrollada con Spring Boot que demuestra el manejo de concurrencia en transacciones bancarias, implementando patrones de sincronización para evitar condiciones de carrera.

## 🚀 Inicio Rápido

### 1. Ejecutar la Aplicación
```bash
./gradlew bootRun
```

### 2. Verificar que Funciona
```bash
curl http://localhost:8080/accounts
```

### 3. Probar la API
```bash
# Crear cuenta
curl -X POST http://localhost:8080/accounts -H "Content-Type: application/json" -d '{"owner":"Test","initialBalance":1000.00}'

# Transferencia síncrona
curl -X POST http://localhost:8080/transactions/transfer-sync -H "Content-Type: application/json" -d '{"fromAccountId":1,"toAccountId":2,"amount":100.00}'

# Probar concurrencia
curl -X POST "http://localhost:8080/demo/concurrent-transfers?fromAccountId=1&toAccountId=2&amount=10.00&numberOfTransfers=3"
```

## 📋 Características Principales

- **Arquitectura MVC**: Separación clara entre Modelo, Vista y Controlador
- **Concurrencia**: Transferencias procesadas en hilos separados con `@Async`
- **Sincronización**: Uso de bloqueos pesimistas para evitar condiciones de carrera
- **Validación**: Validación de datos de entrada con Bean Validation
- **Base de Datos**: PostgreSQL con JPA/Hibernate

## 🌐 Endpoints Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/accounts` | Listar todas las cuentas |
| `POST` | `/accounts` | Crear nueva cuenta |
| `GET` | `/accounts/{id}` | Consultar cuenta específica |
| `POST` | `/transactions/transfer-sync` | Transferencia síncrona |
| `POST` | `/transactions/transfer` | Transferencia asíncrona |
| `GET` | `/transactions` | Listar transacciones |
| `POST` | `/demo/concurrent-transfers` | Demostrar concurrencia |

## ⚡ Manejo de Concurrencia

### Problema Sin Sincronización
- Dos hilos leen el mismo saldo ($1000)
- Ambos restan $500
- Resultado: Saldo incorrecto

### Solución Con Sincronización
- Bloqueos pesimistas (`@Lock`)
- Transacciones (`@Transactional`)
- Procesamiento asíncrono (`@Async`)

## 🧪 Pruebas

### Script Automatizado
```powershell
.\test-simple.ps1
```
```

## 📚 Documentación Completa

Para información detallada sobre:
- Arquitectura del proyecto
- Configuración de base de datos
- Guía completa de pruebas
- Ejemplos de uso
- Monitoreo y logs
- Solución de problemas

**Ver: [DOCUMENTACION_COMPLETA.md](DOCUMENTACION_COMPLETA.md)**

## 🛠️ Tecnologías

- **Spring Boot 3.5.6**
- **Spring Data JPA**
- **PostgreSQL**
- **Java 21**
- **Lombok**

## 🎓 Lo Que Aprenderás

1. **Spring Boot Auto-Configuration**
2. **Arquitectura MVC**
3. **Concurrencia en Java**
4. **REST APIs**
5. **Base de Datos con JPA**
6. **Testing de APIs**

---

**🎉 ¡Tu API de Transacciones Bancarias Concurrentes está lista para usar!**

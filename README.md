# 🌱 SmartFarm Backend — Cloud Farm Management REST API

![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6.x-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)

Enterprise-grade, zero-cost cloud backend API for **SmartFarm**, designed for multi-tier agricultural management (Farm Administrator, Farm Managers, and Field Supervisors).

---

## 📚 Official SDLC Documentation & Architectural Specifications (PDFs)

All official documents are publication-grade PDF files located in the [`/docs`](docs/) directory:

- 📄 **[Software Requirements Specification (SRS) PDF](docs/SmartFarm_SRS_Document.pdf)**
- 🏗️ **[System Design & Architecture (SDD) PDF](docs/SmartFarm_System_Design_and_Architecture.pdf)**
- 📊 **[Complete System Workflow Flowcharts (12 Workflows) PDF](docs/SmartFarm_Complete_System_Flowcharts.pdf)**

---

## 🏛️ Core Architectural Highlights

- **Multi-Tier Hierarchical PBAC:** Admin delegates categories & privileges to Managers; Managers delegate projects & operational privileges to dedicated Supervisors.
- **Financial Privacy Shield:** Field Supervisors only log operational data (harvest, daily activities, stock usage) and are strictly shielded from viewing financial revenue, project budgets, or profit margins.
- **Dynamic Scoped Zero-State Engine:** Automatically tailors dashboard analytics by assigned categories/projects, rendering clean Hero Action Cards when unassigned.
- **Collision-Safe Unique ID Generation:** Algorithmic ID generation preventing primary key collisions across project and transaction deletions.

---

## 🚀 Getting Started

### Prerequisites
- JDK 21 or higher
- Maven 3.9+
- MySQL 8.0+ / TiDB Cloud Serverless instance

### Build & Run
```bash
# Clone the repository
git clone https://github.com/martins-ke/smartfarm-backend.git
cd smartfarm-backend

# Package the application
./mvnw clean package -DskipTests

# Run the Spring Boot application
./mvnw spring-boot:run
```

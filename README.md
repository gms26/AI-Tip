# AI Tip Assistant 🚀

A comprehensive, full-stack application designed to completely reimagine the tipping experience. Built with a modern **React/Vite** frontend and a robust **Spring Boot (Java)** backend, this application leverages **Google Gemini AI** to provide users with context-aware, highly personalized tipping recommendations based on historical data, budget constraints, and real-time service quality evaluations.

---

## 📖 Elaborate Project Review

The AI Tip Assistant is not just a simple tip calculator; it is an intelligent, multi-tenant financial tracking ecosystem. Here is an elaborate review of its capabilities:

### 1. Smart Tip AI & Decision Memory 🧠
The core of the application utilizes Google Gemini AI to generate intelligent tipping recommendations. The system learns from the user over time (Decision Memory), tracking whether users generally tip conservatively, moderately, or generously. It automatically adapts its advice based on past behaviors, budget limits, and specific feedback given by the user on the AI's recommendations.

### 2. Comprehensive Financial Tracking 📊
Users can track every tip they leave. The application calculates:
- **Spend Analytics:** Visualizing tipping behaviors through detailed Recharts graphs, analyzing medians, averages, and historical trends.
- **Budgeting & Goals:** Users can set monthly tipping budgets (e.g., "$100 max tip allowance") or targeted average tip percentages (e.g., "Keep my average tip at 18%").
- **Tip Forecasts & Scenarios:** Predictive modeling helps users estimate their future tipping expenses based on past behavior.

### 3. Receipt OCR Integration 📸
By uploading an image of a receipt, the system parses the total, tax, and itemized costs automatically. It then reconciles these amounts to provide precise, error-free tipping recommendations without manual data entry.

### 4. Advanced Tipping Features 💡
- **Tax Deductions & Tip Pools:** Ability to calculate tips pre-tax or post-tax, and seamlessly split the tip pool among multiple parties.
- **Data Quality & Personalization:** Evaluates how much data the AI has on a user (Tip Data Quality) and allows fine-grained personalization preferences (e.g., "I always over-tip bartenders").
- **Gamification:** Features a robust achievement system that rewards users for consistent, generous, or budget-conscious tipping behaviors.

---

## 🛠️ Technology Stack & Architecture

### Backend (Robust API Layer)
- **Framework:** Spring Boot 3.3.2 (Java 21 support)
- **Database:** PostgreSQL with **Flyway Migrations** ensuring reproducible schemas.
- **Security:** JWT-based Authentication. The system implements strict multi-tenant isolation, ensuring users can only access their own data.
- **Integrations:** Google Gemini AI API via Spring's `RestClient`.
- **Testing:** Comprehensive test suite with JUnit 5 and Mockito (**780+ passing tests**).

### Frontend (Dynamic User Interface)
- **Framework:** React 18 powered by Vite for lightning-fast builds.
- **Styling & UI:** Material UI (MUI) v5 for a polished, responsive, and accessible interface.
- **Charting:** Recharts for dynamic financial data visualization.
- **State Management & Routing:** React Context API and React Router DOM.
- **API Communication:** Axios with centralized URL management and environment-aware configurations.

---

## 🚀 Setup & Deployment Guide

The application is heavily modularized and configured for straightforward production deployment.

### Prerequisites
- Java 17+ and Maven
- Node.js 18+ and npm
- PostgreSQL (running locally or in the cloud)
- A Google Gemini API Key

### Production Configuration (Environment Variables)
The system is built to safely externalize secrets and configurations. Set the following environment variables in your deployment platform (e.g., Render, Heroku, AWS):
- `SPRING_DATASOURCE_URL`: Your production database URL (e.g., `jdbc:postgresql://<host>:5432/<dbname>`). Defaults to localhost for dev.
- `APP_CORS_ALLOWED_ORIGINS`: The domain of your deployed frontend (e.g., `https://my-tip-app.vercel.app`). Defaults to `http://localhost:5173`.
- `VITE_API_BASE_URL`: The domain of your deployed backend. Defaults to `http://localhost:8080`.

### Local Development Setup

**1. Database Initialization**
Create a PostgreSQL database named `tip_calculator`. Flyway will handle the creation of all tables (V1 through V14 migrations) automatically on startup.

**2. Backend Setup**
```bash
cd backend
# Set your Gemini API key in backend/src/main/resources/application.yml or via env variable
mvn clean install
mvn spring-boot:run
```

**3. Frontend Setup**
```bash
cd frontend
npm install
npm run dev
```

---

## ✅ Quality Assurance & Verification
This project is built with an absolute emphasis on stability and security:
- **Test Suite:** Over 780 backend unit and integration tests successfully validate everything from core math logic to AI failure fallbacks.
- **Security:** Extensive Anti-IDOR (Insecure Direct Object Reference) checks guarantee that users are entirely sandboxed within their own data.
- **Frontend Build:** The Vite production build (`npm run build`) is fully optimized and resolves all API paths dynamically.

---
*Developed during the 40-Day Master Roadmap Challenge.*

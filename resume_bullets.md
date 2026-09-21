# AI Tip Assistant — Resume & LinkedIn Highlights

These bullet points are designed to clearly communicate the technical depth, architectural decisions, and production-readiness of your project to recruiters and engineering managers.

## 📄 Resume Bullets (Choose 3-4 for your resume)

* **Architected a full-stack AI financial application** utilizing **Spring Boot 3, React (Vite), and PostgreSQL**, containerized with Docker and Nginx for environment portability.
* **Integrated Google Groq AI** via REST API to deliver real-time, context-aware financial recommendations, leveraging historical user spending metrics and dynamic JSON schema parsing.
* **Engineered robust Multi-Tenant Security** implementing stateless **JWT authentication** and strict Anti-IDOR (Insecure Direct Object Reference) patterns to guarantee complete data isolation across user profiles.
* **Ensured uncompromising system reliability** by authoring a comprehensive **784-test backend suite** (JUnit 5 / Mockito) alongside an automated **Playwright End-to-End** regression suite verifying the frontend user journey.
* **Designed a rigorous, interface-driven external API layer**, seamlessly toggling between real production APIs (Groq, Frankfurter Currency, PostgreSQL) and sandboxed Mock Providers (Stripe Payments, Square POS, Google Places) via environment configurations.

## 💼 LinkedIn Project Description
**AI Tip Assistant — Full-Stack Financial Application**

I built a full-stack financial application that completely reimagines the tipping experience by using AI to generate context-aware recommendations based on personal budgets and historical data.

Instead of building a simple tip calculator, I focused heavily on enterprise-grade architecture, security, and test-driven reliability.

**Key Technical Highlights:**
- **Stack**: Spring Boot (Java), React (Vite), PostgreSQL, Docker, Nginx.
- **AI Integration**: Engineered structured JSON prompt workflows with the Groq API to provide real-time, deterministic financial advice.
- **Security**: Implemented stateless JWT authentication and stringent multi-tenant data isolation (Anti-IDOR).
- **Test-Driven**: Validated core business logic and AI fallbacks with a 784-test backend suite and automated the frontend user journey with Playwright E2E tests (10 passing flows).
- **Architecture**: Leveraged the Strategy Pattern to build a modular external API layer, clearly delineating real integrations (Groq, Currency) from mocked sandboxes (Stripe, Square POS).

*Check out the GitHub repository for the full architectural flow, database ER diagrams, and setup instructions!*

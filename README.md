# 🤖 PairMind – Multi-Agent Support Chat

PairMind is a Spring Boot–based AI customer support system that routes user queries between specialized agents (e.g. billing and technical support) powered by Gemini API.

---

## 🧩 1. Add Your API Key

Edit the following line with your Gemini API key at file:

```
src/main/resources/application.properties
```

---

## ⚙️ 2. Install Dependencies

Make sure you have **Java 17+** and **Maven** installed.

Then run:

```bash
mvn clean install
```

This will download and compile all dependencies defined in `pom.xml`.

---

## 🚀 3. Start the Application

## 🗂️ Notes

* To quickly run the app, simply add your API key and start the backend — everything should work right away.
* To regenerate all embeddings from scratch, delete the `src/main/resources/chunks.json` file and restart the application.
  The system will automatically generate new embeddings and save them to `chunks.json` (this process takes about **3–4 minutes**, or roughly **219 seconds**).

To run the backend:

```bash
mvn spring-boot:run
```

If successful, the server will start at:

```
http://localhost:8080/
```

---


## 💬 4. Open the Frontend

Open your browser and go to:

```
http://localhost:8080/
```

You’ll see a modern chat interface where you can talk to AI agents.

---

## 🧰 Prerequisites

Make sure the following are installed:

* ☕ **Java 17 or later**
* 🧱 **Apache Maven**
* 🌐 Internet connection (for Gemini API access)

---

### 📚 Documentation Base (RAG Knowledge Source)

This project’s **RAG module** is powered by a collection of internal technical and billing documents from the *Company AI Support System*.
These documents describe how the company’s intelligent support platform works — including:

* 🧠 **Architecture overview** — Java 17 + Spring Boot + Maven system integrated with **Gemini API**
* ⚙️ **Technical operations** — installation, configuration, troubleshooting, and API usage
* 💳 **Billing procedures** — payments, refunds, plans, and account management
* 🔐 **External API endpoints** — authentication, user management, and order operations
* 🧾 **Error handling and best practices** — retry logic, API limits, logging, and debugging

The RAG system splits these docs into small **semantic chunks**, generates **embeddings**, and retrieves the most relevant context for each user query.
This enables the **Technical Agent** to answer based only on verified internal documentation — ensuring factual, consistent, and company-aligned responses.

---

### 💬 Example User Questions

Here are some example questions that the RAG system can handle using the internal documentation:

#### Technical Support Questions

* How to uninstall the app?
* How to use T92 in World of Tanks?
* How to send 12,000 requests per minute using the API?
* How to use 9,000,000 tokens per second?

#### Billing & Account Questions

* I want to make a refund
* How much time does it take to process a refund?
* How much does the Pro plan cost?
* What about the Standard plan?


---

**Developed by Jan Jurinok**

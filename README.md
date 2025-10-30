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



---

**Developed by Jan Jurinok**

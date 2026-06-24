# AI Image Generator

A full-stack AI Image Generation platform built with Spring Boot, Ollama, and ComfyUI.

## Features

- Text to Image Generation
- Image to Prompt Generation
- AI Prompt Enhancement
- Local LLM Integration using Ollama
- Image Generation using ComfyUI
- REST APIs with Spring Boot
- Swagger API Documentation
- Environment Variable Support
- Easy Deployment on Render/Railway

---

## Tech Stack

### Backend
- Java 17
- Spring Boot 3
- Maven
- RestTemplate
- Swagger OpenAPI

### AI Services
- Ollama (LLM)
- ComfyUI (Stable Diffusion)

### Frontend (Planned)
- React
- Tailwind CSS

---

## Project Structure

```
src
 ├── main
 │   ├── java
 │   │   └── image.gen.image
 │   │        ├── config
 │   │        ├── controller
 │   │        ├── service
 │   │        └── ImageApplication
 │   └── resources
 │        └── application.properties
```

---

## Configuration

### application.properties

```properties
spring.application.name=image

server.port=${PORT:8080}

ai.ollama-url=${OLLAMA_URL:http://localhost:11434/api/generate}

ai.comfy-url=${COMFY_URL:http://localhost:8188}
```

---

## Prerequisites

### Install Ollama

Download:
https://ollama.com

Pull a model:

```bash
ollama pull llama3
```

Run:

```bash
ollama serve
```

---

### Install ComfyUI

```bash
git clone https://github.com/comfyanonymous/ComfyUI.git
```

Start:

```bash
python main.py
```

Default URL:

```text
http://localhost:8188
```

---

## Run Application

```bash
./mvnw spring-boot:run
```

or

```bash
mvn spring-boot:run
```

---

## Build

```bash
./mvnw clean package
```

Jar will be generated inside:

```text
target/
```

Run:

```bash
java -jar target/image-0.0.1-SNAPSHOT.jar
```
---
## API Endpoints

### Generate Prompt

```http
POST /api/prompt
```

### Generate Image

```http
POST /api/image
```

### Health Check

```http
GET /actuator/health
```

---

## Swagger Documentation

Open:

```text
http://localhost:8080/swagger-ui.html
```

or

```text
http://localhost:8080/swagger-ui/index.html
```

---

## Environment Variables

| Variable | Description |
|-----------|-------------|
| PORT | Spring Boot Port |
| OLLAMA_URL | Ollama Endpoint |
| COMFY_URL | ComfyUI Endpoint |

---

## Future Enhancements
- Cloud Storage
- AI Image Editing
- Image Upscaling
- Social Sharing

---

## Author

Aditya Tiwari
B.Tech CSE | Spring Boot Backend Developer

from fastapi import FastAPI

app = FastAPI(title="ai-service")


# Mismo cuerpo que el /health de admin-api y user-api.
@app.get("/health")
def health():
    return {"status": "ok"}

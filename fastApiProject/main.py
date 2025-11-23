# main.py

from fastapi import FastAPI, HTTPException
from PersonalInfo import PersonalInfo  # Импортируем PersonalInfo
from logic import Logic

app = FastAPI()

@app.get("/")
def root():
    return {"message": "Hello World"}

@app.get("/hello/{name}")
def say_hello(name: str):
    return {"message": f"Hello {name}"}

@app.post("/vehicle-check")
def vehicle_check(info: PersonalInfo, booking_id: str):
    """Generate recommendation for provided PersonalInfo and booking."""
    logic = Logic()
    try:
        return logic.generate_recommendation(info, booking_id)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

@app.get("/mama")
def personal_info():
    return {"message": "Use /vehicle-check to evaluate vehicles"}
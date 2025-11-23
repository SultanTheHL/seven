# main.py

from fastapi import FastAPI
from PersonalInfo import PersonalInfo  # Импортируем PersonalInfo
from pydantic import BaseModel
from logic import Logic

app = FastAPI()

@app.get("/")
def root():
    return {"message": "Hello World"}

@app.get("/hello/{name}")
def say_hello(name: str):
    return {"message": f"Hello {name}"}

@app.post("/vehicle-check")
def vehicle_check(info: PersonalInfo):
    # Generate ML recommendation response
    logic = Logic()
    return logic.generate_recommendation(info)

@app.get("/mama")
def personal_info(info: PersonalInfo):
    a = Logic()
    a.test_metric(info)
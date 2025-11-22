# main.py

from fastapi import FastAPI
from PersonalInfo import PersonalInfo  # Импортируем PersonalInfo
from pydantic import BaseModel

app = FastAPI()

@app.get("/")
def root():
    return {"message": "Hello World"}

@app.get("/hello/{name}")
def say_hello(name: str):
    return {"message": f"Hello {name}"}

@app.post("/vehicle-check")
def vehicle_check(info: PersonalInfo):
    # Печать бренда текущего автомобиля
    print(f"Current vehicle brand: {info.current_vehicle.brand}")
    return {"receivedVehicleBrand": info.current_vehicle.brand}

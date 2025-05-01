from fastapi import FastAPI
from pydantic import BaseModel
import numpy as np
import joblib

# Load artifacts
model = joblib.load('cardio_model.pkl')
scaler = joblib.load('scaler.pkl')
selector = joblib.load('feature_selector.pkl')

# Indices of continuous features in selected ones (from training)
scale_indices = [0, 1, 2, 5]  # Example: adjust if needed

app = FastAPI()

# Define input model based on original 8 input features
class InputData(BaseModel):
    age: float
    gender: int
    ap_hi: float
    ap_lo: float
    smoke: int
    alco: int
    active: int
    BMI: float

@app.post("/predict")
def predict(data: InputData):
    try:
        # Step 1: Prepare raw input in correct order
        raw_input = np.array([
            [data.age, data.gender, data.ap_hi, data.ap_lo,
             data.smoke, data.alco, data.active, data.BMI]
        ])

        # Step 2: Feature selection
        selected_features = selector.transform(raw_input)

        # Step 3: Scale continuous features only
        scaled_part = scaler.transform(selected_features[:, scale_indices])
        selected_features[:, scale_indices] = scaled_part

        # Step 4: Predict
        prediction = model.predict(selected_features)

        return {"prediction": int(prediction[0])}

    except Exception as e:
        return {"error": str(e)}

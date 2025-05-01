import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, GridSearchCV, cross_val_score
from sklearn.preprocessing import StandardScaler
from xgboost import XGBClassifier
from sklearn.metrics import accuracy_score, confusion_matrix, classification_report
from imblearn.over_sampling import SMOTE
from sklearn.feature_selection import SelectKBest, f_classif
import joblib

# Load dataset
df = pd.read_csv("E:\PS1\cardio_train.csv")  # Replace with actual dataset path

# Drop unwanted columns
df = df.drop(columns=['id', 'cholesterol', 'gluc'])

# Create BMI Feature
df['BMI'] = df['weight'] / ((df['height'] / 100) ** 2)

# Drop height and weight since BMI replaces them
df = df.drop(columns=['height', 'weight'])

# Define Features (X) and Target (y)
X = df.drop(columns=['cardio'])  # Features (excluding target)
y = df['cardio']  # Target variable

# 1. FIRST PERFORM FEATURE SELECTION
selector = SelectKBest(f_classif, k=6)
X_selected = selector.fit_transform(X, y)

# Get selected feature names and indices
selected_features = X.columns[selector.get_support()]
selected_indices = [i for i, col in enumerate(X.columns) if col in selected_features]
print("Selected Features:", selected_features.tolist())

# 2. THEN SCALE ONLY THE SELECTED CONTINUOUS FEATURES
# Identify which selected features need scaling (assuming these are continuous)
features_to_scale = ['age', 'ap_hi', 'ap_lo', 'BMI']  # Update based on your features
scaled_features = [f for f in selected_features if f in features_to_scale]
scale_indices = [selected_features.tolist().index(f) for f in scaled_features]

scaler = StandardScaler()
X_selected_scaled = X_selected.copy()
if len(scale_indices) > 0:
    X_selected_scaled[:, scale_indices] = scaler.fit_transform(X_selected[:, scale_indices])

# Split Data
X_train, X_test, y_train, y_test = train_test_split(X_selected_scaled, y, test_size=0.2, random_state=42)

# Handle Class Imbalance using SMOTE
smote = SMOTE(random_state=42)
X_train_resampled, y_train_resampled = smote.fit_resample(X_train, y_train)

# Hyperparameter tuning for XGBoost
param_grid = {
    'n_estimators': [100, 200, 300],
    'learning_rate': [0.01, 0.05, 0.1],
    'max_depth': [3, 5, 7]
}

grid_search = GridSearchCV(XGBClassifier(), param_grid, cv=5, scoring='accuracy', n_jobs=-1)
grid_search.fit(X_train_resampled, y_train_resampled)

# Best model after tuning
best_model = grid_search.best_estimator_

# Evaluate Model
y_pred = best_model.predict(X_test)
print(f"Optimized Model Accuracy: {accuracy_score(y_test, y_pred):.2f}")

# Save artifacts
joblib.dump(best_model, 'cardio_model.pkl')
joblib.dump(scaler, 'scaler.pkl')
joblib.dump(selector, 'feature_selector.pkl')

# Save selected feature information
with open('feature_info.txt', 'w') as f:
    f.write(f"Selected Features: {','.join(selected_features)}\n")
    f.write(f"Features to Scale: {','.join(scaled_features)}\n")
    f.write(f"All Original Features: {','.join(X.columns)}\n")
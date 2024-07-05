import os
import base64
import traceback
from datetime import datetime
from io import BytesIO

from flask import Blueprint, jsonify, request, send_from_directory
import cv2
import numpy as np
from keras._tf_keras.keras.models import load_model
from keras._tf_keras.keras.applications.resnet50 import preprocess_input

bp = Blueprint("api_bp", __name__)

class_labels = ['cellulitis', 'impetigo', 'athlete-foot', 'nail-fungus', 'ringworm', 'healthy', 'cutaneous-larva-migrans', 'chickenpox', 'shingles']
model = load_model('./model/skin_disease_model.keras')

@bp.route("/", methods=["GET", "POST"])
def handle_files():
    if request.method == 'POST':
        return classify_image()
    
def classify_image():
    try:
        if 'foto' not in request.files:
            return jsonify({'error': 'No file part'}), 400

        file = request.files['foto']
        if file.filename == '':
            return jsonify({'error': 'No selected file'}), 400

        # Read the file into a numpy array
        file_bytes = BytesIO(file.read())
        file_bytes.seek(0)
        file_bytes = np.asarray(bytearray(file_bytes.read()), dtype=np.uint8)
        img = cv2.imdecode(file_bytes, cv2.IMREAD_COLOR)

        if img is None:
            return jsonify({'error': 'Invalid image file'}), 400

        img = cv2.resize(img, (224, 224))
        img = np.expand_dims(img, axis=0)  # Add an extra dimension for batching
        img = preprocess_input(img)

        prediction = model.predict(img)
        predicted_class_index = np.argmax(prediction)
        predicted_class = class_labels[predicted_class_index]
        confidence_score = prediction[0][predicted_class_index]

        return jsonify({'status': 'success', 'message': 'Photo uploaded successfully', 'predicted_class': predicted_class, 'confidence': float(confidence_score)}), 200

    except Exception as e:
        traceback.print_exc()
        return jsonify({'status': 'error', 'message': 'Internal server error', 'details': str(e)}), 500
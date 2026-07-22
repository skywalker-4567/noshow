from flask import Flask, request, jsonify
from features import validate_and_encode
from model import predict, load_model

app = Flask(__name__)

# Pre-load model on startup so first /predict call isn't slow
load_model()

@app.route('/predict', methods=['POST'])
def predict_route():
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "JSON body required"}), 400
        features = validate_and_encode(data)
        result = predict(features)
        return jsonify(result)
    except ValueError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        return jsonify({"error": "Prediction failed", "detail": str(e)}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "UP", "model": "loaded"})

if __name__ == '__main__':
    app.run(port=5001, debug=False)
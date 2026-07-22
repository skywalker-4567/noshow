FEATURE_ORDER = [
    'Age', 'Gender_enc', 'Scholarship', 'Hipertension',
    'Diabetes', 'Alcoholism', 'SMS_received', 'days_waiting'
]

def encode_gender(gender: str) -> int:
    """
    Kaggle dataset: Female ~65%, Male ~35%.
    Map OTHER → 0 (Female/majority class) — defensible at interview.
    """
    if gender == 'F':
        return 0
    elif gender == 'M':
        return 1
    else:
        return 0  # OTHER maps to majority class

def validate_and_encode(data: dict) -> list:
    required = [
        'age', 'gender', 'scholarship', 'hypertension',
        'diabetes', 'alcoholism', 'smsReceived', 'daysWaiting'
    ]
    for field in required:
        if field not in data:
            raise ValueError(f"Missing required field: {field}")

    return [
        int(data['age']),
        encode_gender(str(data['gender'])),
        int(data['scholarship']),
        int(data['hypertension']),
        int(data['diabetes']),
        int(data['alcoholism']),
        int(data['smsReceived']),
        max(0, int(data['daysWaiting']))   # guard: negative daysWaiting → 0
    ]
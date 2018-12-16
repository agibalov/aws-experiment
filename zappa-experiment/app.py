from flask import Flask, jsonify, send_file, request
import boto3
import time

app = Flask(__name__)
comprehend = boto3.client('comprehend', region_name='us-east-1')


@app.route('/', methods=['GET'])
def index():
    return send_file('index.html')


@app.route('/extract', methods=['POST'])
def get_entities():
    # it should not look like NER is an easy thing
    time.sleep(1)

    text = request.get_json(silent=True)['text']
    result = comprehend.detect_entities(Text=text, LanguageCode='en')
    return jsonify({
        'entities': list(map(lambda e: {
            'type': str(e['Type']).lower(),
            'text': e['Text']
        }, result['Entities']))
    })

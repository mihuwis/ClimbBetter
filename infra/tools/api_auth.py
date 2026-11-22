"""Short-lived authentication for local history maintenance; never persist tokens in journals."""
import os
from pathlib import Path
from urllib.parse import urlparse


def request_headers(api_url):
    url = urlparse(api_url)
    if url.username or url.password or not (url.scheme == 'https' or
            (url.scheme == 'http' and url.hostname in ('localhost', '127.0.0.1'))):
        raise ValueError('Authenticated maintenance requires HTTPS or a local HTTP API')
    token_file = os.environ.get('CB_FIREBASE_ID_TOKEN_FILE')
    token = Path(token_file).read_text(encoding='utf-8').strip() if token_file else os.environ.get('CB_FIREBASE_ID_TOKEN', '').strip()
    if not token:
        raise ValueError('Set CB_FIREBASE_ID_TOKEN_FILE or CB_FIREBASE_ID_TOKEN for authenticated maintenance')
    if any(char.isspace() for char in token):
        raise ValueError('Invalid ID token')
    return {'Authorization': 'Bearer ' + token}

"""Скріншоти для записки (рис. 3.6, 3.7). Потрібні: node mock-api.js і npm run dev у frontend/ (порт 5173).
pip install playwright && playwright install chromium; python make.py"""
import pathlib
import urllib.parse
from playwright.sync_api import sync_playwright
from PIL import Image

OUT = pathlib.Path(__file__).resolve().parents[2] / 'figures' / 'screenshots'
OUT.mkdir(parents=True, exist_ok=True)
with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(device_scale_factor=2, viewport={'width': 1200, 'height': 860})
    pg.goto('http://localhost:5173/search?q=' + urllib.parse.quote('напівмарафон') + '&mode=hybrid&debug=1')
    pg.wait_for_timeout(2500)
    pg.screenshot(path=str(OUT / 'search-hybrid.png'))
    pg.goto('http://localhost:5173/ask')
    pg.wait_for_timeout(1500)
    pg.fill('textarea', 'На яку гору я піднімався в Карпатах?')
    pg.click('button[type=submit]')
    pg.wait_for_timeout(2000)
    pg.screenshot(path=str(OUT / 'ask-answer.png'), full_page=True)
    b.close()
im = Image.open(OUT / 'ask-answer.png')
im.crop((0, 0, im.width, min(im.height, 2000))).save(OUT / 'ask-answer.png')
print('ok', OUT)

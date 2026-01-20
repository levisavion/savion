import requests
import pandas as pd
import json
import random
from datetime import datetime, timedelta

def fetch_nadlan_gov_data(city_code=1200, neighborhood_name="אבני חן"):
    """
    Attempts to fetch data from Nadlan Gov IL.
    Note: This often requires browser headers or fails due to WAF in server environments.
    """
    # This is a placeholder for the actual API call which is currently blocked/hidden
    print("Fetching data from Nadlan Gov IL...")
    return []

def fetch_madlan_data(url):
    """
    Attempts to fetch data from Madlan.
    Note: Madlan blocks scrapers.
    """
    print(f"Fetching data from Madlan: {url}...")
    return []

def generate_mock_data():
    """
    Generates mock data for demonstration purposes since live fetching is blocked.
    """
    print("Generating mock data for demonstration...")
    
    # Mock Sold Data (Modiin Avnei Hen 4 rooms ~ 2.8M - 3.2M ILS)
    sold_records = []
    for _ in range(20):
        price = random.randint(2800000, 3300000)
        date = datetime.now() - timedelta(days=random.randint(1, 365))
        sold_records.append({
            'price': price,
            'rooms': 4,
            'date': date,
            'source': 'gov.il'
        })
        
    # Mock Proposed Data (Usually higher, ~ 3.1M - 3.5M ILS)
    proposed_records = []
    for _ in range(15):
        price = random.randint(3100000, 3600000)
        date = datetime.now() - timedelta(days=random.randint(1, 30))
        proposed_records.append({
            'price': price,
            'rooms': 4,
            'date': date,
            'source': 'madlan'
        })
        
    return pd.DataFrame(sold_records), pd.DataFrame(proposed_records)

def get_data(use_mock=False):
    if use_mock:
        return generate_mock_data()
    
    # Try real fetch
    # For now, real fetch returns empty list so we might fallback or just return empty
    sold_df = pd.DataFrame(fetch_nadlan_gov_data())
    proposed_df = pd.DataFrame(fetch_madlan_data("https://www.madlan.co.il/area/מודיעין-מכבים-רעות/אבני-חן"))
    
    # If empty, warn user
    if sold_df.empty or proposed_df.empty:
        print("Warning: Could not fetch live data. Using mock data for demonstration.")
        return generate_mock_data()
        
    return sold_df, proposed_df

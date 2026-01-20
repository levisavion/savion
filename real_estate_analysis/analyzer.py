import pandas as pd

def calculate_stats(sold_data, proposed_data):
    """
    Calculates median prices and percentage difference.
    
    Args:
        sold_data (pd.DataFrame): DataFrame containing sold transaction data with columns ['price', 'rooms', 'date']
        proposed_data (pd.DataFrame): DataFrame containing proposed/asking data with columns ['price', 'rooms', 'date']
        
    Returns:
        dict: Dictionary containing stats.
    """
    
    # Filter for 4 rooms if not already filtered
    sold_4_rooms = sold_data[sold_data['rooms'] == 4]
    proposed_4_rooms = proposed_data[proposed_data['rooms'] == 4]
    
    stats = {}
    
    if not sold_4_rooms.empty:
        sold_median = sold_4_rooms['price'].median()
        stats['sold_median'] = sold_median
        stats['sold_count'] = len(sold_4_rooms)
    else:
        stats['sold_median'] = 0
        stats['sold_count'] = 0
        
    if not proposed_4_rooms.empty:
        proposed_median = proposed_4_rooms['price'].median()
        stats['proposed_median'] = proposed_median
        stats['proposed_count'] = len(proposed_4_rooms)
    else:
        stats['proposed_median'] = 0
        stats['proposed_count'] = 0
        
    if stats['sold_median'] > 0 and stats['proposed_median'] > 0:
        # Diff % (Proposed vs Sold) - usually proposed is higher
        diff = stats['proposed_median'] - stats['sold_median']
        diff_percent = (diff / stats['sold_median']) * 100
        stats['diff_percent'] = diff_percent
    else:
        stats['diff_percent'] = 0
        
    return stats

def print_report(stats, city, neighborhood):
    print(f"--- Real Estate Report for {city} - {neighborhood} (4 Rooms) ---")
    print(f"Sold Median Price: {stats['sold_median']:,.2f} ILS (based on {stats['sold_count']} transactions)")
    print(f"Proposed (Asking) Median Price: {stats['proposed_median']:,.2f} ILS (based on {stats['proposed_count']} listings)")
    
    if stats['sold_median'] > 0 and stats['proposed_median'] > 0:
        direction = "higher" if stats['diff_percent'] > 0 else "lower"
        print(f"Asking prices are {abs(stats['diff_percent']):.2f}% {direction} than sold prices.")
    else:
        print("Insufficient data for comparison.")

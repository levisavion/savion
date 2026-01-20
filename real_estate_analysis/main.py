import argparse
from data_fetcher import get_data
from analyzer import calculate_stats, print_report

def main():
    parser = argparse.ArgumentParser(description="Real Estate Price Analyzer for Modiin, Avnei Hen")
    parser.add_argument("--mock", action="store_true", help="Use mock data instead of live fetch", default=True)
    args = parser.parse_args()
    
    print("Starting Real Estate Analysis...")
    print("Target: Modiin, Avnei Hen, 4 Room Apartments")
    
    sold_df, proposed_df = get_data(use_mock=args.mock)
    
    if sold_df.empty and proposed_df.empty:
        print("No data available.")
        return
        
    stats = calculate_stats(sold_df, proposed_df)
    print_report(stats, "Modiin", "Avnei Hen")

if __name__ == "__main__":
    main()

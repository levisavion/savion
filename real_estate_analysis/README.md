# Modiin Real Estate Price Analyzer

This tool is designed to compare sold apartment prices (from the Tax Authority) with proposed/asking prices (from Madlan) for 4-room apartments in the Avnei Hen neighborhood of Modiin.

## Limitations

Due to security measures (WAF, Anti-bot) on government and real estate websites (`nadlan.gov.il`, `madlan.co.il`), automatic data fetching from a cloud environment is restricted.

The `data_fetcher.py` script includes a mock data generator to demonstrate the analysis logic. To use real data, you would need to implement a robust scraper (e.g., using Selenium or Playwright) or manually provide the datasets.

## Project Structure

- `main.py`: Entry point for the application.
- `data_fetcher.py`: Handles data retrieval (currently mocks data due to restrictions).
- `analyzer.py`: logic for calculating medians and percentage differences.
- `requirements.txt`: Python dependencies.

## Usage

1. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

2. Run the analysis (uses mock data by default):
   ```bash
   python3 main.py
   ```

## Future Improvements

- Integration with Selenium/Playwright for robust scraping.
- Support for more neighborhoods and cities.
- Historical data analysis.

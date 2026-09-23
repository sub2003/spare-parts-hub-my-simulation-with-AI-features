from __future__ import annotations

import argparse
import json

from forecasting import train_candidate


def main() -> None:
    parser = argparse.ArgumentParser(description="Train the Spare Parts Hub demo demand forecasting models.")
    parser.add_argument("--force-promote", action="store_true", help="Promote the candidate even if an active model scores better.")
    args = parser.parse_args()
    metadata = train_candidate(force_promote=args.force_promote)
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()

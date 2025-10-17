#!/bin/bash
# BIST Trading Platform CLI Client - Start Script

# Activate virtual environment
if [ -d "venv" ]; then
    source venv/bin/activate
else
    echo "Error: Virtual environment not found"
    echo "Please run ./setup.sh first"
    exit 1
fi

# Start CLI client
python -m bist_cli.main "$@"

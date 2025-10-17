#!/bin/bash
# BIST Trading Platform CLI Client - Setup Script

echo "================================================"
echo "BIST Trading Platform - CLI Client Setup"
echo "================================================"
echo ""

# Check Python version
echo "Checking Python version..."
python3 --version

if [ $? -ne 0 ]; then
    echo "Error: Python 3 is not installed"
    exit 1
fi

# Create virtual environment
echo ""
echo "Creating virtual environment..."
python3 -m venv venv

if [ $? -ne 0 ]; then
    echo "Error: Failed to create virtual environment"
    exit 1
fi

# Activate virtual environment
echo "Activating virtual environment..."
source venv/bin/activate

# Upgrade pip
echo ""
echo "Upgrading pip..."
pip install --upgrade pip

# Install dependencies
echo ""
echo "Installing dependencies..."
pip install -r requirements.txt

if [ $? -ne 0 ]; then
    echo "Error: Failed to install dependencies"
    exit 1
fi

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    echo ""
    echo "Creating .env configuration file..."
    cp .env.example .env
    echo "✓ .env file created"
    echo "  You can customize it by editing cli-client/.env"
fi

# Test API connection
echo ""
echo "Testing API connection..."
python -m bist_cli.main --test-connection

echo ""
echo "================================================"
echo "Setup Complete!"
echo "================================================"
echo ""
echo "To start the CLI client:"
echo "  1. Activate virtual environment: source venv/bin/activate"
echo "  2. Run the client: python -m bist_cli.main"
echo ""
echo "Or use the quick start script:"
echo "  ./start.sh"
echo ""

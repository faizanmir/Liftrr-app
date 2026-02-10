#!/bin/bash

# Script to download Gemma model without Kaggle CLI
# Usage: ./download_model.sh

set -e

MODEL_DIR="app/src/main/assets/llm"
MODEL_FILE="gemma-2b.bin"

echo "📥 Gemma Model Download Helper"
echo "================================"
echo ""
echo "Since Kaggle requires authentication, here are your options:"
echo ""
echo "OPTION 1: Manual Download (Easiest)"
echo "  1. Visit: https://www.kaggle.com/models/google/gemma"
echo "  2. Sign in with Google"
echo "  3. Click on 'Gemma 2B' -> 'TensorFlow Lite' -> 'gemma-2b-it-cpu-int4'"
echo "  4. Download the .bin file"
echo "  5. Move to: $PWD/$MODEL_DIR/$MODEL_FILE"
echo ""
echo "OPTION 2: Use Hugging Face (Alternative)"
echo "  1. Visit: https://huggingface.co/google/gemma-2b-it"
echo "  2. Download quantized .bin file"
echo "  3. Move to: $PWD/$MODEL_DIR/$MODEL_FILE"
echo ""
echo "OPTION 3: Create Test Model (Development Only)"
echo "  This creates a dummy file for testing the integration:"
echo ""

read -p "Create test model? (y/n) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Creating test model..."
    mkdir -p "$MODEL_DIR"
    echo "test_model_placeholder" > "$MODEL_DIR/$MODEL_FILE"
    echo "✅ Test model created at: $MODEL_DIR/$MODEL_FILE"
    echo ""
    echo "⚠️  Note: This is a placeholder. It won't work for actual AI inference."
    echo "   Download the real model from Kaggle or Hugging Face for production use."
    echo ""
    echo "Next step: ./gradlew pushLLMModel"
else
    echo ""
    echo "Please download the model manually and place it at:"
    echo "  $PWD/$MODEL_DIR/$MODEL_FILE"
    echo ""
    echo "Then run: ./gradlew pushLLMModel"
fi

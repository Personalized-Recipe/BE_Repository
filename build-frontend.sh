#!/bin/bash

echo "Building frontend..."
cd frontend
npm install
npm run build

echo "Copying build files to Spring Boot static directory..."
rm -rf ../src/main/resources/static/*
cp -r build/* ../src/main/resources/static/

echo "Frontend build complete!"

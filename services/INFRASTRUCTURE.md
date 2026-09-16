# Shop infrastructure

Run from this directory:

    cp .env.example .env
    # edit .env with real secrets
    docker compose up -d --build

Useful checks:

    docker compose ps
    docker compose logs -f apigatewayservice
    docker compose down

The application services are intentionally not host-exposed; API traffic should enter through the API Gateway on port 8080.

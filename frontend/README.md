# Shop Frontend

React + Vite storefront connected to the Shop API Gateway.

## Backend

The frontend expects the API Gateway at:

`http://localhost:8080`

To change it, create `.env` from `.env.example`:

`VITE_API_URL=http://localhost:8080`

## Run

```bash
npm install
npm run dev
```

## Backend-connected behavior

- `GET /foods` supplies the live catalogue, images, prices and categories.
- `POST /auth/register` creates a USER account.
- `POST /auth/login` authenticates and stores the JWT locally.
- `POST /order` is used for checkout; the backend calculates authoritative food prices and totals.
- `GET /order?page=0&size=10` loads the authenticated user's orders.
- ADMIN JWTs unlock the catalogue management UI using `POST/PUT/DELETE /foods`.
- No client-supplied role is sent during registration.
- The frontend never calls individual microservices directly; it uses the API Gateway.
